package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.IntegrationStatus;
import tz.elmkusoma.administration.domain.WebhookEvent;
import tz.elmkusoma.administration.dto.IntegrationStatusResponse;
import tz.elmkusoma.administration.dto.WebhookEventResponse;
import tz.elmkusoma.administration.repository.IntegrationStatusRepository;
import tz.elmkusoma.administration.repository.WebhookEventRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.repository.UserRepository;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Integration Management (spec §54) + Webhook Operations (spec §55).
 * Probes are real: configuration presence, TCP reachability, local filesystem
 * writability, DB queries, and recorded webhook events. Secrets are never
 * returned — only CONFIGURED / NOT_CONFIGURED and status words.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformIntegrationService {

    private final IntegrationStatusRepository integrationRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final UserRepository userRepository;

    @Value("${livekit.server.url:}")
    private String livekitUrl;
    @Value("${livekit.server.api-key:}")
    private String livekitApiKey;
    @Value("${payment.webhook.secret:}")
    private String paymentWebhookSecret;
    @Value("${spring.mail.host:}")
    private String mailHost;
    @Value("${file.upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<IntegrationStatusResponse> listIntegrations() {
        return integrationRepository.findByIsDeletedFalse().stream()
                .map(this::toResponse).toList();
    }

    public IntegrationStatusResponse probe(String key) {
        IntegrationStatus st = integrationRepository.findByIntegrationKeyAndIsDeletedFalse(key)
                .orElseThrow(() -> new ResourceNotFoundException("Integration", "key", key));

        String connection;
        String detail;
        String configStatus;

        switch (key) {
            case "livekit" -> {
                configStatus = (livekitUrl != null && !livekitUrl.isBlank()
                        && livekitApiKey != null && !livekitApiKey.isBlank()) ? "CONFIGURED" : "NOT_CONFIGURED";
                if (livekitUrl == null || livekitUrl.isBlank()) {
                    connection = "UNKNOWN";
                    detail = "LiveKit URL not configured";
                } else {
                    connection = probeTcp(livekitUrl);
                    detail = "Probed " + redact(livekitUrl) + " (api key " + (configStatus.equals("CONFIGURED") ? "present" : "absent") + ")";
                }
            }
            case "payment" -> {
                configStatus = (paymentWebhookSecret != null && !paymentWebhookSecret.isBlank())
                        ? "CONFIGURED" : "NOT_CONFIGURED";
                long total = webhookEventRepository.countBySourceAndIsDeletedFalse("PAYMENT");
                long failed = webhookEventRepository.countBySourceAndProcessingResultAndIsDeletedFalse("PAYMENT", "FAILED");
                WebhookEvent last = webhookEventRepository.findTopBySourceAndIsDeletedFalseOrderByReceivedAtDesc("PAYMENT");
                connection = total == 0 ? "UNKNOWN" : (last != null && "SUCCESS".equals(last.getProcessingResult()) ? "Operational" : "Failing");
                detail = "Webhook events: " + total + " total, " + failed + " failed"
                        + (configStatus.equals("NOT_CONFIGURED") ? " — webhook secret not configured" : "");
            }
            case "email" -> {
                configStatus = (mailHost != null && !mailHost.isBlank()) ? "CONFIGURED" : "NOT_CONFIGURED";
                connection = "UNKNOWN";
                detail = configStatus.equals("CONFIGURED")
                        ? "SMTP host configured (" + mailHost + ") — delivery requires provider logs"
                        : "No SMTP host configured — email delivery unavailable";
            }
            case "sms" -> {
                configStatus = "NOT_CONFIGURED";
                connection = "UNKNOWN";
                detail = "No SMS gateway configuration present in application config";
            }
            case "storage" -> {
                Path dir = Paths.get(uploadDir != null ? uploadDir : "uploads");
                try {
                    Files.createDirectories(dir);
                    boolean writable = Files.isWritable(dir);
                    connection = writable ? "Operational" : "Failing";
                    configStatus = "CONFIGURED";
                    detail = "Upload dir " + dir.toAbsolutePath() + " writable=" + writable;
                } catch (Exception e) {
                    connection = "Failing";
                    configStatus = "CONFIGURED";
                    detail = "Upload dir inaccessible: " + e.getMessage();
                }
            }
            case "authentication" -> {
                try {
                    userRepository.countByIsDeletedFalse();
                    connection = "Operational";
                    configStatus = "CONFIGURED";
                    detail = "Local identity store reachable (DB user query OK)";
                } catch (Exception e) {
                    connection = "Failing";
                    configStatus = "CONFIGURED";
                    detail = "Identity store probe failed: " + e.getMessage();
                }
            }
            case "notification" -> {
                try {
                    webhookEventRepository.countByReceivedAtAfterAndIsDeletedFalse(LocalDateTime.now().minusDays(1));
                    connection = "Operational";
                    configStatus = "CONFIGURED";
                    detail = "Notification tables reachable";
                } catch (Exception e) {
                    connection = "Failing";
                    configStatus = "CONFIGURED";
                    detail = "Notification probe failed: " + e.getMessage();
                }
            }
            default -> {
                connection = "UNKNOWN";
                configStatus = "UNKNOWN";
                detail = "No probe defined for this integration";
            }
        }

        st.setConnectionStatus(connection);
        st.setConfigStatus(configStatus);
        st.setProbeDetail(detail);
        st.setDiagnostics(key + " probe @ " + LocalDateTime.now());
        if ("Operational".equalsIgnoreCase(connection)) {
            st.setLastSuccessAt(LocalDateTime.now());
        } else if ("Failing".equalsIgnoreCase(connection)) {
            st.setFailureCount((st.getFailureCount() != null ? st.getFailureCount() : 0) + 1);
        }
        // refresh webhook health from real events where applicable
        if ("payment".equals(key) || "livekit".equals(key)) {
            String src = "payment".equals(key) ? "PAYMENT" : "LIVEKIT";
            long failed = webhookEventRepository.countBySourceAndProcessingResultAndIsDeletedFalse(src, "FAILED");
            st.setWebhookStatus(failed == 0 ? "OPERATIONAL" : "FAILING");
            st.setRetryStatus(failed == 0 ? "IDLE" : "RETRIES_PENDING");
        }
        integrationRepository.save(st);
        log.info("Integration probe {}: {} ({})", key, connection, detail);
        return toResponse(st);
    }

    public void recordWebhook(String source, String eventType, String verificationStatus,
                              String processingResult, String errorDetails) {
        try {
            WebhookEvent ev = WebhookEvent.builder()
                    .source(source)
                    .eventType(eventType)
                    .verificationStatus(verificationStatus)
                    .processingResult(processingResult)
                    .errorDetails(errorDetails)
                    .processedAt(LocalDateTime.now())
                    .build();
            webhookEventRepository.save(ev);

            String key = "PAYMENT".equals(source) ? "payment" : "livekit";
            integrationRepository.findByIntegrationKeyAndIsDeletedFalse(key).ifPresent(st -> {
                st.setWebhookStatus("SUCCESS".equals(processingResult) ? "OPERATIONAL" : "FAILING");
                if ("SUCCESS".equals(processingResult)) {
                    st.setLastSuccessAt(LocalDateTime.now());
                    st.setRetryStatus("IDLE");
                } else {
                    st.setFailureCount((st.getFailureCount() != null ? st.getFailureCount() : 0) + 1);
                    st.setRetryStatus("RETRIES_PENDING");
                }
                integrationRepository.save(st);
            });
        } catch (Exception e) {
            log.warn("Failed to record webhook event {}/{}: {}", source, eventType, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public tz.elmkusoma.common.PageResponse<WebhookEventResponse> listWebhookEvents(String source, boolean failedOnly, int page, int size) {
        Page<WebhookEvent> p;
        if (failedOnly) {
            p = webhookEventRepository.findFailed(source, PageRequest.of(page, size));
        } else if (source != null && !source.isBlank()) {
            p = webhookEventRepository.findBySourceAndIsDeletedFalse(source.toUpperCase(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt")));
        } else {
            p = webhookEventRepository.findByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt")));
        }
        var content = p.getContent().stream()
                .map(w -> WebhookEventResponse.builder()
                        .id(w.getId()).source(w.getSource()).eventType(w.getEventType())
                        .verificationStatus(w.getVerificationStatus()).processingResult(w.getProcessingResult())
                        .errorDetails(w.getErrorDetails()).retryCount(w.getRetryCount())
                        .receivedAt(w.getReceivedAt()).processedAt(w.getProcessedAt())
                        .build())
                .toList();
        return new tz.elmkusoma.common.PageResponse<>(content, p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    private String probeTcp(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort()
                    : ("wss".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) ? 443 : 80;
            if (host == null) return "UNKNOWN";
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 2000);
                return "Operational";
            }
        } catch (Exception e) {
            return "Failing";
        }
    }

    private String redact(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + (uri.getHost() != null ? uri.getHost() : "***")
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            return "***";
        }
    }

    private IntegrationStatusResponse toResponse(IntegrationStatus st) {
        return IntegrationStatusResponse.builder()
                .key(st.getIntegrationKey()).name(st.getDisplayName()).category(st.getCategory())
                .connectionStatus(st.getConnectionStatus()).lastSuccessAt(st.getLastSuccessAt())
                .failureCount(st.getFailureCount()).webhookStatus(st.getWebhookStatus())
                .retryStatus(st.getRetryStatus()).configStatus(st.getConfigStatus())
                .diagnostics(st.getDiagnostics()).probeDetail(st.getProbeDetail())
                .build();
    }
}
