package tz.elmkusoma.certificate.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders a verification link as a QR code the certificate carries with it.
 *
 * <p>The code is generated server-side and stored on the certificate rather than drawn in the
 * browser, for three reasons. The document is correct wherever it ends up: a printed copy, an
 * exported PDF, or a screenshot, none of which re-run JavaScript. It encodes the canonical
 * verification URL rather than whatever origin the page happened to be served from - the previous
 * client-side code used {@code window.location.origin}, so the same certificate scanned to
 * different hosts depending on where it was viewed. And a reviewer or auditor reading the row
 * directly gets the same artifact a holder would.</p>
 *
 * <p>The output is an SVG rather than a PNG because certificates are printed. A raster QR is
 * interpolated when it is scaled up, which softens the module edges a scanner has to resolve; a
 * vector QR stays exact at any size.</p>
 *
 * <p>Every method fails to {@code null} rather than throwing. The QR is an affordance over a
 * verification URL that already exists on the row, so a rendering failure must never abort an
 * issuance or a persist.</p>
 */
public final class CertificateQrCode {

    private CertificateQrCode() {
    }

    /**
     * The QR as a self-contained data URI, suitable for storing in {@code qr_code_url}.
     *
     * <p>A data URI rather than an endpoint URL: no public base URL for the API is configured, and
     * the certificate then carries its own QR with no second request at the moment it is read.</p>
     *
     * @return {@code data:image/svg+xml;base64,...}, or null if the content cannot be encoded
     */
    public static String svgDataUri(String content) {
        String svg = svg(content);
        if (svg == null) {
            return null;
        }
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The QR as an SVG document, or null when the content is empty or cannot be encoded.
     *
     * <p>Modules are emitted as horizontal runs in a single path rather than one rect each: a
     * typical certificate QR has several hundred dark modules, and per-module shapes would inflate
     * a string the database stores on every row.</p>
     */
    public static String svg(String content) {
        BitMatrix matrix = matrix(content);
        if (matrix == null) {
            return null;
        }
        return toSvg(matrix);
    }

    /**
     * The raw module grid, or null when the content is empty or cannot be encoded.
     *
     * <p>Exposed for callers that do not write markup. The PDF renderer draws this as filled
     * rectangles rather than embedding the SVG: PDFBox cannot rasterise vector markup without a
     * browser, and a rectangle per horizontal run is exact at any print size - which is the whole
     * reason the SVG is vector in the first place.</p>
     */
    public static BitMatrix matrix(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            // M recovers 15% of codewords. Print is the hostile case here - a fold, a coffee ring
            // or a bad photocopy - and L's 7% leaves little margin for a document meant to outlive
            // the screen it was created on.
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            // Left at the library default of 4 modules, which is what the QR specification
            // requires as a quiet zone. A tighter margin scans worse than it looks.
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (Exception e) {
            // WriterException for content too long for the largest version, or a malformed
            // argument. The verification URL already exists on the certificate, so the QR is
            // dropped and the row is still complete.
            return null;
        }
    }

    private static String toSvg(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        StringBuilder path = new StringBuilder(width * height / 4);
        for (int y = 0; y < height; y++) {
            int runStart = -1;
            for (int x = 0; x <= width; x++) {
                boolean dark = x < width && matrix.get(x, y);
                if (dark && runStart < 0) {
                    runStart = x;
                }
                if (!dark && runStart >= 0) {
                    int length = x - runStart;
                    path.append('M').append(runStart).append(',').append(y)
                            .append('h').append(length)
                            .append("v1h-").append(length)
                            .append('z');
                    runStart = -1;
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "
                + width + " " + height + "\" shape-rendering=\"crispEdges\">"
                + "<rect width=\"" + width + "\" height=\"" + height + "\" fill=\"#ffffff\"/>"
                + "<path fill=\"#000000\" d=\"" + path + "\"/></svg>";
    }
}
