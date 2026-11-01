package org.recap.filter;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.springframework.security.web.csrf.CsrfToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Created by hemalathas on 30/3/17.
 */
public class CsrfCookieGeneratorFilterUT extends BaseTestCaseUT {

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse httpServletResponse;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    CsrfCookieGeneratorFilter csrfCookieGeneratorFilter;

    @Mock
    CsrfToken csrfToken;

    @Test
    public void testDoFilterInternal() throws ServletException, IOException {
        Mockito.when((CsrfToken) request.getAttribute("_csrf")).thenReturn(csrfToken);
        csrfCookieGeneratorFilter.doFilterInternal(request,httpServletResponse,filterChain);
        assertTrue(true);
    }

    @Test
    public void testDoFilterInternalTest() throws ServletException, IOException {
        Mockito.when((CsrfToken) request.getAttribute("_csrf")).thenReturn(csrfToken);
        String actualToken = "X-CSRF-TOKEN";
        Mockito.when(request.getHeader(actualToken)).thenReturn("X-CSRF-TOKEN");
        csrfCookieGeneratorFilter.doFilterInternal(request,httpServletResponse,filterChain);
        assertTrue(true);
    }



}