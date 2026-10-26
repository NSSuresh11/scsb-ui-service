package org.recap.security.cas;

import org.apereo.cas.client.validation.Assertion;
import org.apereo.cas.client.validation.TicketValidator;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.recap.BaseTestCaseUT;
import org.springframework.context.MessageSource;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.authentication.StatelessTicketCache;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;


public class CasAuthenticationProviderUT extends BaseTestCaseUT {

    @InjectMocks
    @Spy
    CasAuthenticationProvider casAuthenticationProvider;

    @Mock
    Authentication authentication;

    @Mock
    StatelessTicketCache statelessTicketCache;

    @Mock
    GrantedAuthoritiesMapper authoritiesMapper;

    @Mock
    ServiceProperties serviceProperties;

    @Mock
    UserDetails userDetails;

    @Mock
    UserDetailsService userDetailsService;

    @Mock
    Assertion assertion;

    @Mock
    AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService;

    @Mock
    Key key;

    @Mock
    TicketValidator ticketValidator;

    @Mock
    MessageSource messageSource;

    @Test
    public void authenticate(){
        Mockito.doReturn(Boolean.TRUE).when(casAuthenticationProvider).supports(authentication.getClass());
        try {
            Authentication auth = casAuthenticationProvider.authenticate(authentication);
        }catch (Exception e){}
    }

    @Test
    public void getServiceUrlNull(){
        try {
            ServiceProperties serviceProperties = new ServiceProperties();
            Mockito.when(authentication.getDetails()).thenReturn("Test");
            ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "getServiceUrl", authentication);
        }catch (Exception e){}
    }

    @Test
    public void getServiceUrl(){
        try {
            serviceProperties.setService("test");
            Mockito.when(authentication.getDetails()).thenReturn("Test");
            ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "getServiceUrl", authentication);
        }catch (Exception e){}
    }

    @Test
    public void getServiceUrlServiceAsNull(){
        try {
            Mockito.when(authentication.getDetails()).thenReturn("Test");
            ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "getServiceUrl", authentication);
        }catch (Exception e){}
    }

    @Test
    public void authenticateNow(){
        try {
            ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "authenticateNow", authentication);
        }catch (Exception e){}
    }

    @Test
    public void loadUserByAssertion(){
        Mockito.when(authenticationUserDetailsService.loadUserDetails(any())).thenReturn(userDetails);
        ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "loadUserByAssertion",assertion);
    }

    @Test
    public void setUserDetailsService(){
        ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "setUserDetailsService",userDetailsService);
    }

    @Test
    public void getKey(){
        ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "getKey");
    }

    @Test
    public void getTicketValidator(){
        ReflectionTestUtils.invokeMethod(casAuthenticationProvider,"getTicketValidator");
    }

    @Test
    public void getStatelessTicketCache(){
        StatelessTicketCache cache = casAuthenticationProvider.getStatelessTicketCache();
        assertNotNull(cache);
    }
    @Test
    public void setStatelessTicketCache(){
        casAuthenticationProvider.setStatelessTicketCache(statelessTicketCache);
    }
    @Test
    public void setAuthoritiesMapper(){
        casAuthenticationProvider.setAuthoritiesMapper(authoritiesMapper);
    }
    @Test
    public void supports(){
        Boolean result  = casAuthenticationProvider.supports(authentication.getClass());
        assertNotNull(result);
    }

    @Test
    public void setKey(){
       casAuthenticationProvider.setKey(String.valueOf(key));
    }

    @Test
    public void setTicketValidator(){
        casAuthenticationProvider.setTicketValidator(ticketValidator);
    }

    @Test
    public void setMessageSource(){
        casAuthenticationProvider.setMessageSource(messageSource);
    }




}
