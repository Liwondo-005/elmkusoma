package tz.elmkusoma.payments.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The network value Stakaba receives is case-sensitive and must be exactly
 * one of the four documented strings; this pins both the exact outbound
 * values and the tolerant inbound parsing.
 */
class MobileMoneyNetworkTest {

    @Test
    void documentedApiValuesAreExact() {
        assertEquals("Mpesa", MobileMoneyNetwork.MPESA.apiValue());
        assertEquals("AirtelMoney", MobileMoneyNetwork.AIRTEL_MONEY.apiValue());
        assertEquals("TigoPesa", MobileMoneyNetwork.TIGO_PESA.apiValue());
        assertEquals("HaloPesa", MobileMoneyNetwork.HALO_PESA.apiValue());
    }

    @Test
    void caseInsensitiveParsingStillYieldsTheExactDocumentedValue() {
        assertEquals(MobileMoneyNetwork.MPESA, MobileMoneyNetwork.from("mpesa"));
        assertEquals(MobileMoneyNetwork.MPESA, MobileMoneyNetwork.from("MPESA"));
        assertEquals(MobileMoneyNetwork.AIRTEL_MONEY, MobileMoneyNetwork.from("airtel"));
        assertEquals(MobileMoneyNetwork.AIRTEL_MONEY, MobileMoneyNetwork.from("AirtelMoney"));
        assertEquals(MobileMoneyNetwork.TIGO_PESA, MobileMoneyNetwork.from("TigoPesa"));
        assertEquals(MobileMoneyNetwork.TIGO_PESA, MobileMoneyNetwork.from("mixx"));
        assertEquals(MobileMoneyNetwork.HALO_PESA, MobileMoneyNetwork.from("halo"));
        assertEquals(MobileMoneyNetwork.HALO_PESA, MobileMoneyNetwork.from("HaloPesa"));
    }

    @Test
    void unsupportedNetworksAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> MobileMoneyNetwork.from("OrangeMoney"));
        assertThrows(IllegalArgumentException.class, () -> MobileMoneyNetwork.from(""));
        assertThrows(IllegalArgumentException.class, () -> MobileMoneyNetwork.from(null));
    }
}
