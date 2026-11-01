package org.recap.util;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Created by hemalathas on 31/3/17.
 */
public class HelperUtilUT extends BaseTestCaseUT {

    @InjectMocks
    HelperUtil mockedHelperUtil;

    @Mock
    Cookie cookie;

    @Mock
    HttpServletRequest httpServletRequest;

    @Mock
    HttpServletRequest request;

    @Mock
    Authentication auth;

    @Mock
    File file;

    @Mock
    Collection<GrantedAuthority> authorities;

    @Test
    public void testGetAttributeValueFromRequest(){
        HelperUtil helperUtil = new HelperUtil();
        String response = helperUtil.getAttributeValueFromRequest(httpServletRequest,"test");
        assertNull(response);
    }

    @Test
    public void testGetLogoutUrl() throws Exception{
        try {
        String  institutionCode = "NYPL";
        HelperUtil helperUtil = new HelperUtil();
        String casLogoutUrl = helperUtil.getLogoutUrl(institutionCode);
        assertNotNull(casLogoutUrl);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testGetLogoutUrl2() throws Exception{
        try {
            String institutionCode = "PUL";
            HelperUtil helperUtil = new HelperUtil();
            String casLogoutUrl = helperUtil.getLogoutUrl(institutionCode);
            assertNotNull(casLogoutUrl);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testLogoutFromShiro() throws Exception{
        Object attribute = "scsb" ;
        HelperUtil helperUtil = new HelperUtil();
        helperUtil.logoutFromShiro(attribute);

    }
    @Test
    public void testIsAnonymousUser() throws Exception{
        Mockito.when(auth.getName()).thenReturn(ScsbConstants.ANONYMOUS_USER);
        boolean result=mockedHelperUtil.isAnonymousUser(auth);
        assertNotNull(result);
    }

    @Test
    public void setCookieProperties(){
        mockedHelperUtil.setCookieProperties(cookie);
    }

    @Test
    public void getFileContent() throws Exception {
        File file = getBibContentFile();
        byte[] bytes = mockedHelperUtil.getFileContent(file,"csv","test");
        assertNotNull(bytes);
    }

    private File getBibContentFile() throws URISyntaxException {
        URL resource = null;
        resource = getClass().getResource("BulkRequest.csv");
        return new File(resource.toURI());
    }

    @Test
    public void getLogoutUrl(){
        try {
            String casLogoutUrl = mockedHelperUtil.getLogoutUrl("HD");
            assertNotNull(casLogoutUrl);
        }catch (Exception e){}
    }

    @Test
    public void getInstitutionFromRequestIsBlank(){
        Mockito.when(request.getParameter("institution")).thenReturn("");
        Mockito.when(request.getAttribute(ScsbConstants.SCSB_INSTITUTION_CODE)).thenReturn("HD");
        String institution = mockedHelperUtil.getInstitutionFromRequest(request);
        assertNotNull(institution);
    }

    @Test
    public void getInstitutionFromRequest(){
        Mockito.when(request.getParameter("institution")).thenReturn("HD");
        String institution = mockedHelperUtil.getInstitutionFromRequest(request);
        assertNotNull(institution);
    }

}