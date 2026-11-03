package org.jasig.cas.client.jaas;

import java.io.IOException;
import java.security.GeneralSecurityException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.jasig.cas.client.Protocol;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.util.CommonUtils;

public final class Servlet3AuthenticationFilter extends AbstractCasFilter {

    public Servlet3AuthenticationFilter() {
        super(Protocol.CAS2);
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        final HttpSession session = request.getSession();
        final String ticket = CommonUtils.safeGetParameter(request, getProtocol().getArtifactParameterName());

        if (session != null && session.getAttribute(CONST_CAS_ASSERTION) == null && ticket != null) {
            try {
                final String service = constructServiceUrl(request, response);
                logger.debug("Attempting CAS ticket validation with service={} and ticket={}", service, ticket);
                request.login(service, ticket);
                if (request.getUserPrincipal() instanceof AssertionPrincipal) {
                    final AssertionPrincipal principal = (AssertionPrincipal) request.getUserPrincipal();
                    logger.debug("Installing CAS assertion into session.");
                    request.getSession().setAttribute(CONST_CAS_ASSERTION, principal.getAssertion());
                } else {
                    logger.debug("Aborting -- principal is not of type AssertionPrincipal");
                    throw new GeneralSecurityException("JAAS authentication did not produce CAS AssertionPrincipal.");
                }
            } catch (final ServletException e) {
                logger.debug("JAAS authentication failed.");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } catch (final GeneralSecurityException e) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            }
        } else if (session != null && request.getUserPrincipal() == null) {
            // There is evidence that in some cases the principal can disappear
            // in JBoss despite a valid session.
            // This block forces consistency between principal and assertion.
            logger.info("User principal not found.  Removing CAS assertion from session to force re-authentication.");
            session.removeAttribute(CONST_CAS_ASSERTION);
        }
        chain.doFilter(request, response);
    }
}
