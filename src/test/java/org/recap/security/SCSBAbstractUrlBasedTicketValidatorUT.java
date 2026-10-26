package org.recap.security;

import org.apereo.cas.client.ssl.HttpURLConnectionFactory;
import org.apereo.cas.client.validation.TicketValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith({SpringExtension.class})
public class SCSBAbstractUrlBasedTicketValidatorUT {

    @Mock
    SCSBAbstractUrlBasedTicketValidator SCSBAbstractUrlBasedTicketValidator;

    @Mock
    HttpURLConnectionFactory httpURLConnectionFactory;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    public void constructValidationUrl() {
        String ticket = "ticket";
        String serviceUrl = "http://localhost:9095/requestItem/";
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("pgtUrl", "test");
        SCSBAbstractUrlBasedTicketValidator = new SCSBCas20ServiceTicketValidator("http://localhost:9095/requestItem");
        SCSBAbstractUrlBasedTicketValidator.setCustomParameters(customParameters);
        SCSBAbstractUrlBasedTicketValidator.setRenew(true);
        SCSBAbstractUrlBasedTicketValidator.setEncoding("Test");
        //ReflectionTestUtils.setField(SCSBAbstractUrlBasedTicketValidator,"logger",log);
        String validateUrl = SCSBAbstractUrlBasedTicketValidator.constructValidationUrl(ticket, serviceUrl);
        assertNotNull(validateUrl);
    }

    @Test
    public void constructValidationUrlTest() {
        String ticket = "ticket";
        String serviceUrl = "http://localhost:9095/requestItem/";
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("renew", "test");
        SCSBAbstractUrlBasedTicketValidator = new SCSBCas20ServiceTicketValidator("http://localhost:9095/requestItem");
        SCSBAbstractUrlBasedTicketValidator.setCustomParameters(customParameters);
        SCSBAbstractUrlBasedTicketValidator.setRenew(true);
        SCSBAbstractUrlBasedTicketValidator.setEncoding("Test");
        //ReflectionTestUtils.setField(SCSBAbstractUrlBasedTicketValidator,"logger",log);
        String validateUrl = SCSBAbstractUrlBasedTicketValidator.constructValidationUrl(ticket, serviceUrl);
        assertNotNull(validateUrl);
    }

    @Test
    public void constructValidationUrlasNull() {
        String ticket = "ticket";
        String serviceUrl = "http://localhost:9095/requestItem/";
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("renew", "test");
        SCSBAbstractUrlBasedTicketValidator = new SCSBCas20ServiceTicketValidator("http://localhost:9095/requestItem");
        SCSBAbstractUrlBasedTicketValidator.setCustomParameters(customParameters);
        SCSBAbstractUrlBasedTicketValidator.setRenew(false);
        SCSBAbstractUrlBasedTicketValidator.setEncoding("Test");
        //ReflectionTestUtils.setField(SCSBAbstractUrlBasedTicketValidator,"logger",log);
        String validateUrl = SCSBAbstractUrlBasedTicketValidator.constructValidationUrl(ticket, serviceUrl);
        assertNotNull(validateUrl);
    }

    @Test
    public void constructValidationUrlasNullTest() {
        String ticket = "ticket";
        String serviceUrl = "http://localhost:9095/requestItem/";
        Map<String, String> customParameters = new HashMap<>();

        customParameters.putAll(customParameters);
        SCSBAbstractUrlBasedTicketValidator = new SCSBCas20ServiceTicketValidator("http://localhost:9095/requestItem");
        SCSBAbstractUrlBasedTicketValidator.setCustomParameters(null);
        String validateUrl = SCSBAbstractUrlBasedTicketValidator.constructValidationUrl(ticket, serviceUrl);
        assertNotNull(validateUrl);
    }

    @Test
    public void validate() throws TicketValidationException {
        String ticket = "ticket";
        String serviceUrl = "http://localhost:9095/requestItem/";
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("pgtUrl", "test");
        SCSBAbstractUrlBasedTicketValidator = new SCSBCas20ServiceTicketValidator("http://localhost:9095/requestItem");
        SCSBAbstractUrlBasedTicketValidator.setCustomParameters(customParameters);
        SCSBAbstractUrlBasedTicketValidator.setRenew(true);
        SCSBAbstractUrlBasedTicketValidator.setEncoding("Test");
        SCSBAbstractUrlBasedTicketValidator.setURLConnectionFactory(httpURLConnectionFactory);
        //ReflectionTestUtils.setField(SCSBAbstractUrlBasedTicketValidator,"logger",log);
        try {
            Mockito.when(SCSBAbstractUrlBasedTicketValidator.validate(ticket, serviceUrl)).thenCallRealMethod();
            SCSBAbstractUrlBasedTicketValidator.validate(ticket, serviceUrl);
        } catch (Exception e) {
        }
    }

    @Test
    public void encodeUrl() {
        String serviceUrl = "http://localhost:9095/requestItem/";
        String result = SCSBAbstractUrlBasedTicketValidator.encodeUrl(serviceUrl);
//        assertNotNull(result);
    }

    @Test
    public void encodeUrlNull() {
        String serviceUrl = null;
        String result = SCSBAbstractUrlBasedTicketValidator.encodeUrl(serviceUrl);
        assertNull(result);
    }

    @Test
    public void getReCAPAbstractUrlBasedTicketValidator() {
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put("b", "b");
        Mockito.doCallRealMethod().when(SCSBAbstractUrlBasedTicketValidator).setCasServerUrlPrefix("http");
        Mockito.doCallRealMethod().when(SCSBAbstractUrlBasedTicketValidator).setURLConnectionFactory(httpURLConnectionFactory);
        SCSBAbstractUrlBasedTicketValidator.setURLConnectionFactory(httpURLConnectionFactory);
        SCSBAbstractUrlBasedTicketValidator.setCasServerUrlPrefix("http");
        SCSBAbstractUrlBasedTicketValidator.setCustomParameters(customParameters);
        SCSBAbstractUrlBasedTicketValidator.setRenew(true);
        SCSBAbstractUrlBasedTicketValidator.setEncoding("Test");
//        assertNotNull(SCSBAbstractUrlBasedTicketValidator.getCasServerUrlPrefix());
        assertNotNull(SCSBAbstractUrlBasedTicketValidator.getCustomParameters());
//        assertNotNull(SCSBAbstractUrlBasedTicketValidator.getEncoding());
        assertNull(SCSBAbstractUrlBasedTicketValidator.getUrlSuffix());
//        assertTrue(SCSBAbstractUrlBasedTicketValidator.isRenew());
    }
}
