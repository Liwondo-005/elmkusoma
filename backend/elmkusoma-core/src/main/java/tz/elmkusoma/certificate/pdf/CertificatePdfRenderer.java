package tz.elmkusoma.certificate.pdf;

import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateSignatorySnapshot;
import tz.elmkusoma.certificate.qr.CertificateQrCode;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Renders a certificate to a PDF.
 *
 * <p>This exists because the only other renderer in the repository - {@code
 * CertificatePdfService} in the orphaned {@code elmkusoma-workers} module - is a placeholder: it
 * prints a fixed "CERTIFICATE OF COMPLETION" heading whatever the certificate says, hardcodes a
 * verification domain, and has no access to the lifecycle, the signatories, the grade, the serial
 * or the QR code. It is driven by a message queue nothing in this application publishes to.
 * Bringing rendering here means it reads the same rows the rest of the certificate code does,
 * behind the same authorisation, with no second service to deploy.</p>
 *
 * <p>The layout deliberately mirrors {@code frontend/components/certificates/certificate-document.tsx}
 * section for section - header, centred body, signatory row, verification block - so a PDF and a
 * printed browser document of the same certificate say the same things in the same places. It is
 * not byte-identical and is not meant to be; the content is what must agree.</p>
 *
 * <p>The QR is drawn as filled rectangles from the module grid rather than by embedding the stored
 * SVG, because PDFBox cannot rasterise vector markup. A rectangle per horizontal run is exact at
 * any print size, which is the reason the SVG is vector in the first place.</p>
 */
@Component
public class CertificatePdfRenderer {

    private static final float PAGE_W = PDRectangle.A4.getHeight();
    private static final float PAGE_H = PDRectangle.A4.getWidth();
    private static final float MARGIN = 40f;

    private static final Color INK = new Color(17, 24, 39);
    private static final Color MUTED = new Color(107, 114, 128);
    private static final Color LINE = new Color(209, 213, 219);
    private static final Color REVOKED_RED = new Color(185, 28, 28);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy");

    /**
     * Renders one certificate.
     *
     * @param certificate    the certificate; its status drives the watermark
     * @param signatories    frozen-at-issuance signatory snapshots, in display order
     * @param institutionName awarding institution, may be null
     * @param logoUrl        a {@code data:} URL only. Remote URLs are ignored rather than fetched:
     *                       producing a document would otherwise make an outbound request per
     *                       download, to a host the operator controls and the renderer does not.
     */
    public byte[] render(Certificate certificate,
                         List<CertificateSignatorySnapshot> signatories,
                         String institutionName,
                         String logoUrl) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle(certificate.getTitle());
            info.setSubject(certificate.getCertificateType() + " " + certificate.getSerialNumber());
            info.setAuthor(institutionName == null ? "ELMKUSOMA" : institutionName);
            info.setCreator("ELMKUSOMA");

            PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawFrame(cs);
                drawHeader(document, cs, certificate, institutionName, logoUrl);
                drawBody(cs, certificate);
                drawSignatories(document, cs, signatories);
                drawVerification(cs, certificate);
                drawWatermark(cs, certificate);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private void drawFrame(PDPageContentStream cs) throws IOException {
        cs.setStrokingColor(LINE);
        cs.setLineWidth(1.5f);
        cs.addRect(MARGIN, MARGIN, PAGE_W - 2 * MARGIN, PAGE_H - 2 * MARGIN);
        cs.stroke();

        cs.setLineWidth(0.5f);
        float inset = MARGIN + 7;
        cs.addRect(inset, inset, PAGE_W - 2 * inset, PAGE_H - 2 * inset);
        cs.stroke();
    }

    private void drawHeader(PDDocument document, PDPageContentStream cs, Certificate certificate,
                            String institutionName, String logoUrl) throws IOException {
        float top = PAGE_H - MARGIN - 34;
        float textX = MARGIN + 20;

        PDImageXObject logo = decodeImage(document, logoUrl);
        float textLeft = textX;
        if (logo != null) {
            float h = 40f;
            float w = h * ((float) logo.getWidth() / Math.max(1, logo.getHeight()));
            if (w > 90f) {
                w = 90f;
                h = w * ((float) logo.getHeight() / Math.max(1, logo.getWidth()));
            }
            cs.drawImage(logo, textX, top - h + 6, w, h);
            textLeft = textX + w + 12;
        }

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.setNonStrokingColor(INK);
        cs.newLineAtOffset(textLeft, top);
        cs.showText(fit(institutionName == null || institutionName.isBlank()
                ? "Issuing Institution" : institutionName));
        cs.endText();

        // Right-hand column: the two identifiers an operator reads first.
        float rightEdge = PAGE_W - MARGIN - 20;
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 8.5f);
        cs.setNonStrokingColor(MUTED);
        float serialWidth = PDType1Font.HELVETICA.getStringWidth("Certificate No. " + certificate.getSerialNumber()) / 1000f * 8.5f;
        cs.newLineAtOffset(rightEdge - serialWidth, top + 2);
        cs.showText(fit("Certificate No. " + certificate.getSerialNumber()));
        cs.endText();

