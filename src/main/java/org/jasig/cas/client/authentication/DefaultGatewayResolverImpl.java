package org.jasig.cas.client.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class DefaultGatewayResolverImpl implements GatewayResolver {

    public static final String CONST_CAS_GATEWAY = "_const_cas_gateway_";

    @Override
    public boolean hasGatewayedAlready(final HttpServletRequest request, final String serviceUrl) {
        final HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        final boolean result = session.getAttribute(CONST_CAS_GATEWAY) != null;
        return result;
    }

    @Override
    public String storeGatewayInformation(final HttpServletRequest request, final String serviceUrl) {
        request.getSession(true).setAttribute(CONST_CAS_GATEWAY, "yes");
        return serviceUrl;
    }
}
