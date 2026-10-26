package org.recap.service;

import org.apereo.cas.client.authentication.AttributePrincipal;
import org.apereo.cas.client.validation.Assertion;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Created by hemalathas on 30/3/17.
 */
public class CustomUserDetailsServiceUT extends BaseTestCaseUT {

    @Mock
    Assertion assertion;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    AttributePrincipal principal;

    @Test
    public void testLoadUserDetails() throws Exception{
        Set<String> admins = new HashSet<>();
        admins.add("admin");
        customUserDetailsService = new CustomUserDetailsService(admins);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("a", Arrays.asList("CN=role_a1,OU=roles,DC=spring,DC=io", "CN=role_a2,OU=roles,DC=spring,DC=io"));
        attributes.put("b", "b");
        attributes.put("c", "c");
        attributes.put("d", null);
        attributes.put("someother", "unused");
        Mockito.when(principal.getName()).thenReturn("admin");
        Mockito.when(assertion.getPrincipal()).thenReturn(principal);
        CasAssertionAuthenticationToken token = new CasAssertionAuthenticationToken(assertion, "ticket");
        UserDetails userDetails = customUserDetailsService.loadUserDetails(token);
        assertNotNull(userDetails);
    }
    @Test
    public void testLoadUserDetailsWithoutAdmin() throws Exception{
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("a", Arrays.asList("CN=role_a1,OU=roles,DC=spring,DC=io", "CN=role_a2,OU=roles,DC=spring,DC=io"));
        attributes.put("b", "b");
        attributes.put("c", "c");
        attributes.put("d", null);
        attributes.put("someother", "unused");
        Mockito.when(principal.getName()).thenReturn("admin");
        Mockito.when(assertion.getPrincipal()).thenReturn(principal);
        CasAssertionAuthenticationToken token = new CasAssertionAuthenticationToken(assertion, "ticket");
        UserDetails userDetails = customUserDetailsService.loadUserDetails(token);
        assertNotNull(userDetails);
    }
}