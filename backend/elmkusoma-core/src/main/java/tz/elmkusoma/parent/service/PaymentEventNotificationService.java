package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.subscription.domain.Subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Emits the payment lifecycle events the derived parent inbox cannot project for itself.
 *
 * <p>The parent inbox is deliberately built two ways. Most money facts - a payment that
 * completed, failed, was cancelled or expired, a receipt that exists, a payment that was
 * refunded - are <em>derived on read</em> from {@link Payment} by
 * {@link ParentFinancialNotificationAssembler}, because deriving them means a duplicated
 * callback or a re-run of a sweep can never manufacture a second alert. That projection is
 * complete for settled money but silent about everything that happens <em>before</em> money
 * settles and everything that happens to a period of access rather than to a payment.</p>
 *
 * <p>This service fills exactly that hole and nothing more. It owns four events:</p>
 *
 * <ul>
 *   <li><b>Payment initiated</b> - the assembler skips {@code PENDING} rows on purpose, so
 *       nobody is ever told about money that is merely in flight.</li>
 *   <li><b>Subscription activated</b> - a period of access opening, which no payment
 *       projection can see.</li>
 *   <li><b>Subscription expired</b> - the same period closing.</li>
 *   <li><b>Refund status changed</b> - the journey through review, approval, processing,
 *       rejection and cancellation, which the assembler does not report until the money has
 *       actually landed back.</li>
 * </ul>
 *
 * <p>Three properties this class is written to keep:</p>
 *
 * <ul>
 *   <li><b>Duplicates are refused.</b> Every send is checked against what is already in the
 *       inbox for that user, event target, type and headline, so a retried webhook, a replayed
 *       idempotency key or a sweep that runs twice cannot produce two rows. A completed refund
 *       of a fully refunded payment is skipped entirely, because the derived projection already
 *       reports it and the two would say the same thing twice.</li>
 *   <li><b>Preferences are respected.</b> Sending goes through
 *       {@link NotificationService#notifyUser}, which consults the parent's own preference
 *       record. Financial headlines are governed by {@code feeAlerts}, the same switch the
 *       assembler honours, so a parent who has turned fee alerts off is not reached by either
 *       path.</li>
 *   <li><b>Access is never touched.</b> Nothing here reads or writes an {@code Entitlement},
 *       a {@code Subscription} status or a payment status. A notification reports what already
 *       happened; it can neither grant access nor revoke it, and a failure to write one is
 *       logged rather than thrown so it can never fail, roll back or distort the money event
 *       it describes.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventNotificationService {

    /** Financial headlines: gated by the parent's {@code feeAlerts} preference. */
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_REFUND = "REFUND";
    /** Access periods are money too, so they are gated by the same fee preference. */
    public static final String TYPE_SUBSCRIPTION = "SUBSCRIPTION";

    public static final String TARGET_PAYMENT = "payment";
    public static final String TARGET_REFUND = "refund";
    public static final String TARGET_SUBSCRIPTION = "subscription";

    public static final String TITLE_PAYMENT_INITIATED = "Payment initiated";
    public static final String TITLE_SUBSCRIPTION_ACTIVATED = "Subscription activated";
    public static final String TITLE_SUBSCRIPTION_EXPIRED = "Subscription expired";

    private static final DateTimeFormatter PERIOD =
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm");

    private final NotificationService notificationService;
    private final LearnerNotificationRepository notificationRepository;
    private final ParentRepository parentRepository;

    /** A payment that has been created but not yet confirmed by the gateway. */
    public void paymentInitiated(Payment payment) {
        if (payment == null || payment.getId() == null) {
            return;
        }
        notifyOnce(payerUserId(payment.getParentId()), payment.getId(), TARGET_PAYMENT,
                TYPE_PAYMENT, TITLE_PAYMENT_INITIATED,
                String.format("Your payment of %s %s for %s is awaiting confirmation.",
                        money(payment.getAmount()), nullToDash(payment.getCurrency()),
                        nullToDash(payment.getServiceType())));
    }

    /**
     * A subscription period opening after a verified payment.
     *
     * <p>Called for every successful activation, including one that returned an already
     * existing record, because a retried webhook must not open a second period - and the
     * duplicate guard is what keeps it from opening a second headline either.</p>
     */
    public void subscriptionActivated(Subscription subscription) {
        if (subscription == null || subscription.getId() == null) {
            return;
        }
        notifyOnce(payerUserId(subscription.getPayerId()), subscription.getId(),
                TARGET_SUBSCRIPTION, TYPE_SUBSCRIPTION, TITLE_SUBSCRIPTION_ACTIVATED,
                String.format("Your %s access is active until %s.",
                        nullToDash(subscription.getServiceType()),
                        period(subscription.getPeriodEnd())));
    }

    /** A subscription period that has run out. Only ever reached by an absorbing transition. */
    public void subscriptionExpired(Subscription subscription) {
        if (subscription == null || subscription.getId() == null) {
            return;
        }
        notifyOnce(payerUserId(subscription.getPayerId()), subscription.getId(),
                TARGET_SUBSCRIPTION, TYPE_SUBSCRIPTION, TITLE_SUBSCRIPTION_EXPIRED,
                String.format("Your %s access ended on %s.",
                        nullToDash(subscription.getServiceType()),
                        period(subscription.getPeriodEnd())));
    }

    /**
     * A refund moving between lifecycle states.
     *
     * <p>The one state deliberately left out is a completed refund of a
     * <em>fully</em> refunded payment: the derived projection already reports that as
     * "Payment Refunded", so emitting it here would tell the payer the same thing twice.
     * A completed <em>partial</em> refund is reported here, because the projection only
     * speaks up when nothing is left to refund and a partial completion would otherwise go
     * unheard.</p>
     */
    public void refundStatusChanged(RefundRequest request, Payment payment) {
        if (request == null || request.getId() == null || payment == null) {
            return;
        }
        String status = request.getStatus();
        if (RefundRequest.STATUS_COMPLETED.equals(status) && payment.isFullyRefunded()) {
            return;
        }
        notifyOnce(payerUserId(payment.getParentId()), request.getId(), TARGET_REFUND,
                TYPE_REFUND, "Refund " + titleCase(status),
                String.format("Your refund of %s %s is now %s.",
                        money(request.getAmount()), nullToDash(currencyOf(payment)),
                        nullToDash(status)));
    }

    /**
     * Resolves the payer's account, then emits exactly once per event and headline.
     *
     * <p>Ownership travels with the recipient lookup: the notification is addressed to the
     * user behind the parent row that paid, and to nobody else, so no caller can redirect a
     * money headline at an account that was not party to it.</p>
     */
    private void notifyOnce(UUID recipient, UUID targetId, String targetType,
                            String notificationType, String title, String message) {
        if (recipient == null || targetId == null) {
            return;
        }
        try {
            if (notificationRepository
                    .existsByUserIdAndTargetIdAndNotificationTypeAndTitleAndIsDeletedFalse(
                            recipient, targetId, notificationType, title)) {
                log.debug("Duplicate payment notification '{}' for target {} skipped",
                        title, targetId);
                return;
            }
            notificationService.notifyUser(recipient, title, message,
                    notificationType, targetType, targetId);
        } catch (RuntimeException ex) {
            // The money event this describes has already been recorded. Failing the inbox
            // write could not undo it, and would only turn a successful payment into an error
            // the payer sees, so the gap is logged for reconciliation instead.
            log.warn("Payment notification '{}' for target {} could not be emitted: {}",
                    title, targetId, ex.getMessage());
        }
    }

    private UUID payerUserId(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return parentRepository.findById(parentId).map(Parent::getUserId).orElse(null);
    }

    private static String currencyOf(Payment payment) {
        return payment == null ? null : payment.getCurrency();
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }

    private static String period(LocalDateTime value) {
        return value == null ? "the end of the period" : PERIOD.format(value);
    }

    private static String nullToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    /**
     * Turns a canonical status into a readable headline word, keeping the canonical form in
     * the body of the message where it is unambiguous.
     *
     * <p>{@code UNDER_REVIEW} becomes {@code Under Review}, not {@code Under_review}: the
     * underscore is a storage convention and has no business reaching a parent's inbox.</p>
     */
    private static String titleCase(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder headline = new StringBuilder(value.length());
        for (String word : value.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!headline.isEmpty()) {
                headline.append(' ');
            }
            headline.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        }
        return headline.toString();
    }
}
