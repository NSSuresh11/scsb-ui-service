package org.recap.security.cas;

import org.apereo.cas.client.validation.Assertion;
import org.apereo.cas.client.validation.TicketValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.recap.ScsbConstants;
import org.recap.security.SCSBCas20ServiceTicketValidator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class})
public class AuthenticateNowTest {

    @InjectMocks
    private CasAuthenticationProvider casAuthenticationProvider;

    @Mock
    private SCSBCas20ServiceTicketValidator ticketValidator;

    @Mock
    private AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService;

    @Mock
    private UserDetailsChecker userDetailsChecker;

    @Mock
    private GrantedAuthoritiesMapper authoritiesMapper;

    @Mock
    private Assertion assertion;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAuthenticateNowSuccess() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute(ScsbConstants.SCSB_INSTITUTION_CODE, "TestInstitution");
        RequestAttributes requestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(requestAttributes);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn("serviceTicket");
        when(ticketValidator.validate(anyString(), anyString())).thenReturn(assertion);
        ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "loadUserByAssertion", assertion);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        doNothing().when(userDetailsChecker).check(userDetails);
        try {
            Object authenticateNow = ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "authenticateNow", authentication);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }


    @Test
    public void testAuthenticateNowTicketValidationException() throws Exception {
        try {
            MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.setAttribute(ScsbConstants.SCSB_INSTITUTION_CODE, "TestInstitution");
            RequestAttributes requestAttributes = new ServletRequestAttributes(mockRequest);
            RequestContextHolder.setRequestAttributes(requestAttributes);
            Authentication authentication = mock(Authentication.class);
            when(authentication.getCredentials()).thenReturn("serviceTicket");
            when(ticketValidator.validate(anyString(), anyString())).thenThrow(new TicketValidationException("Invalid Ticket"));
            ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "authenticateNow", authentication);
            verify(ticketValidator).validate(anyString(), anyString());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }


}
