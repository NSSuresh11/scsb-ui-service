package org.recap.util;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbConstants;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.service.RestHeaderService;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Created by dharmendrag on 7/2/17.
 */
public class UserAuthUtilUT extends BaseTestCaseUT {

    @InjectMocks
    UserAuthUtil userAuthUtil;

    @Mock
    RestHeaderService restHeaderService;

    @Mock
    RestTemplate restTemplate;

    @Mock
    UsernamePasswordToken token;

    @Mock
    HttpSession session;

    @Mock
    HttpServletRequest httpServletRequest;

    @Mock
    HttpSession httpSession;


    @Test
    public void testdoAuthentication() {
        Mockito.when(session.getAttribute(Mockito.anyString())).thenReturn(true);
        Mockito.when(session.getAttribute(ScsbConstants.USER_INSTITUTION)).thenReturn(1);
        UserDetailsForm userDetailsForm=userAuthUtil.getUserDetails(session,"");
        assertTrue(userDetailsForm.isRepositoryUser());
    }

    @Test
    public void testLoginAndAuthorizationExceptionLogOut() {
        boolean authorized = userAuthUtil.authorizedUser(ScsbConstants.LOGOUT, token);
        assertFalse(authorized);
    }

    @Test
    public void testLoginAndAuthorException() {
        Mockito.when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        Mockito.when(httpSession.getAttribute(ScsbConstants.USER_TOKEN)).thenReturn(token);
        boolean authorized = userAuthUtil.isAuthenticated(httpServletRequest, ScsbConstants.SCSB_SHIRO_COLLECTION_URL);
        assertFalse(authorized);
    }


}
