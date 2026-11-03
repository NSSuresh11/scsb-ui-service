package org.jasig.cas.client.validation;

import jakarta.servlet.FilterConfig;

import org.jasig.cas.client.Protocol;
import org.jasig.cas.client.configuration.ConfigurationKeys;
import org.jasig.cas.client.ssl.HttpURLConnectionFactory;
import org.jasig.cas.client.ssl.HttpsURLConnectionFactory;

public class Cas10TicketValidationFilter extends AbstractTicketValidationFilter {

    public Cas10TicketValidationFilter() {
        super(Protocol.CAS1);
    }

    @Override
    protected final TicketValidator getTicketValidator(final FilterConfig filterConfig) {
        final String casServerUrlPrefix = getString(ConfigurationKeys.CAS_SERVER_URL_PREFIX);
        final Cas10TicketValidator validator = new Cas10TicketValidator(casServerUrlPrefix);
        validator.setRenew(getBoolean(ConfigurationKeys.RENEW));

        final HttpURLConnectionFactory factory = new HttpsURLConnectionFactory(getHostnameVerifier(),
                getSSLConfig());
        validator.setURLConnectionFactory(factory);
        validator.setEncoding(getString(ConfigurationKeys.ENCODING));

        return validator;
    }
}
