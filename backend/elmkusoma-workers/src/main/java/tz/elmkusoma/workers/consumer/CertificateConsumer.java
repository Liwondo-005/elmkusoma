package tz.elmkusoma.workers.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tz.elmkusoma.workers.config.RabbitMQConfig;
import tz.elmkusoma.workers.event.CertificateGenerationEvent;
import tz.elmkusoma.workers.service.CertificatePdfService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateConsumer {

    private final CertificatePdfService certificatePdfService;

    @Value("${app.workers.certificate.storage-path:./certificates}")
    private String storagePath;

    @RabbitListener(queues = RabbitMQConfig.CERTIFICATE_QUEUE)
    public void handleCertificateGenerationEvent(CertificateGenerationEvent event) {
        log.info("Received certificate generation event: id={}, certificateId={}, student={}",
                event.getId(), event.getCertificateId(), event.getStudentName());

        try {
            String verificationCode = generateVerificationCode();

            byte[] pdfBytes = certificatePdfService.generateCertificatePdf(
                    event.getStudentName(),
                    event.getCourseName(),
                    LocalDate.now(),
                    verificationCode,
                    "Elmkusoma LMS"
            );

            String filePath = savePdfToFile(pdfBytes, event.getCertificateId());

            log.info("Certificate PDF generated and saved successfully. certificateId={}, filePath={}, verificationCode={}",
                    event.getCertificateId(), filePath, verificationCode);

            publishCertificateCompletionEvent(event.getCertificateId(), filePath, verificationCode);

        } catch (Exception e) {
            log.error("Failed to generate certificate for certificateId={}. Error: {}",
                    event.getCertificateId(), e.getMessage(), e);
            throw new RuntimeException("Certificate generation failed", e);
        }
    }

    private String generateVerificationCode() {
        return "ELM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String savePdfToFile(byte[] pdfBytes, Long certificateId) throws IOException {
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        String fileName = "certificate_" + certificateId + ".pdf";
        Path filePath = storageDir.resolve(fileName);
        Files.write(filePath, pdfBytes);

        return filePath.toString();
    }

    private void publishCertificateCompletionEvent(Long certificateId, String filePath, String verificationCode) {
        try {
            log.info("Certificate generation completed: certificateId={}, filePath={}, verificationCode={}",
                    certificateId, filePath, verificationCode);
        } catch (Exception e) {
            log.warn("Failed to publish certificate completion event: {}", e.getMessage());
        }
    }
}
