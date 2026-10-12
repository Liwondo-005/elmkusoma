package tz.elmkusoma.certificate.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the content back out of a generated QR, for tests that need to prove an image scans.
 *
 * <p>Shared by the renderer test and the persistence test: the interesting question is not only
 * "did the SVG get built" but "did the image that reached the database still decode to this
 * certificate's verification URL", and answering that twice in two test classes would only mean
 * two places for the parsing to drift.</p>
 */
public final class QrDecodeSupport {

    private QrDecodeSupport() {
    }

    /**
     * Decodes a stored {@code data:image/svg+xml;base64,...} QR back to its content.
     *
     * @throws AssertionError if the payload is not an SVG QR or does not decode
     */
    public static String decodeSvgDataUri(String dataUri) {
        if (dataUri == null || !dataUri.startsWith("data:image/svg+xml;base64,")) {
            throw new AssertionError("expected a QR data URI, got: " + abbreviate(dataUri));
        }
        String payload = dataUri.substring("data:image/svg+xml;base64,".length());
        String svg;
        try {
            svg = new String(Base64.getDecoder().decode(payload));
        } catch (IllegalArgumentException e) {
            throw new AssertionError("QR payload is not valid base64", e);
        }
        return decodeSvg(svg);
    }

    /** Decodes the module grid of an SVG QR produced by {@link CertificateQrCode}. */
    public static String decodeSvg(String svg) {
        Matcher viewBox = Pattern.compile("viewBox=\"0 0 (\\d+) (\\d+)\"").matcher(svg);
        if (!viewBox.find()) {
            throw new AssertionError("SVG must declare a viewBox sized to the module grid");
        }
        int width = Integer.parseInt(viewBox.group(1));
        int height = Integer.parseInt(viewBox.group(2));
        boolean[][] modules = new boolean[height][width];

        // The path format is ours - one M x,y h len v1 h -len z per horizontal run - so parsing it
        // is exact, and any change to the writer that stops emitting valid geometry fails here
        // rather than producing an image that will not scan.
        Matcher runs = Pattern.compile("M(\\d+),(\\d+)h(\\d+)v1h-\\d+z").matcher(svg);
        int found = 0;
        while (runs.find()) {
            int x = Integer.parseInt(runs.group(1));
            int y = Integer.parseInt(runs.group(2));
            int length = Integer.parseInt(runs.group(3));
            if (x + length > width) {
                throw new AssertionError("a run must stay inside the viewBox");
            }
            for (int i = 0; i < length; i++) {
                modules[y][x + i] = true;
            }
            found++;
        }
        if (found == 0) {
            throw new AssertionError("the QR must have dark modules");
        }

        // PURE_BARCODE rather than the detector path: this is an exact, axis-aligned
        // reconstruction of the matrix with no photograph to correct for. The detector searches
        // the image for finder patterns and does not settle on a grid this small; asking it to
        // only makes the assertion about detection rather than about whether the code is right.
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        try {
            Result result = new MultiFormatReader().decode(bitmapOf(modules), hints);
            return result.getText();
        } catch (Exception e) {
            throw new AssertionError("the generated QR does not decode", e);
        }
    }

    private static BinaryBitmap bitmapOf(boolean[][] modules) {
        int height = modules.length;
        int width = modules[0].length;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = modules[y][x] ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        return new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() <= 80 ? value : value.substring(0, 80) + "...";
    }
}
