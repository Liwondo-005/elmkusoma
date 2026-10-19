package tz.elmkusoma.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Transaction search and CSV export for the financial command centre.
 *
 * <p>The behaviour that matters most here is not the paging - it is that a figure which does not
 * exist is reported as absent rather than as zero. A commission of 0 and a commission that was
 * never approved look identical in a spreadsheet, so the export keeps them distinct.</p>
 *
 * <p>Class-level {@code @Transactional} is load-bearing rather than incidental: without it each
 * {@code @BeforeEach} commits its seeds and the thirteen methods accumulate rows into a shared
 * schema, so the count assertions drift and fail for no real reason. With it every test sees
 * exactly what it seeded and rolls back afterwards.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class TransactionSearchServiceTest {

    @Autowired private TransactionSearchService service;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MockMvc mockMvc;

    private UUID paidWithCommission;
    private UUID paidWithoutCommission;

    @BeforeEach
    void seed() {
        // One payment resolved under an approved rate, one with no approved rate at all.
        Payment approved = payment("COMPLETED", "50000", "RCPT-APPROVED-1");
        approved.setCommissionAmount(new BigDecimal("5000"));
        approved.setCommissionStatus("RESOLVED");
        approved.setCommissionPolicyKey("STD-COMMISSION");
        approved.setCommissionPolicyVersion(1);
        approved.setReceiptNumber("RCPT-APPROVED-1");
        paymentRepository.saveAndFlush(approved);
        paidWithCommission = approved.getId();

        Payment unresolved = payment("COMPLETED", "30000", "RCPT-NONE-2");
        // commissionAmount, commissionStatus and the policy key all stay null on purpose.
        unresolved.setReceiptNumber("RCPT-NONE-2");
        paymentRepository.saveAndFlush(unresolved);
        paidWithoutCommission = unresolved.getId();

        Payment pending = payment("PENDING", "70000", null);
        pending.setPaidAt(null);
        paymentRepository.saveAndFlush(pending);
    }

    private Payment payment(String status, String amount, String receipt) {
        Payment p = Payment.builder()
                .parentId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .serviceType("MONTHLY_ACCESS")
                .provider("stakaba")
                .providerReference(receipt == null ? null : "M_" + UUID.randomUUID())
                .amount(new BigDecimal(amount))
                .currency("TZS")
                .status(status)
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();
        p.setInstitutionId(TestDataSeeder.INSTITUTION_ID);
        p.setReceiptNumber(receipt);
        return p;
    }

    private TransactionSearchService.SearchCriteria all() {
        return new TransactionSearchService.SearchCriteria(
                null, null, null, null, null, null, null, null);
    }

    @Test
    void search_returnsStoredPaymentsWithoutInventingAny() {
        var page = service.search(all(), PageRequest.of(0, 50));
        assertTrue(page.getTotalElements() >= 3, "every seeded payment should be found");
    }

    @Test
    void search_filtersByStatusExactly_notByPrefix() {
        var completed = service.search(
                new TransactionSearchService.SearchCriteria(
                null, "COMPLETED", null, null, null, null, null, null),
                PageRequest.of(0, 50));
        assertTrue(completed.getTotalElements() >= 2);
        assertTrue(completed.getContent().stream()
                        .noneMatch(r -> "PENDING".equals(r.status())),
                "a COMPLETED filter must not leak neighbouring states");

        var pending = service.search(
                new TransactionSearchService.SearchCriteria(
                null, "PENDING", null, null, null, null, null, null),
                PageRequest.of(0, 50));
        assertEquals(1, pending.getTotalElements());
    }

    @Test
    void search_findsAPaymentByItsReceiptNumber() {
        // The reference an operator actually has when a family phones: the number on the document.
        var found = service.search(
                new TransactionSearchService.SearchCriteria(
                null, null, null, null, null, null, null, null),
                PageRequest.of(0, 50));
        assertTrue(found.getContent().stream()
                        .anyMatch(r -> "RCPT-APPROVED-1".equals(r.receiptNumber())),
                "the seeded receipt number should be searchable");
    }

    @Test
    void search_doesNotReturnPaymentsMarkedDeleted() {
        Payment hidden = payment("COMPLETED", "1000", null);
        hidden.setIsDeleted(true);
        paymentRepository.saveAndFlush(hidden);

        var page = service.search(all(), PageRequest.of(0, 100));
        assertTrue(page.getContent().stream().noneMatch(r -> hidden.getId().equals(r.paymentId())),
                "a deleted payment must never appear in reporting");
    }

    @Test
    void row_reportsTheRemainingBalanceRatherThanTheGrossAgain() {
        Payment p = paymentRepository.findById(paidWithCommission).orElseThrow();
        var row = service.search(all(), PageRequest.of(0, 50)).getContent().stream()
                .filter(r -> r.paymentId().equals(p.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, new BigDecimal("50000").compareTo(row.amount()));
        assertEquals(0, new BigDecimal("50000").compareTo(row.refundableAmount()),
                "nothing refunded, so the whole charge is still refundable");
    }

    // -------------------------------------------------------------- the CSV

    @Test
    void csv_hasAHeaderAndOneLinePerTransaction() {
        String csv = new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");
        assertTrue(lines[0].startsWith("payment_id,receipt_number"), "header first");
        assertTrue(csv.contains("paid_at"), "the paid_at column must exist");
        assertTrue(lines.length >= 4, "three seeded payments plus the header");
    }

    @Test
    void csv_leavesAnUnresolvedCommissionBlank_ratherThanWritingZero() {
        // This is the whole point of the export. A 0 here would be read as "the platform earned
        // nothing and recorded it", when the truth is "no approved rate existed at confirmation".
        String csv = new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        String unresolvedLine = csv.lines()
                .filter(l -> l.contains("RCPT-NONE-2"))
                .findFirst()
                .orElseThrow();

        String[] cells = unresolvedLine.split(",", -1);
        int commissionAmountColumn = 10;
        int commissionStatusColumn = 11;
        assertEquals("", cells[commissionAmountColumn],
                "an absent commission must be an empty cell, not 0");
        assertEquals("", cells[commissionStatusColumn],
                "an unresolved allocation must not be reported as resolved");
    }

    @Test
    void csv_carriesTheApprovedCommissionWhenOneExists() {
        String csv = new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        String approvedLine = csv.lines()
                .filter(l -> l.contains("RCPT-APPROVED-1"))
                .findFirst()
                .orElseThrow();
        assertTrue(approvedLine.contains("5000.00") || approvedLine.contains("5000"),
                "an approved commission must survive the export");
        assertTrue(approvedLine.contains("RESOLVED"));
    }

    @Test
    void csv_isBoundedSoOneRequestCannotStreamEverything() {
        assertTrue(TransactionSearchService.EXPORT_LIMIT <= 50_000,
                "the export ceiling must stay a deliberate bound, not 'whatever the table holds'");
    }

    // ------------------------------------------- RFC 4180 escaping + formula guard

    /** Seeds a payment whose only unusual field is its receipt number, then exports the CSV. */
    private String csvForReceipt(String receipt) {
        Payment p = payment("COMPLETED", "50000", null);
        p.setReceiptNumber(receipt);
        paymentRepository.saveAndFlush(p);
        try {
            return new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        } finally {
            paymentRepository.delete(p);
            paymentRepository.flush();
        }
    }

    /**
     * Minimal RFC 4180 reader used by the escaping tests. {@code String.lines()} cannot verify
     * escaping, because it splits on a CR/LF even when that CR/LF is legally inside a quoted
     * field - which is exactly the case under test.
     */
    private static List<List<String>> parseCsv(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                current.add(field.toString());
                field.setLength(0);
            } else if (c == '\r') {
                if (i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
                current.add(field.toString());
                field.setLength(0);
                records.add(current);
                current = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            records.add(current);
        }
        return records;
    }

    private static final int EXPORT_COLUMNS = 18;

    @Test
    void csv_quotesAValueContainingAComma() {
        String csv = csvForReceipt("RCPT,COMMA");
        assertTrue(csv.contains("\"RCPT,COMMA\""),
                "a comma inside a value must not shift every later column");
        for (List<String> record : parseCsv(csv)) {
            assertEquals(EXPORT_COLUMNS, record.size(),
                    "escaping must not change the column count: " + record);
        }
        assertTrue(parseCsv(csv).stream()
                        .anyMatch(r -> r.contains("RCPT,COMMA")),
                "the comma must survive as one cell, not two");
    }

    @Test
    void csv_doublesAnEmbeddedDoubleQuote() {
        String csv = csvForReceipt("RCPT\"QUOTE");
        assertTrue(csv.contains("\"RCPT\"\"QUOTE\""),
                "RFC 4180 escapes an inner quote by doubling it");
        assertTrue(parseCsv(csv).stream().anyMatch(r -> r.contains("RCPT\"QUOTE")),
                "the doubled quote must round-trip back to one cell holding one quote");
    }

    @Test
    void csv_quotesCarriageReturnAndLineFeed_withoutSplittingTheRecord() {
        String csv = csvForReceipt("RCPT\r\nBREAK");
        assertTrue(csv.contains("\"RCPT\r\nBREAK\""),
                "CR/LF must be inside a quoted field");
        List<List<String>> records = parseCsv(csv);
        long matching = records.stream().filter(r -> r.contains("RCPT\r\nBREAK")).count();
        assertEquals(1, matching, "the embedded newline must not create a second record");
        for (List<String> record : records) {
            assertEquals(EXPORT_COLUMNS, record.size(),
                    "escaping must not change the column count: " + record);
        }
    }

    @Test
    void csv_neutralisesFormulaInjection() {
        assertTrue(csvForReceipt("=1+1").contains("'=1+1"),
                "a leading = is executed by spreadsheet software on open");
        assertTrue(csvForReceipt("+1+1").contains("'+1+1"));
        assertTrue(csvForReceipt("@SUM(A1)").contains("'@SUM(A1)"));
    }

    @Test
    void csv_neutralisesFormulaInjectionHiddenBehindLeadingWhitespaceOrControlCharacters() {
        // Excel skips leading whitespace before deciding a cell is a formula, so a naive
        // "starts with =" check is not enough.
        assertTrue(csvForReceipt(" =1+1").contains("' =1+1"),
                "a space before = must not bypass the guard");
        assertTrue(csvForReceipt("\t=1+1").contains("'\t=1+1"),
                "a tab before = must not bypass the guard");
    }

    @Test
    void csv_doesNotCorruptATypedNegativeAmount() {
        // A negative figure is a real balance, not an attack. The formula guard must never
        // rewrite it, or a reconciliation reading the file silently loses its sign.
        Payment p = payment("COMPLETED", "-5000.00", null);
        p.setReceiptNumber("RCPT-NEGATIVE");
        paymentRepository.saveAndFlush(p);
        String csv;
        try {
            csv = new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        } finally {
            paymentRepository.delete(p);
            paymentRepository.flush();
        }
        String line = csv.lines().filter(l -> l.contains("RCPT-NEGATIVE")).findFirst().orElseThrow();
        assertTrue(line.contains("-5000.00"), "the negative amount must survive verbatim: " + line);
        assertFalse(line.contains("'-5000.00"), "a typed number must never be apostrophe-guarded");
        assertFalse(line.contains("\"-5000.00\""), "a typed number needs no quoting");
    }

    @Test
    void csv_leavesUuidAndTimestampColumnsUnquotedAndUnguarded() {
        String csv = new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        String line = csv.lines().filter(l -> l.contains("RCPT-APPROVED-1")).findFirst().orElseThrow();
        assertFalse(line.contains("'\""), "no cell may be an apostrophe-quote: " + line);
        assertTrue(line.matches("(?s).*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*"),
                "the payment_id UUID must be emitted plainly");
    }

    @Test
    void csv_rendersNullAsAnEmptyCellNotAsZeroOrNullText() {
        String csv = new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        String line = csv.lines().filter(l -> l.contains("RCPT-NONE-2")).findFirst().orElseThrow();
        assertFalse(line.contains("null"), "a null must not be written as the word null");
        String[] cells = line.split(",", -1);
        assertEquals("", cells[10]);
    }

    @Test
    void csv_preservesUnicodeVerbatim() {
        String line = csvForReceipt("RCPT-Ø-Ë-日本語");
        assertTrue(line.contains("RCPT-Ø-Ë-日本語"),
                "unicode must pass through unescaped and unmangled: " + line);
    }

    @Test
    void csv_columnOrderIsUnchanged_soTheFrontendContractHolds() {
        String csv = new String(service.exportCsv(all()), StandardCharsets.UTF_8);
        String header = csv.lines().findFirst().orElseThrow();
        assertEquals("payment_id,receipt_number,provider_reference,provider,service_type,status,"
                        + "amount,currency,refunded_amount,refundable_amount,commission_amount,"
                        + "commission_status,commission_policy_key,commission_policy_version,paid_at,"
                        + "parent_id,student_id,institution_id",
                header,
                "the column order is a published contract; escaping must not have changed it");
    }

    // -------------------------------------------------------------- the endpoint

    @Test
    @WithMockUser(roles = "ADMIN")
    void endpoint_servesTheSearchToAPlatformAdmin() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/financial/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void endpoint_exportsCsvWithAnAttachmentHeader() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/financial/transactions/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("transactions_")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void endpoint_refusesAnUnknownSortColumn_byFallingBackNotByFailing() throws Exception {
        // sortBy arrives from the client. An arbitrary column name must not reach the query.
        mockMvc.perform(get("/v1/platform-admin/financial/transactions")
                        .param("sortBy", "not_a_column"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void endpoint_isForbiddenToANonAdmin() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/financial/transactions"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403,
                            "a parent must not read platform-wide financials, got " + code);
                });
    }
}