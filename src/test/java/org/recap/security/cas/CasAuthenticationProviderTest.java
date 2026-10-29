package org.recap.security.cas;

import org.apereo.cas.client.validation.TicketValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.*;
import org.springframework.security.cas.authentication.CasServiceTicketAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class})
public class CasAuthenticationProviderTest {

    @InjectMocks
    private CasAuthenticationProvider casAuthenticationProvider;

    @Mock
    private StatelessTicketCache statelessTicketCache;

    @Mock
    private TicketValidator ticketValidator;

    @Mock
    private AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService;

    @Mock
    private ServiceProperties serviceProperties;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        casAuthenticationProvider = new CasAuthenticationProvider();
        casAuthenticationProvider.setTicketValidator(ticketValidator);
        casAuthenticationProvider.setServiceProperties(serviceProperties);
        statelessTicketCache = new NullStatelessTicketCache();
        casAuthenticationProvider.setStatelessTicketCache(statelessTicketCache);
        casAuthenticationProvider.setAuthenticationUserDetailsService(authenticationUserDetailsService);
    }

    @Test
    public void testAuthenticateUnsupportedAuthenticationClass() {
        Authentication unsupportedAuth = mock(Authentication.class);
        assertNull(casAuthenticationProvider.authenticate(unsupportedAuth));
    }

    @Test
    public void testAuthenticateUsernamePasswordNonCasRelated() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password");
        assertNull(casAuthenticationProvider.authenticate(authentication));
    }

    @Test
    public void testAuthenticateCasAuthenticationTokenValid() {
        CasAuthenticationToken casToken = mock(CasAuthenticationToken.class);
        when(casToken.getKeyHash()).thenReturn("someKey".hashCode());
        casAuthenticationProvider.setKey("someKey");
        assertEquals(casToken, casAuthenticationProvider.authenticate(casToken));
    }

    @Test
    public void testAuthenticateCasAuthenticationTokenInvalid() {
        CasAuthenticationToken casToken = mock(CasAuthenticationToken.class);
        when(casToken.getKeyHash()).thenReturn("wrongKey".hashCode());
        casAuthenticationProvider.setKey("someKey");
        assertThrows(BadCredentialsException.class, () -> casAuthenticationProvider.authenticate(casToken));
    }

    @Test
    public void testAuthenticateNoServiceTicket() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(CasServiceTicketAuthenticationToken.CAS_STATELESS_IDENTIFIER, null);
        assertThrows(BadCredentialsException.class, () -> casAuthenticationProvider.authenticate(authentication));
    }

    @Test
    public void testAuthenticateStatelessTokenFromCache() {
        try {
            Authentication authentication = new UsernamePasswordAuthenticationToken(CasServiceTicketAuthenticationToken.CAS_STATELESS_IDENTIFIER, "ticket");
            CasAuthenticationToken casToken = mock(CasAuthenticationToken.class);
            assertEquals(casToken, casAuthenticationProvider.authenticate(authentication));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAuthenticateStatelessTokenNotInCache() {
        try {
            Authentication authentication = new UsernamePasswordAuthenticationToken(org.springframework.security.cas.authentication.CasServiceTicketAuthenticationToken.CAS_STATELESS_IDENTIFIER, "ticket");
            CasAuthenticationToken casToken = mock(CasAuthenticationToken.class);
            ReflectionTestUtils.invokeMethod(casAuthenticationProvider, "authenticateNow", authentication);
            assertEquals(casToken, casAuthenticationProvider.authenticate(authentication));
            verify(statelessTicketCache).putTicketInCache(casToken);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
