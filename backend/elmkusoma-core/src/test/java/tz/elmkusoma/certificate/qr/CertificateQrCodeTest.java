package tz.elmkusoma.certificate.qr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The QR a certificate carries must actually scan back to the verification link.
 *
 * <p>Asserting only that an SVG came out would pass for an empty or malformed image, which is
 * precisely the failure nobody notices until a holder points a phone at a printed certificate.
 * So the emitted geometry is parsed back into modules and decoded, and the round trip has to
 * return the original string.</p>
 */
class CertificateQrCodeTest {

    private static final String URL = "https://learn.elmkusoma.tz/certificates/verify/9F2A4C7B1D6E8A30";

    @Test
    @DisplayName("the verification link round-trips through the generated QR")
    void theVerificationLinkRoundTrips() {
        String svg = CertificateQrCode.svg(URL);
        assertNotNull(svg, "a well-formed URL must encode");

        assertEquals(URL, QrDecodeSupport.decodeSvg(svg),
                "the code a holder scans must open this certificate's verification page");
    }

    @Test
    @DisplayName("the output is a self-contained data URI")
    void theOutputIsADataUri() {
        String uri = CertificateQrCode.svgDataUri(URL);

        assertNotNull(uri);
        assertTrue(uri.startsWith("data:image/svg+xml;base64,"),
                "an <img src> needs the media type, not a bare payload");
        assertEquals(URL, QrDecodeSupport.decodeSvgDataUri(uri),
                "the stored payload must be the image itself, ready to render with no second request");

        String svg = new String(Base64.getDecoder().decode(
                uri.substring("data:image/svg+xml;base64,".length())));
        assertTrue(svg.contains("viewBox="), "a viewBox is what lets it scale for print");
        assertFalse(svg.contains("foreignObject") || svg.contains("<script"),
                "the image is rendered directly, so it must stay inert markup");
    }

    @Test
    @DisplayName("a failed encode returns null instead of throwing")
    void aFailedEncodeReturnsNull() {
        assertNull(CertificateQrCode.svg(null), "null content must not abort a persist");
        assertNull(CertificateQrCode.svg("   "), "blank content must not abort a persist");
        assertNull(CertificateQrCode.svgDataUri(null));
        // Far beyond what any QR version can hold; the WriterException path must be reachable.
        assertNull(CertificateQrCode.svg("x".repeat(10_000)),
                "unencodable content is dropped, never propagated to the caller");
    }
}
