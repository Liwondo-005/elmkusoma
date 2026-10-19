package tz.elmkusoma.finance.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transaction search and export for the Platform Admin financial command centre.
 *
 * <p>The command centre previously had only an aggregate {@code /summary}. An operator who needed
 * to answer "where is the payment for this receipt number" had no way to ask, and no way to get
 * the underlying rows out for reconciliation. This supplies both, from the same stored payments
 * the summary reads - never a recomputed or invented figure.</p>
 *
 * <p>Figures are reproduced exactly as stored. {@code commissionAmount} is null whenever no
 * approved rate existed at confirmation time, and a null is carried through as an empty CSV cell
 * and a null field, never as zero: "no approved rate existed" and "the rate was zero" are
 * different facts and collapsing them would misstate revenue.</p>
 */
@Service
@RequiredArgsConstructor
public class TransactionSearchService {

    /** Hard ceiling on an export, so one request cannot stream the whole table. */
    static final int EXPORT_LIMIT = 10_000;

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * A leading {@code =}, {@code +}, {@code -} or {@code @} makes spreadsheet software execute
     * the cell. Any run of whitespace or control characters may precede it without changing that,
     * so they are skipped when detecting the prefix.
     */
    private static final java.util.regex.Pattern FORMULA_PREFIX =
            java.util.regex.Pattern.compile("^[\\p{Z}\\s\\u0000-\\u001F]*[=+\\-@]");

    /** Characters that force a field to be quoted: delimiter, quote, CR, LF. */
    private static final java.util.regex.Pattern NEEDS_QUOTING =
            java.util.regex.Pattern.compile("[,\"\r\n]");

    private final PaymentRepository paymentRepository;

    /**
     * One row of a transaction, shaped for display rather than for the database.
     *
     * <p>Derived fields are computed here from stored columns so the UI and the CSV cannot
     * disagree about the same payment.</p>
     */
    public record TransactionRow(
            UUID paymentId,
            String receiptNumber,
            String providerReference,
            String provider,
            String serviceType,
            String status,
            BigDecimal amount,
            String currency,
            BigDecimal refundedAmount,
            BigDecimal refundableAmount,
            BigDecimal commissionAmount,
            String commissionStatus,
            String commissionPolicyKey,
            Integer commissionPolicyVersion,
            LocalDateTime paidAt,
            UUID parentId,
            UUID studentId,
            UUID institutionId) {

        static TransactionRow of(Payment p) {
            return new TransactionRow(
                    p.getId(),
                    p.getReceiptNumber(),
                    p.getProviderReference(),
                    p.getProvider(),
                    p.getServiceType(),
                    p.getStatus(),
                    p.getAmount(),
                    p.getCurrency(),
                    p.getRefundedAmount(),
                    p.refundableAmount(),
                    p.getCommissionAmount(),
                    p.getCommissionStatus(),
                    p.getCommissionPolicyKey(),
                    p.getCommissionPolicyVersion(),
                    p.getPaidAt(),
                    p.getParentId(),
                    p.getStudentId(),
                    p.getInstitutionId());
        }
    }

