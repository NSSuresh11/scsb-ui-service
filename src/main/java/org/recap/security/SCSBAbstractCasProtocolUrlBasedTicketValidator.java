package org.recap.security;

import lombok.extern.slf4j.Slf4j;
import org.jasig.cas.client.util.CommonUtils;

import java.net.URL;

/**
 * Created by sheiks on 25/01/17.
 */
@Slf4j
public abstract class SCSBAbstractCasProtocolUrlBasedTicketValidator extends SCSBAbstractUrlBasedTicketValidator {

    /**
     * Instantiates a new ReCAPAbstractCasProtocolUrlBasedTicketValidator.
     *
     * @param casServerUrlPrefix the cas server url prefix
     */
    protected SCSBAbstractCasProtocolUrlBasedTicketValidator(final String casServerUrlPrefix) {
        super(casServerUrlPrefix);
    }

    /**
     * Contacts the CAS Server to retrieve the response for the ticket validation.
     *
     * @param validationUrl the url to send the validation request to.
     * @param ticket the ticket to validate.
     * @return the response from the CAS server.
     */
    protected final String retrieveResponseFromServer(final URL validationUrl, final String ticket) {
        log.info("validationUrl >>>>>>> " + validationUrl);
        log.info("getURLConnectionFactory() >>>>>>> " + getURLConnectionFactory());
        log.info("getEncoding() >>>>>>> " + getEncoding());
        return CommonUtils.getResponseFromServer(validationUrl, getURLConnectionFactory(), getEncoding());
    }
}

