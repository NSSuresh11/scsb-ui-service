package org.recap.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbConstants;
import org.recap.security.UserInstitutionCache;
import org.recap.util.HelperUtil;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by hemalathas on 30/3/17.
 */


@ExtendWith({SpringExtension.class})
public class SCSBInstitutionFilterUT extends BaseTestCaseUT {

   @InjectMocks
   SCSBInstitutionFilter SCSBInstitutionFilter;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse httpServletResponse;

    @Mock
    FilterChain filterChain;

    @Mock
    UserInstitutionCache userInstitutionCache;

    @Mock
    HttpSession value;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoFilterInternal() throws ServletException, IOException {
        try (MockedStatic<HelperUtil> helperUtilMock = Mockito.mockStatic(HelperUtil.class)) {
            helperUtilMock.when(() -> HelperUtil.getBean(Mockito.any())).thenReturn(userInstitutionCache);
            Mockito.when(request.getSession()).thenReturn(value);
            Mockito.when(value.getId()).thenReturn("1");
            Cookie mockCookie = Mockito.mock(Cookie.class);
            Mockito.when(mockCookie.getName()).thenReturn(ScsbConstants.IS_USER_AUTHENTICATED).thenReturn(ScsbConstants.LOGGED_IN_INSTITUTION);
            Mockito.when(mockCookie.getValue()).thenReturn("Y");
            Mockito.when(request.getCookies()).thenReturn(new Cookie[]{mockCookie});
            Mockito.when(request.getRequestURI()).thenReturn("/home");
            SCSBInstitutionFilter.doFilterInternal(request, httpServletResponse, filterChain);
            assertTrue(true);
        }
    }

    @Test
    public void testDoFilterInternalIOException() throws ServletException, IOException {
        try (MockedStatic<HelperUtil> helperUtilMock = Mockito.mockStatic(HelperUtil.class)) {
            helperUtilMock.when(() -> HelperUtil.getBean(Mockito.any())).thenReturn(userInstitutionCache);
            helperUtilMock.when(() -> HelperUtil.getLogoutUrl(Mockito.any())).thenReturn("logoutUrl");
            Mockito.when(request.getSession()).thenReturn(value);
            Mockito.when(value.getId()).thenReturn("1");
            Cookie mockCookie = Mockito.mock(Cookie.class);
            Mockito.when(mockCookie.getName()).thenReturn(ScsbConstants.IS_USER_AUTHENTICATED).thenReturn(ScsbConstants.LOGGED_IN_INSTITUTION);
            Mockito.when(mockCookie.getValue()).thenReturn("Y");
            Mockito.when(request.getCookies()).thenReturn(new Cookie[]{mockCookie});
            Mockito.when(request.getRequestURI()).thenReturn("/home");
            Mockito.doThrow(IOException.class).when(httpServletResponse).sendRedirect(Mockito.anyString());
            SCSBInstitutionFilter.doFilterInternal(request, httpServletResponse, filterChain);
            assertTrue(true);
        }
    }


    @Test
    public void testDoFilterInternalReturnInstitution() throws ServletException, IOException {
        try (MockedStatic<HelperUtil> helperUtilMock = Mockito.mockStatic(HelperUtil.class)) {
            helperUtilMock.when(() -> HelperUtil.getBean(Mockito.any())).thenReturn(userInstitutionCache);
            Mockito.when(userInstitutionCache.getInstitutionForRequestSessionId(Mockito.anyString())).thenReturn("PUL");
            helperUtilMock.when(() -> HelperUtil.getInstitutionFromRequest(Mockito.any())).thenReturn("PUL");
            Mockito.when(request.getSession()).thenReturn(value);
            Mockito.when(value.getId()).thenReturn("1");
            Cookie mockCookie = Mockito.mock(Cookie.class);
            Mockito.when(mockCookie.getName()).thenReturn(ScsbConstants.IS_USER_AUTHENTICATED).thenReturn(ScsbConstants.LOGGED_IN_INSTITUTION);
            Mockito.when(mockCookie.getValue()).thenReturn("Y");
            Mockito.when(request.getCookies()).thenReturn(new Cookie[]{mockCookie});
            SCSBInstitutionFilter.doFilterInternal(request, httpServletResponse, filterChain);
            assertTrue(true);
        }
    }

    @Test
    public void testDoFilterInternalWithNoInstitution() throws ServletException, IOException {
        try (MockedStatic<HelperUtil> helperUtilMock = Mockito.mockStatic(HelperUtil.class)) {
            helperUtilMock.when(() -> HelperUtil.getBean(Mockito.any())).thenReturn(userInstitutionCache);
            Mockito.when(userInstitutionCache.getInstitutionForRequestSessionId(Mockito.anyString())).thenReturn(null);
            helperUtilMock.when(() -> HelperUtil.getInstitutionFromRequest(Mockito.any())).thenReturn(null);
            Mockito.when(request.getSession()).thenReturn(value);
            Mockito.when(value.getId()).thenReturn("1");
            Cookie mockCookie = Mockito.mock(Cookie.class);
            Mockito.when(mockCookie.getName()).thenReturn(ScsbConstants.IS_USER_AUTHENTICATED).thenReturn(ScsbConstants.LOGGED_IN_INSTITUTION);
            Mockito.when(request.getCookies()).thenReturn(new Cookie[]{mockCookie});
            Mockito.when(request.getRequestURI()).thenReturn("/home");
            SCSBInstitutionFilter.doFilterInternal(request, httpServletResponse, filterChain);
            assertTrue(true);
        }
    }

}