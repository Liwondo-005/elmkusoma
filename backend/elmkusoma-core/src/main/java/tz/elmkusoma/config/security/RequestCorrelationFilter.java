package tz.elmkusoma.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request a correlation id and puts it in the MDC so that one line of
 * application output can be tied back to the request that produced it.
 *
 * <p>The value is deliberately constrained: an inbound {@code X-Correlation-Id} is only
 * reused when it is short and made of {@code [A-Za-z0-9._-]}, because anything else would
 * let a caller choose the text that appears inside log lines (and could carry CR/LF to
 * forge a second line). An inbound value that does not qualify is dropped and replaced
 * with a generated one, so a hostile header is never echoed and never logged.
 *
 * <p>This filter runs standalone ahead of the security chain rather than inside it, so the
 * id is present for the security filters' own output too, and so requests rejected before
 * authentication are still traceable. It reads no request body and touches no security
 * state.
 */
public final class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTRIBUTE = "correlationId";

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = idFromEarlierDispatchOrHeader(request);

        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        MDC.put(MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Never leak one request's id into the next one handled by this thread.
            MDC.remove(MDC_KEY);
        }
    }

    private String idFromEarlierDispatchOrHeader(HttpServletRequest request) {
        Object attribute = request.getAttribute(REQUEST_ATTRIBUTE);
        if (attribute instanceof String existing && SAFE_ID.matcher(existing).matches()) {
            // ERROR dispatch: keep the id the original dispatch already reported.
            return existing;
        }
        String supplied = request.getHeader(CORRELATION_HEADER);
        if (supplied != null && SAFE_ID.matcher(supplied).matches()) {
            return supplied;
        }
        return UUID.randomUUID().toString();
    }
}