        String issued = certificate.getIssueDate() == null ? "" : DATE.format(certificate.getIssueDate().toLocalDate());
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 8.5f);
        cs.setNonStrokingColor(MUTED);
        float issuedWidth = PDType1Font.HELVETICA.getStringWidth("Issued " + issued) / 1000f * 8.5f;
        cs.newLineAtOffset(rightEdge - issuedWidth, top - 12);
        cs.showText(fit("Issued " + issued));
        cs.endText();

        cs.setStrokingColor(LINE);
        cs.setLineWidth(0.75f);
        cs.moveTo(MARGIN + 20, top - 26);
        cs.lineTo(PAGE_W - MARGIN - 20, top - 26);
        cs.stroke();
    }

    private void drawBody(PDPageContentStream cs, Certificate certificate) throws IOException {
        float centreX = PAGE_W / 2f;

        // Heading comes from the type, the same mapping the browser document uses.
        String heading = switch (certificate.getCertificateType()) {
            case ACHIEVEMENT -> "CERTIFICATE OF ACHIEVEMENT";
            case PARTICIPATION -> "CERTIFICATE OF PARTICIPATION";
            case TRANSCRIPT -> "ACADEMIC TRANSCRIPT";
            default -> "CERTIFICATE OF COMPLETION";
        };
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
        cs.setNonStrokingColor(INK);
        cs.newLineAtOffset(centreX - width(PDType1Font.HELVETICA_BOLD, heading, 22) / 2f, PAGE_H - 140);
        cs.showText(fit(heading));
        cs.endText();

        drawCentered(cs, "This is to certify that", PDType1Font.HELVETICA, 11f, MUTED, PAGE_H - 176);
        drawCentered(cs, certificate.getStudentName(), PDType1Font.HELVETICA_BOLD, 30f, INK, PAGE_H - 214);

        if (certificate.getStudentIdNumber() != null && !certificate.getStudentIdNumber().isBlank()) {
            drawCentered(cs, certificate.getStudentIdNumber(), PDType1Font.HELVETICA, 9.5f, MUTED, PAGE_H - 232);
        }

        String statement = statement(certificate);
        drawCentered(cs, statement, PDType1Font.HELVETICA, 11f, MUTED, PAGE_H - 264);

        String course = certificate.getCourseOrProgramme() != null && !certificate.getCourseOrProgramme().isBlank()
                ? certificate.getCourseOrProgramme() : certificate.getTitle();
        drawCentered(cs, course, PDType1Font.HELVETICA_BOLD, 16f, INK, PAGE_H - 292);

        // Attribute chips: grade, instructor, completion date, expiry - only those present.
        StringBuilder chips = new StringBuilder();
        if (certificate.getGrade() != null && !certificate.getGrade().isBlank()) {
            chips.append("Grade ").append(certificate.getGrade());
        }
        if (certificate.getInstructorName() != null && !certificate.getInstructorName().isBlank()) {
            if (chips.length() > 0) chips.append("   |   ");
            chips.append(certificate.getInstructorName());
        }
        if (certificate.getCompletionDate() != null) {
            if (chips.length() > 0) chips.append("   |   ");
            chips.append("Completed ").append(DATE.format(certificate.getCompletionDate()));
        }
        if (certificate.getExpiryDate() != null) {
            if (chips.length() > 0) chips.append("   |   ");
            chips.append("Valid until ").append(DATE.format(certificate.getExpiryDate()));
        }
        if (chips.length() > 0) {
            drawCentered(cs, chips.toString(), PDType1Font.HELVETICA, 9.5f, MUTED, PAGE_H - 316);
        }

        if (certificate.getSkills() != null && !certificate.getSkills().isEmpty()) {
            drawCentered(cs, String.join("  ·  ", certificate.getSkills()),
                    PDType1Font.HELVETICA, 9.5f, MUTED, PAGE_H - 336);
        }
    }

    private String statement(Certificate certificate) {
        if (certificate.getDescription() != null && !certificate.getDescription().isBlank()) {
            return certificate.getDescription();
        }
        return switch (certificate.getCertificateType()) {
            case ACHIEVEMENT -> "has demonstrated outstanding achievement in";
            case PARTICIPATION -> "has participated in";
            case TRANSCRIPT -> "is issued the record of results for";
            default -> "has successfully completed";
        };
    }

    private void drawSignatories(PDDocument document, PDPageContentStream cs,
                                 List<CertificateSignatorySnapshot> signatories) throws IOException {
        if (signatories == null || signatories.isEmpty()) {
            return;
        }
        float baseline = 138f;
        float left = MARGIN + 24;
        float available = PAGE_W - 2 * (MARGIN + 24);
        int shown = Math.min(3, signatories.size());
        float column = available / shown;

        for (int i = 0; i < shown; i++) {
            CertificateSignatorySnapshot s = signatories.get(i);
            float x = left + column * i;
            float columnCentre = x + column / 2f;

            PDImageXObject signature = decodeImage(document, s.getSignatureImage());
            if (signature != null) {
                float h = 34f;
                float w = h * ((float) signature.getWidth() / Math.max(1, signature.getHeight()));
                if (w > column - 30f) {
                    w = column - 30f;
                    h = w * ((float) signature.getHeight() / Math.max(1, signature.getWidth()));
                }
                cs.drawImage(signature, columnCentre - w / 2f, baseline + 6, w, h);
            }

            cs.setStrokingColor(INK);
            cs.setLineWidth(0.75f);
            cs.moveTo(x + 6, baseline);
            cs.lineTo(x + column - 20, baseline);
            cs.stroke();

            // Centre each line on the column, not the page.
            drawColumnCentre(cs, s.getFullName(), PDType1Font.HELVETICA_BOLD, 10f, INK,
                    columnCentre, baseline - 15);
            drawColumnCentre(cs, s.getPositionTitle(), PDType1Font.HELVETICA, 8.5f, MUTED,
                    columnCentre, baseline - 28);
            drawColumnCentre(cs, s.getOrganization(), PDType1Font.HELVETICA, 8.5f, MUTED,
                    columnCentre, baseline - 40);
        }
    }

    private void drawColumnCentre(PDPageContentStream cs, String text, PDType1Font font,
                                  float size, Color colour, float centre, float y) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        String fitted = fit(text);
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(colour);
        cs.newLineAtOffset(centre - width(font, fitted, size) / 2f, y);
        cs.showText(fitted);
        cs.endText();
    }

    private void drawVerification(PDPageContentStream cs, Certificate certificate) throws IOException {
        float right = PAGE_W - MARGIN - 24;
        float bottom = MARGIN + 24;
        float qrSize = 68f;
        float qrX = right - qrSize;
        float qrY = bottom;

        drawQr(cs, certificate, qrX, qrY, qrSize);

        float textRight = qrX - 14;
        String label = "Scan to verify";
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9f);
        cs.setNonStrokingColor(INK);
        cs.newLineAtOffset(textRight - width(PDType1Font.HELVETICA_BOLD, label, 9f), qrY + qrSize - 12);
        cs.showText(fit(label));
        cs.endText();

        String url = certificate.getVerificationUrl() == null ? "" : certificate.getVerificationUrl();
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 8f);
        cs.setNonStrokingColor(MUTED);
        cs.newLineAtOffset(textRight - width(PDType1Font.HELVETICA, url, 8f), qrY + qrSize - 26);
        cs.showText(fit(url));
        cs.endText();

        String serial = "Serial " + certificate.getSerialNumber();
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 8f);
        cs.setNonStrokingColor(MUTED);
        cs.newLineAtOffset(textRight - width(PDType1Font.HELVETICA, serial, 8f), qrY + qrSize - 40);
        cs.showText(fit(serial));
        cs.endText();

        String issuedBy = "Issued under the authority of the issuing institution";
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 7.5f);
        cs.setNonStrokingColor(MUTED);
        cs.newLineAtOffset(MARGIN + 24, bottom + 4);
        cs.showText(fit(issuedBy));
        cs.endText();
    }

    /**
     * Draws the module grid as filled rectangles.
     *
     * <p>A rectangle per horizontal run rather than per module: a certificate QR has several
     * hundred dark modules, and per-module path operations make the page needlessly large for a
     * result that is visually identical.</p>
     */
    private void drawQr(PDPageContentStream cs, Certificate certificate, float x, float y, float size)
            throws IOException {
        String content = certificate.getVerificationUrl() != null && !certificate.getVerificationUrl().isBlank()
                ? certificate.getVerificationUrl()
                : certificate.getVerificationCode();
        BitMatrix matrix = CertificateQrCode.matrix(content);
        if (matrix == null) {
            return;
        }
        int w = matrix.getWidth();
        int h = matrix.getHeight();

        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(x, y, size, size);
        cs.fill();

        cs.setNonStrokingColor(Color.BLACK);
        float module = size / w;
        for (int row = 0; row < h; row++) {
            int runStart = -1;
            for (int col = 0; col <= w; col++) {
                boolean dark = col < w && matrix.get(col, row);
                if (dark && runStart < 0) {
                    runStart = col;
                }
                if (!dark && runStart >= 0) {
                    int length = col - runStart;
                    // PDF origin is bottom-left; the matrix grows downwards.
                    float px = x + runStart * module;
                    float py = y + size - (row + 1) * module;
                    cs.addRect(px, py, length * module, module);
                    runStart = -1;
                }
            }
        }
        cs.fill();
    }

    /**
     * Stamps the watermark last so it sits over the content.
     *
     * <p>A revoked certificate exported to PDF is the case where a watermark matters most: the file
     * is detached from the system the moment it is saved, and a copy forwarded to a third party
     * has no way to ask the server whether it is still valid.</p>
     */
    private void drawWatermark(PDPageContentStream cs, Certificate certificate) throws IOException {
        if (certificate.getStatus() != Certificate.CertificateStatus.REVOKED
                && certificate.getStatus() != Certificate.CertificateStatus.DRAFT) {
            return;
        }
        String word = certificate.getStatus() == Certificate.CertificateStatus.REVOKED
                ? "REVOKED" : "DRAFT";
        float size = 110f;
        float wordWidth = width(PDType1Font.HELVETICA_BOLD, word, size);

        // Rotate about the centre of the page so the word crosses it diagonally. A plain
        // horizontal stamp reads as a label; a rotated one reads as a watermark, and it cannot be
        // mistaken for content that belongs to the document.
        cs.saveGraphicsState();
        cs.transform(org.apache.pdfbox.util.Matrix.getRotateInstance(
                Math.toRadians(32), PAGE_W / 2f, PAGE_H / 2f));
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, size);
        // Solid, washed-out red rather than a translucent one: PDF transparency needs an ExtGState
        // graphics state, and a pale solid colour reads the same when printed.
        cs.setNonStrokingColor(new Color(236, 170, 170));
        cs.newLineAtOffset(-wordWidth / 2f, -size / 2f);
        cs.showText(fit(word));
        cs.endText();
        cs.restoreGraphicsState();
    }

    private void drawCentered(PDPageContentStream cs, String text, PDType1Font font,
                              float size, Color colour, float y) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        String fitted = fit(text);
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(colour);
        cs.newLineAtOffset(PAGE_W / 2f - width(font, fitted, size) / 2f, y);
        cs.showText(fitted);
        cs.endText();
    }

    private float width(PDType1Font font, String text, float size) {
        try {
            return font.getStringWidth(fit(text)) / 1000f * size;
        } catch (IOException | IllegalArgumentException e) {
            // getStringWidth fails only for text we could not show either; estimate so layout
            // still happens rather than the whole export aborting.
            return text.length() * size * 0.5f;
        }
    }

    /**
     * Makes a string safe for the PDF standard fonts.
     *
     * <p>{@code showText} throws on a glyph the font's encoding does not carry, and a certificate
     * holder's name is exactly the sort of field that can arrive with a character outside it. NFD
     * decomposition strips diacritics (é to e, ç to c) rather than dropping the letter, the common
     * typographic marks are folded to their ASCII equivalents, printable Latin-1 is passed through
     * - it is covered by WinAnsiEncoding, which the standard fonts use - and anything still
     * unmapped, such as a character from another script entirely, becomes a question mark. The
     * document stays printable whatever it is given.</p>
     */
    static String fit(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalised = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u2018', '\'').replace('\u2019', '\'')
                .replace('\u201C', '"').replace('\u201D', '"')
                .replace('\u2013', '-').replace('\u2014', '-')
                .replace("\u2026", "...")
                .replace('\u00A0', ' ');
        StringBuilder out = new StringBuilder(normalised.length());
        for (int i = 0; i < normalised.length(); i++) {
            char c = normalised.charAt(i);
            boolean printable = (c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF);
            out.append(printable ? c : '?');
        }
        return out.toString();
    }

    /**
     * Decodes a {@code data:} URL to a drawable image, or null.
     *
     * <p>Only data URLs. A remote logo would mean an outbound request on every export, to a host
     * the operator controls and this renderer does not, turning a download into a server-side
     * fetch - and a document is meant to be reproducible offline.</p>
     */
    private PDImageXObject decodeImage(PDDocument document, String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            return null;
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            return null;
        }
        String header = dataUrl.substring(5, comma);
        boolean base64 = header.endsWith(";base64");
        String payload = dataUrl.substring(comma + 1);
        try {
            byte[] bytes = base64
                    ? Base64.getDecoder().decode(payload.getBytes(StandardCharsets.US_ASCII))
                    : payload.getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0) {
                return null;
            }
            // Decoded in memory rather than through a temp file: createFromFileByContent takes a
            // File in this version of PDFBox, and writing a transient image to disk to draw one
            // logo would put a cleanup obligation on every export.
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return null;
            }
            return org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, image);
        } catch (Exception e) {
            // A logo that will not decode is a logo the document can do without. Losing the
            // image must not fail the export of the certificate itself.
            return null;
        }
    }
}
