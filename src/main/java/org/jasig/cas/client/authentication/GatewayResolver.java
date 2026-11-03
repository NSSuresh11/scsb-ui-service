package org.jasig.cas.client.authentication;

import jakarta.servlet.http.HttpServletRequest;

public interface GatewayResolver {

    /**
     * Determines if the request has  been gatewayed already.  Should also do gateway clean up.
     *
     * @param request the Http Servlet Request
     * @param serviceUrl the service url
     * @return true if yes, false otherwise.
     */
    boolean hasGatewayedAlready(HttpServletRequest request, String serviceUrl);

    /**
     * Storage the request for gatewaying and return the service url, which can be modified.
     *
     * @param request the HttpServletRequest.
     * @param serviceUrl the service url
     * @return the potentially modified service url to redirect to
     */
    String storeGatewayInformation(HttpServletRequest request, String serviceUrl);
}
