package tz.elmkusoma.workers.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class CertificatePdfService {

    private static final float PAGE_WIDTH = 842;
    private static final float PAGE_HEIGHT = 595;

    public byte[] generateCertificatePdf(String studentName, String courseName,
                                          LocalDate issueDate, String verificationCode,
                                          String institutionName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, Font.NORMAL, java.awt.Color.DARK_GRAY);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.NORMAL, java.awt.Color.GRAY);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, java.awt.Color.BLACK);
            Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL, java.awt.Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, java.awt.Color.GRAY);
            Font verificationFont = FontFactory.getFont(FontFactory.COURIER, 9, Font.NORMAL, java.awt.Color.GRAY);

            addEmptyLine(document, 3);

            Paragraph institutionParagraph = new Paragraph(institutionName, subtitleFont);
            institutionParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(institutionParagraph);

            addEmptyLine(document, 2);

            Paragraph certTitle = new Paragraph("CERTIFICATE OF COMPLETION", titleFont);
            certTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(certTitle);

            addEmptyLine(document, 1);

            Paragraph divider = new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", subtitleFont);
            divider.setAlignment(Element.ALIGN_CENTER);
            document.add(divider);

            addEmptyLine(document, 2);

            Paragraph thisCertifies = new Paragraph("This is to certify that", bodyFont);
            thisCertifies.setAlignment(Element.ALIGN_CENTER);
            document.add(thisCertifies);

            addEmptyLine(document, 1);

            Paragraph nameParagraph = new Paragraph(studentName, boldBodyFont);
            nameParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(nameParagraph);

            addEmptyLine(document, 1);

            Paragraph hasCompleted = new Paragraph("has successfully completed the course", bodyFont);
            hasCompleted.setAlignment(Element.ALIGN_CENTER);
            document.add(hasCompleted);

            addEmptyLine(document, 1);

            Paragraph courseParagraph = new Paragraph(courseName, boldBodyFont);
            courseParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(courseParagraph);

            addEmptyLine(document, 2);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            String formattedDate = issueDate.format(formatter);

            Paragraph dateParagraph = new Paragraph("Date of Issue: " + formattedDate, bodyFont);
            dateParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(dateParagraph);

            addEmptyLine(document, 3);

            Paragraph issuedBy = new Paragraph("Issued by: " + institutionName, smallFont);
            issuedBy.setAlignment(Element.ALIGN_CENTER);
            document.add(issuedBy);

            addEmptyLine(document, 1);

            Paragraph verification = new Paragraph("Verification Code: " + verificationCode, verificationFont);
            verification.setAlignment(Element.ALIGN_CENTER);
            document.add(verification);

            Paragraph footerText = new Paragraph(
                    "This certificate is issued under the authority of " + institutionName +
                    " and can be verified at verify.elmkusoma.co.tz", smallFont);
            footerText.setAlignment(Element.ALIGN_CENTER);
            document.add(footerText);

            document.close();

            log.info("Certificate PDF generated successfully for student: {}, course: {}", studentName, courseName);

        } catch (Exception e) {
            log.error("Failed to generate certificate PDF for student: {}. Error: {}",
                    studentName, e.getMessage(), e);
            throw new RuntimeException("Failed to generate certificate PDF", e);
        }

        return baos.toByteArray();
    }

    private void addEmptyLine(Document document, int number) throws DocumentException {
        for (int i = 0; i < number; i++) {
            document.add(new Paragraph(" "));
        }
    }
}
