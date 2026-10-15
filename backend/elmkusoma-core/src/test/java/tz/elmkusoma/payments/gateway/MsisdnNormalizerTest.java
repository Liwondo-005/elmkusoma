package tz.elmkusoma.payments.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the MSISDN normalisation rules to the documented Stakaba examples
 * ({@code https://docs.stakaba.com/get-started/overview.md}): every accepted
 * input form must land on {@code 255XXXXXXXXX}, and anything that cannot be a
 * Tanzanian mobile number must be refused before a request is built.
 */
class MsisdnNormalizerTest {

    @Test
    void documentedInputFormsAllNormaliseToTheSameMsisdn() {
        assertEquals("255742000331", MsisdnNormalizer.normalize("0742 000 331"));
        assertEquals("255742000331", MsisdnNormalizer.normalize("+255742000331"));
        assertEquals("255742000331", MsisdnNormalizer.normalize("742000331"));
        assertEquals("255742000331", MsisdnNormalizer.normalize("255742000331"));
    }

    @Test
    void sixPrefixNumbersAreAccepted() {
        assertEquals("255655123456", MsisdnNormalizer.normalize("0655123456"));
    }

    @Test
    void dashedAndParenthesisedInputIsCleaned() {
        assertEquals("255742000331", MsisdnNormalizer.normalize("0742-000-331"));
        assertEquals("255742000331", MsisdnNormalizer.normalize("(0742) 000 331"));
    }

    @Test
    void emptyOrNullIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> MsisdnNormalizer.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> MsisdnNormalizer.normalize("  "));
    }

    @Test
    void nonTanzanianOrMalformedNumbersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> MsisdnNormalizer.normalize("12345"));
        assertThrows(IllegalArgumentException.class, () -> MsisdnNormalizer.normalize("2551234567890"));
        // 255 + 8x is not a Tanzanian mobile prefix.
        assertThrows(IllegalArgumentException.class, () -> MsisdnNormalizer.normalize("255812345678"));
        // Too short after the leading zero.
        assertThrows(IllegalArgumentException.class, () -> MsisdnNormalizer.normalize("0742000"));
    }

    @Test
    void isValidMirrorsNormalize() {
        assertTrue(MsisdnNormalizer.isValid("0742000331"));
        assertFalse(MsisdnNormalizer.isValid("not-a-number"));
    }

    @Test
    void documentedSandboxFailureNumber_isIdentifiedButNotAValidMsisdn() {
        // It is a test affordance, not a real number: identifying it must stay
        // separate from accepting it (only PaymentGatewayService may accept
        // it, and only on a sandbox key).
        assertTrue(MsisdnNormalizer.isDocumentedSandboxFailureNumber("255000000001"));
        assertTrue(MsisdnNormalizer.isDocumentedSandboxFailureNumber("+255000000001"));
        assertFalse(MsisdnNormalizer.isDocumentedSandboxFailureNumber("255742000331"));
        assertFalse(MsisdnNormalizer.isDocumentedSandboxFailureNumber(null));

        // And the strict rule still rejects it.
        assertFalse(MsisdnNormalizer.isValid("255000000001"));
        assertThrows(IllegalArgumentException.class, () -> MsisdnNormalizer.normalize("255000000001"));
    }
}
