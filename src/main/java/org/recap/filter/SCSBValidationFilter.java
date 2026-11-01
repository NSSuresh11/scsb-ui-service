package org.recap.filter;

import org.apache.http.HttpStatus;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.spring.ApplicationContextProvider;
import org.recap.spring.PropertyValueProvider;
import org.recap.util.HelperUtil;
import org.recap.util.UserAuthUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;


public class SCSBValidationFilter implements Filter {


    UserAuthUtil userAuthUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        var user_authenticated = ScsbConstants.FALSE_STRING;
        try {
            PropertyValueProvider propertyValueProvider = HelperUtil.getBean(PropertyValueProvider.class);
            if (propertyValueProvider.getProperty(ScsbConstants.CSP_ENABLE).equals(Boolean.TRUE)) {
                httpServletResponse.addHeader(ScsbConstants.CSP,
                        "default-src " + propertyValueProvider.getProperty(PropertyKeyConstants.SCSB_UI_URL) + " "
                                + propertyValueProvider.getProperty(ScsbConstants.CSP_VALUE) + "; frame-ancestors " + propertyValueProvider.getProperty(ScsbConstants.FRAME_ANCESTOR_VALUE));
            }
            HttpSession session = httpServletRequest.getSession(ScsbConstants.FALSE);
            Optional<String> API_PATH = Optional.ofNullable(httpServletRequest.getHeader(ScsbConstants.API_PATH));
            API_PATH = (API_PATH.isEmpty()) ? Optional.ofNullable(ScsbConstants.SEARCH.toLowerCase()) : API_PATH;
            if(API_PATH.isPresent()) {
                boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.AUTH_PATH + API_PATH.get());
                user_authenticated = (authenticated) ? ScsbConstants.TRUE_STRING : ScsbConstants.FALSE_STRING;
                httpServletResponse.setHeader(ScsbConstants.USER_AUTHENTICATED, user_authenticated);
            }
        } catch (Exception e) {
            httpServletResponse.setHeader(ScsbConstants.USER_AUTHENTICATED, user_authenticated);
        }
        if (user_authenticated.equalsIgnoreCase(ScsbConstants.TRUE_STRING))
            chain.doFilter(request, response);
        else
            httpServletResponse.setStatus(HttpStatus.SC_OK);
    }

    public UserAuthUtil getUserAuthUtil() {
        if (userAuthUtil == null) {
            userAuthUtil = ApplicationContextProvider.getInstance().getApplicationContext().getBean(UserAuthUtil.class);
        }
        return userAuthUtil;
    }
}
