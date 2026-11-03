package org.jasig.cas.client.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ErrorRedirectFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<ErrorHolder> errors = new ArrayList<ErrorHolder>();

    private String defaultErrorRedirectPage;

    @Override
    public void destroy() {
        // nothing to do here
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            filterChain.doFilter(request, response);
        } catch (final Exception e) {
            final Throwable t = extractErrorToCompare(e);
            ErrorHolder currentMatch = null;
            for (final ErrorHolder errorHolder : this.errors) {
                if (errorHolder.exactMatch(t)) {
                    currentMatch = errorHolder;
                    break;
                } else if (errorHolder.inheritanceMatch(t)) {
                    currentMatch = errorHolder;
                }
            }

            if (currentMatch != null) {
                httpResponse.sendRedirect(currentMatch.getUrl());
            } else {
                httpResponse.sendRedirect(defaultErrorRedirectPage);
            }
        }
    }

    /**
     * Determine which error to use for comparison.  If there is an {@link Throwable#getCause()} then that will be used. Otherwise, the original throwable is used.
     *
     * @param throwable the throwable to look for a root cause.
     * @return the throwable to use for comparison.  MUST NOT BE NULL.
     */
    private Throwable extractErrorToCompare(final Throwable throwable) {
        final Throwable cause = throwable.getCause();

        if (cause != null) {
            return cause;
        }

        return throwable;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        this.defaultErrorRedirectPage = filterConfig.getInitParameter("defaultErrorRedirectPage");

        final Enumeration<?> enumeration = filterConfig.getInitParameterNames();
        while (enumeration.hasMoreElements()) {
            final String className = (String) enumeration.nextElement();
            try {
                if (!className.equals("defaultErrorRedirectPage")) {
                    this.errors.add(new ErrorHolder(className, filterConfig.getInitParameter(className)));
                }
            } catch (final ClassNotFoundException e) {
                logger.warn("Class [{}] cannot be found in ClassLoader.  Ignoring.", className);
            }
        }
    }

    protected final class ErrorHolder {

        private Class<?> className;

        private String url;

        protected ErrorHolder(final String className, final String url) throws ClassNotFoundException {
            this.className = Class.forName(className);
            this.url = url;
        }

        public boolean exactMatch(final Throwable e) {
            return this.className.equals(e.getClass());
        }

        public boolean inheritanceMatch(final Throwable e) {
            return className.isAssignableFrom(e.getClass());
        }

        public String getUrl() {
            return this.url;
        }
    }
}
