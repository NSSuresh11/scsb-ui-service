package org.recap.filter;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbConstants;
import org.recap.util.UserAuthUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class SCSBValidationFilterUT extends BaseTestCaseUT {

    @InjectMocks
    @Spy
    SCSBValidationFilter scsbValidationFilter;

    @Mock
    UserAuthUtil userAuthUtil;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpSession session;

    @Mock
    HttpServletResponse servletResponse;

    @Mock
    FilterChain filterChain;

    @Test
    public void doFilter() throws ServletException, IOException {
        Optional<String> API_PATH = Optional.ofNullable("Header");
        Mockito.when(request.getSession(ScsbConstants.FALSE)).thenReturn(session);
        Mockito.when(request.getHeader(ScsbConstants.API_PATH)).thenReturn("Header");
        Mockito.when(userAuthUtil.isAuthenticated(session, ScsbConstants.AUTH_PATH + API_PATH.get())).thenReturn(Boolean.TRUE);
        Mockito.when(scsbValidationFilter.getUserAuthUtil()).thenReturn(userAuthUtil);
        Mockito.doNothing().when(filterChain).doFilter(any(), any());
        scsbValidationFilter.doFilter(request,servletResponse,filterChain);
    }
    @Test
    public void doFilterException() throws ServletException, IOException {
        Mockito.when(request.getSession(ScsbConstants.FALSE)).thenThrow(new NullPointerException());
        scsbValidationFilter.doFilter(request,servletResponse,filterChain);
    }


    @Test
    public void getUserAuthUtil(){
        SCSBValidationFilter scsbValidationFilter = new SCSBValidationFilter();
        try {
            scsbValidationFilter.getUserAuthUtil();
        }catch (Exception e){}
    }
}
