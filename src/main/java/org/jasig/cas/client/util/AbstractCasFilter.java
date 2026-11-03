package org.jasig.cas.client.util;

import org.jasig.cas.client.Protocol;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jasig.cas.client.configuration.ConfigurationKeys;

import java.util.Arrays;

public abstract class AbstractCasFilter extends AbstractConfigurationFilter {

    /** Represents the constant for where the assertion will be located in memory. */
    public static final String CONST_CAS_ASSERTION = "_const_cas_assertion_";

    private final Protocol protocol;

    /** Sets where response.encodeUrl should be called on service urls when constructed. */
    private boolean encodeServiceUrl = true;

    /**
     * The name of the server.  Should be in the following format: {protocol}:{hostName}:{port}.
     * Standard ports can be excluded. */
    private String serverName;

    /** The exact url of the service. */
    private String service;

    protected AbstractCasFilter(final Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public final void init(final FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        if (!isIgnoreInitConfiguration()) {
            setServerName(getString(ConfigurationKeys.SERVER_NAME));
            setService(getString(ConfigurationKeys.SERVICE));
            setEncodeServiceUrl(getBoolean(ConfigurationKeys.ENCODE_SERVICE_URL));

            initInternal(filterConfig);
        }
        init();
    }


    /** Controls the ordering of filter initialization and checking by defining a method that runs before the init.
     * @param filterConfig the original filter configuration.
     * @throws ServletException if there is a problem.
     *
     */
    protected void initInternal(final FilterConfig filterConfig) throws ServletException {
        // template method
    }

    /**
     * Initialization method.  Called by Filter's init method or by Spring.  Similar in concept to the InitializingBean interface's
     * afterPropertiesSet();
     */
    public void init() {
        CommonUtils.assertTrue(CommonUtils.isNotEmpty(this.serverName) || CommonUtils.isNotEmpty(this.service),
                "serverName or service must be set.");
        CommonUtils.assertTrue(CommonUtils.isBlank(this.serverName) || CommonUtils.isBlank(this.service),
                "serverName and service cannot both be set.  You MUST ONLY set one.");
    }

    // empty implementation as most filters won't need this.
    @Override
    public void destroy() {
        // nothing to do
    }

    protected final String constructServiceUrl(final HttpServletRequest request, final HttpServletResponse response) {
        return CommonUtils.constructServiceUrl(request, response, this.service, this.serverName,
                this.protocol.getServiceParameterName(),
                this.protocol.getArtifactParameterName(), this.encodeServiceUrl);
    }

    /**
     * Note that trailing slashes should not be used in the serverName.  As a convenience for this common misconfiguration, we strip them from the provided
     * value.
     *
     * @param serverName the serverName. If this method is called, this should not be null.  This AND service should not be both configured.
     */
    public final void setServerName(final String serverName) {
        if (serverName != null && serverName.endsWith("/")) {
            this.serverName = serverName.substring(0, serverName.length() - 1);
            logger.info("Eliminated extra slash from serverName [{}].  It is now [{}]", serverName, this.serverName);
        } else {
            this.serverName = serverName;
        }
    }

    public final void setService(final String service) {
        this.service = service;
    }

    public final void setEncodeServiceUrl(final boolean encodeServiceUrl) {
        this.encodeServiceUrl = encodeServiceUrl;
    }

    protected Protocol getProtocol() {
        return this.protocol;
    }

    /**
     * Template method to allow you to change how you retrieve the ticket.
     *
     * @param request the HTTP ServletRequest.  CANNOT be NULL.
     * @return the ticket if its found, null otherwise.
     */
    protected String retrieveTicketFromRequest(final HttpServletRequest request) {
        return CommonUtils.safeGetParameter(request, this.protocol.getArtifactParameterName(),
                Arrays.asList(this.protocol.getArtifactParameterName()));
    }
}