    /** The filter set the command centre can search by. Every field is optional. */
    public record SearchCriteria(
            String query,
            String status,
            String serviceType,
            String provider,
            UUID institutionId,
            UUID parentId,
            LocalDateTime from,
            LocalDateTime to) {

        String queryAsText() {
            return (query == null || query.isBlank()) ? null : query.trim();
        }

        /** The search term as a payment id, when it is one. A receipt number is not, so usually null. */
        UUID queryAsId() {
            String text = queryAsText();
            if (text == null) {
                return null;
            }
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException notAUuid) {
                return null;
            }
        }
    }

    /**
     * The filter as a CriteriaBuilder predicate.
     *
     * <p>Written with the Criteria API rather than a JPQL string because every filter is
     * optional. The JPQL form of an optional filter, {@code (:p IS NULL OR col = :p)}, leaves
     * PostgreSQL unable to infer the type of a parameter used in more than one position, and the
     * query fails at execution. Here each value is bound exactly once, with its type.</p>
     */
    private Specification<Payment> toSpecification(SearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.isFalse(root.get("isDeleted")));

            if (c.status() != null) {
                where.add(cb.equal(root.get("status"), c.status()));
            }
            if (c.serviceType() != null) {
                where.add(cb.equal(root.get("serviceType"), c.serviceType()));
            }
            if (c.provider() != null) {
                where.add(cb.equal(root.get("provider"), c.provider()));
            }
            if (c.institutionId() != null) {
                where.add(cb.equal(root.get("institutionId"), c.institutionId()));
            }
            if (c.parentId() != null) {
                where.add(cb.equal(root.get("parentId"), c.parentId()));
            }
            // A payment that has never been paid has no paidAt, and must not be swept into a
            // date range it does not belong to.
            if (c.from() != null) {
                where.add(cb.and(cb.isNotNull(root.get("paidAt")),
                        cb.greaterThanOrEqualTo(root.get("paidAt"), c.from())));
            }
            if (c.to() != null) {
                where.add(cb.and(cb.isNotNull(root.get("paidAt")),
                        cb.lessThanOrEqualTo(root.get("paidAt"), c.to())));
            }

            String text = c.queryAsText();
            if (text != null) {
                List<Predicate> byText = new ArrayList<>();
                byText.add(cb.equal(root.get("providerReference"), text));
                byText.add(cb.equal(root.get("receiptNumber"), text));
                if (c.queryAsId() != null) {
                    byText.add(cb.equal(root.get("id"), c.queryAsId()));
                }
                where.add(cb.or(byText.toArray(new Predicate[0])));
            }

            return cb.and(where.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public Page<TransactionRow> search(SearchCriteria criteria, Pageable pageable) {
        Pageable safe = pageable == null
                ? PageRequest.of(0, 50, defaultSort())
                : PageRequest.of(pageable.getPageNumber(),
                        Math.min(pageable.getPageSize(), 200), pageable.getSort());
        return paymentRepository.findAll(toSpecification(criteria), safe)
                .map(TransactionRow::of);
    }

    /**
     * CSV export of the same filter set, bounded by {@link #EXPORT_LIMIT}.
     *
     * <p>Cells that are deliberately absent stay empty rather than becoming {@code 0}. A zero
     * commission would be read as "the platform charged nothing and recorded it", whereas an empty
     * cell is the accurate report: no approved rate existed.</p>
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(SearchCriteria criteria) {
        List<Payment> rows = paymentRepository.findAll(toSpecification(criteria),
                PageRequest.of(0, EXPORT_LIMIT, defaultSort())).getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("payment_id,receipt_number,provider_reference,provider,service_type,status,")
                .append("amount,currency,refunded_amount,refundable_amount,commission_amount,")
                .append("commission_status,commission_policy_key,commission_policy_version,paid_at,")
                .append("parent_id,student_id,institution_id\r\n");

        for (TransactionRow r : rows.stream().map(TransactionRow::of).toList()) {
            csv.append(value(r.paymentId())).append(',')
                    .append(value(r.receiptNumber())).append(',')
                    .append(value(r.providerReference())).append(',')
                    .append(value(r.provider())).append(',')
                    .append(value(r.serviceType())).append(',')
                    .append(value(r.status())).append(',')
                    .append(value(r.amount())).append(',')
                    .append(value(r.currency())).append(',')
                    .append(value(r.refundedAmount())).append(',')
                    .append(value(r.refundableAmount())).append(',')
                    .append(value(r.commissionAmount())).append(',')
                    .append(value(r.commissionStatus())).append(',')
                    .append(value(r.commissionPolicyKey())).append(',')
                    .append(value(r.commissionPolicyVersion())).append(',')
                    .append(r.paidAt() == null ? "" : TS.format(r.paidAt())).append(',')
                    .append(value(r.parentId())).append(',')
                    .append(value(r.studentId())).append(',')
                    .append(value(r.institutionId()))
                    .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Newest first.
     *
     * <p>Ordered by {@code createdAt} rather than {@code paidAt} because a payment that has not
     * been paid has no {@code paidAt} at all, and expressing "unpaid rows last" needs a NULLS
     * LAST clause that Hibernate's Criteria API cannot render. {@code createdAt} is always
     * present, so this orders every row deterministically without the unsupported construct.</p>
     */
    private static Sort defaultSort() {
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    /**
     * Escapes one CSV field per RFC 4180.
     *
     * <p>Two separate protections, because they defend against different things:</p>
     * <ul>
     *   <li><b>Structural escaping</b> - a value containing a comma, a double quote, CR or LF is
     *       wrapped in double quotes and its inner quotes doubled. Without this, a comma inside a
     *       value silently shifts every column to its right for the whole file.</li>
     *   <li><b>Formula-injection neutralisation</b> - spreadsheet software executes a cell whose
     *       text begins with {@code =}, {@code +}, {@code -} or {@code @} when the file is opened,
     *       so untrusted text that starts that way is prefixed with a single quote. Leading
     *       whitespace and control characters are skipped when detecting the prefix, because
     *       {@code " =1+1"} and {@code "\t=1+1"} are executed too.</li>
     * </ul>
     *
     * <p>Trusted typed values are emitted verbatim and are never guarded: a {@link BigDecimal} of
     * {@code -5000.00} is a real negative balance and prefixing an apostrophe would silently
     * corrupt the figure a reconciliation depends on. The same applies to UUIDs and timestamps.
     * Only free text, where the leading characters could have been chosen by whoever wrote the
     * row, is guarded.</p>
     *
     * <p>Mirrors {@code frontend/lib/csv.ts} so a file downloaded from either side obeys the same
     * rules; the numeric exception exists here because this export emits typed amounts.</p>
     */
    private static String value(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Number || v instanceof UUID || v instanceof java.time.temporal.TemporalAccessor
                || v instanceof Boolean) {
            // Trusted representation: exact text, no quoting, no formula guard.
            return String.valueOf(v);
        }
        String text = String.valueOf(v);
        if (text.isEmpty()) {
            return text;
        }
        String guarded = FORMULA_PREFIX.matcher(text).find() ? "'" + text : text;
        return NEEDS_QUOTING.matcher(guarded).find()
                ? '"' + guarded.replace("\"", "\"\"") + '"'
                : guarded;
    }
}