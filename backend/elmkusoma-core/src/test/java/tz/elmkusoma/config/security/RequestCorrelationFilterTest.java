package tz.elmkusoma.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void generatesAnIdAndEchoesItWhenTheCallerSuppliesNone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/my/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String id = run(request, response);

        assertThat(id).matches("[0-9a-f-]{36}");
        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_HEADER)).isEqualTo(id);
    }

    @Test
    void reusesAWellFormedCallerSuppliedId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/my/payments");
        request.addHeader(RequestCorrelationFilter.CORRELATION_HEADER, "trace-42.abc_DEF-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(run(request, response)).isEqualTo("trace-42.abc_DEF-1");
    }

    @Test
    void refusesToEchoAnUnsafeInboundIdSoItsTextNeverEntersTheLogUnaltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/my/payments");
        request.addHeader(RequestCorrelationFilter.CORRELATION_HEADER, "FORGED\r\nINFO fake line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String id = run(request, response);

        assertThat(id).doesNotContain("FORGED").doesNotContain("\r").doesNotContain("\n");
        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_HEADER)).isEqualTo(id);
    }

    @Test
    void replacesAnOverlongInboundIdInsteadOfLoggingItVerbatim() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/my/payments");
        request.addHeader(RequestCorrelationFilter.CORRELATION_HEADER, "a".repeat(4096));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(run(request, response)).hasSize(36);
    }

    @Test
    void holdsTheIdInMdcWhileTheChainRunsAndClearsItAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/my/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();

        String[] seenByChain = new String[1];
        run(request, response, id -> seenByChain[0] = MDC.get(RequestCorrelationFilter.MDC_KEY));

        assertThat(seenByChain[0]).isNotNull().isEqualTo(request.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE));
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcEvenWhenTheChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/my/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain exploding = (req, res) -> {
            throw new ServletException("boom");
        };

        org.junit.jupiter.api.Assertions.assertThrows(ServletException.class,
                () -> filter.doFilter(request, response, exploding));

        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void keepsTheOriginalIdOnAnErrorDispatchOfTheSameRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/my/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String first = run(request, response);

        MockHttpServletResponse errorResponse = new MockHttpServletResponse();
        String again = run(request, errorResponse);

        assertThat(again).isEqualTo(first);
    }

    private String run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        return run(request, response, id -> { });
    }

    private String run(MockHttpServletRequest request, MockHttpServletResponse response,
                       java.util.function.Consumer<String> duringChain) throws Exception {
        String[] generated = new String[1];
        FilterChain chain = (req, res) -> {
            generated[0] = (String) req.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE);
            duringChain.accept(generated[0]);
        };
        filter.doFilter(request, response, chain);
        return generated[0];
    }
}
