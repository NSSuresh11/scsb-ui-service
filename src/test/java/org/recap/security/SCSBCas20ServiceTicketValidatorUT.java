package org.recap.security;


import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.client.proxy.ProxyGrantingTicketStorage;
import org.apereo.cas.client.proxy.ProxyGrantingTicketStorageImpl;
import org.apereo.cas.client.proxy.ProxyRetriever;
import org.apereo.cas.client.validation.TicketValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class SCSBCas20ServiceTicketValidatorUT extends BaseTestCaseUT {

    @Mock
    SCSBCas20ServiceTicketValidator reCAPCas20ServiceTicketValidator;

    @Mock
    ProxyGrantingTicketStorage proxyGrantingTicketStorage;

    @Mock
    ProxyRetriever proxyRetriever;
    @Mock
    HttpServletResponse httpServletResponse;



    @Test
    public void parseResponseFromServerTicketValidation() throws Exception{
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<note>\n" +
                "  <user>Jani</user>\n" +
                "  <proxyGrantingTicket>235665431117</proxyGrantingTicket>\n" +
                "  <body>Renew your book before existing due date!</body>\n" +
                "</note>";
        reCAPCas20ServiceTicketValidator.parseResponseFromServer(response);
    }
    @Test
    public void parseResponseFromServerTicketValidationException() throws Exception{
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<note>\n" +
                "  <authenticationFailure>Invalid User</authenticationFailure>\n" +
                "  <from>Jani</from>\n" +
                "  <heading>Reminder</heading>\n" +
                "  <body>Renew your book before existing due date!</body>\n" +
                "</note>";
        try {
            reCAPCas20ServiceTicketValidator.parseResponseFromServer(response);
        }catch (TicketValidationException e){
            assertNotNull(e);
            assertEquals("Invalid User",e.getMessage());
        }
    }
    @Test
    public void parseResponseFromServerNoprincipal() throws Exception{
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<note>\n" +
                "  <from>Jani</from>\n" +
                "  <heading>Reminder</heading>\n" +
                "  <body>Renew your book before existing due date!</body>\n" +
                "</note>";
        try {
            reCAPCas20ServiceTicketValidator.parseResponseFromServer(response);
        }catch (Exception e){
            assertNotNull(e);
            assertEquals("No principal was found in the response from the CAS server.",e.getMessage());
        }
    }
    @Test
    public void setProxyCallbackUrl() throws Exception{
        String proxyCallbackUrl = "http://localhost:9095/requestItem/patronValidationBulkRequest";
        reCAPCas20ServiceTicketValidator.setProxyCallbackUrl(proxyCallbackUrl);
    }
    @Test
    public void extractCustomAttributes() throws Exception{
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<note>\n" +
                "  <to>Tove</to>\n" +
                "  <from>Jani</from>\n" +
                "  <heading>Reminder</heading>\n" +
                "  <body>Renew your book before existing due date!</body>\n" +
                "</note>";
        Mockito.doCallRealMethod().when(reCAPCas20ServiceTicketValidator).extractCustomAttributes(xml);
        Map<String, Object> result = reCAPCas20ServiceTicketValidator.extractCustomAttributes(xml);
        assertNotNull(result);
    }
    @Test
    public void extractCustomAttributesException() throws Exception{
        String xml = "testXML";
        //ReflectionTestUtils.setField(reCAPCas20ServiceTicketValidator,"logger",log);
        Mockito.doCallRealMethod().when(reCAPCas20ServiceTicketValidator).extractCustomAttributes(xml);
        Map<String, Object> result = reCAPCas20ServiceTicketValidator.extractCustomAttributes(xml);
        assertNotNull(result);
    }

    @Test
    public void setProxyGrantingTicketStorage(){
        ProxyGrantingTicketStorage proxyGrantingTicketStorage = new ProxyGrantingTicketStorageImpl();
        reCAPCas20ServiceTicketValidator.setProxyGrantingTicketStorage(proxyGrantingTicketStorage);
    }
    @Test
    public void setProxyRetriever(){
        reCAPCas20ServiceTicketValidator.setProxyRetriever(proxyRetriever);
    }

    @Test
    public void getProxyCallbackUrl(){
        String proxyUrl = reCAPCas20ServiceTicketValidator.getProxyCallbackUrl();
        assertNull(proxyUrl);
    }
    @Test
    public void getProxyGrantingTicketStorage(){
        ProxyGrantingTicketStorage proxyGrantingTicketStorage = reCAPCas20ServiceTicketValidator.getProxyGrantingTicketStorage();
        assertNull(proxyGrantingTicketStorage);
    }
    @Test
    public void getProxyRetriever(){
        SCSBCas20ServiceTicketValidator reCAPCas20ServiceTicketValidator = new SCSBCas20ServiceTicketValidator("http://localhost:9980/testurl");
        ProxyRetriever proxyRetriever = reCAPCas20ServiceTicketValidator.getProxyRetriever();
        assertNotNull(proxyRetriever);
    }

    @Test
    public void populateUrlAttributeMap(){
        Map<String, String> urlParameters = new HashMap<>();
        reCAPCas20ServiceTicketValidator.populateUrlAttributeMap(urlParameters);
    }

    @Test
    public void getUrlSuffix(){
        Mockito.doCallRealMethod().when(reCAPCas20ServiceTicketValidator).getUrlSuffix();
        String urlSuffix = reCAPCas20ServiceTicketValidator.getUrlSuffix();
        assertNotNull(urlSuffix);
    }

}
