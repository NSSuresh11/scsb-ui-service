package org.recap.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sanitizes suspicious header values (e.g. ${jndi:...}) before Spring parses them.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HeaderSanitizationFilter extends OncePerRequestFilter {

    @Value("${security.headers.sanitize.enabled:true}")
    private boolean enabled;

    @Value("${security.headers.sanitize.mode:reject}")
    private String mode;

    @Value("${security.headers.sanitize.headers:Forwarded,X-Forwarded-Host,X-Forwarded-For,X-Forwarded-Proto,Host,Accept,Content-Type,User-Agent,Referer}")
    private String headersToCheckProperty;

    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
            "(\\$\\{)|(?i)\\b(jndi:|ldap:|ldaps:|rmi:|dns:|file:)"
    );

    private List<String> headersToCheck;

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        headersToCheck = Arrays.stream(headersToCheckProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        Map<String, List<String>> suspicious = findSuspiciousHeaders(request);

        if (suspicious.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("strip".equalsIgnoreCase(mode)) {
            HttpServletRequest wrapped = new SanitizedHeaderRequestWrapper(request, suspicious);
            filterChain.doFilter(wrapped, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Malformed or malicious header detected");
        }
    }

    private Map<String, List<String>> findSuspiciousHeaders(HttpServletRequest request) {
        Map<String, List<String>> suspicious = new LinkedHashMap<>();

        for (String header : headersToCheck) {
            Enumeration<String> values = request.getHeaders(header);
            if (values == null) continue;
            while (values.hasMoreElements()) {
                String v = values.nextElement();
                if (isSuspicious(v)) {
                    suspicious.computeIfAbsent(header, k -> new ArrayList<>()).add(v);
                }
            }
        }
        return suspicious;
    }

    private boolean isSuspicious(String value) {
        if (value == null) return false;
        String t = value.trim();
        if (t.isEmpty()) return false;
        if (MALICIOUS_PATTERN.matcher(t).find()) return true;
        for (char c : t.toCharArray()) {
            if (c < 32 && c != '\t' && c != '\r' && c != '\n') return true;
        }
        return false;
    }

    private static class SanitizedHeaderRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, List<String>> sanitizedHeaders;

        SanitizedHeaderRequestWrapper(HttpServletRequest request, Map<String, List<String>> suspiciousMap) {
            super(request);
            sanitizedHeaders = new LinkedHashMap<>();
            Enumeration<String> names = request.getHeaderNames();
            if (names != null) {
                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    List<String> kept = Collections.list(request.getHeaders(name)).stream()
                            .filter(v -> !suspiciousMap.getOrDefault(name, List.of()).contains(v))
                            .collect(Collectors.toList());
                    if (!kept.isEmpty()) sanitizedHeaders.put(name, kept);
                }
            }
        }

        @Override
        public String getHeader(String name) {
            List<String> list = sanitizedHeaders.get(name);
            return (list == null || list.isEmpty()) ? null : list.get(0);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> list = sanitizedHeaders.get(name);
            return Collections.enumeration(list == null ? List.of() : list);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(sanitizedHeaders.keySet());
        }
    }
}