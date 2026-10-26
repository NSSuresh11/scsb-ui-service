package org.recap.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.spring.ApplicationContextProvider;
import org.recap.spring.PropertyValueProvider;
import org.recap.spring.SwaggerAPIProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by sheiks on 20/01/17.
 */
@Slf4j
public class HelperUtil {


    /**
     * Gets attribute value from request.
     *
     * @param request the request
     * @param key     the key
     * @return the attribute value of the given key
     */

    public static String getAttributeValueFromRequest(HttpServletRequest request, String key) {
        return (String) request.getAttribute(key);
    }

    /**
     * Gets bean for the given type.
     *
     * @param <T>          the type parameter
     * @param requiredType the required type
     * @return the bean
     */
    public static <T> T getBean(Class<T> requiredType) {
        ApplicationContext applicationContext = ApplicationContextProvider.getInstance().getApplicationContext();
        return applicationContext.getBean(requiredType);
    }

    /**
     * Gets institution from request.
     *
     * @param request the request
     * @return the institution
     */
    public static String getInstitutionFromRequest(HttpServletRequest request) {
        String institution = request.getParameter("institution");
        if (StringUtils.isBlank(institution)) {
            institution = (String) request.getAttribute(ScsbConstants.SCSB_INSTITUTION_CODE);
        }
        return institution;
    }

    /**
     * Gets logout url based on the loggined in institution.
     *
     * @param institutionCode the institution code
     * @return the logout url
     */
    public static String getLogoutUrl(String institutionCode) {
        String casLogoutUrl;
        PropertyValueProvider propertyValueProvider = HelperUtil.getBean(PropertyValueProvider.class);
        String authType = HelperUtil.getBean(PropertyUtil.class).getPropertyByInstitutionAndKey(institutionCode,  PropertyKeyConstants.ILS.ILS_AUTH_TYPE);
        if (StringUtils.equals(authType, ScsbConstants.AUTH_TYPE_SAML)) {
            casLogoutUrl = propertyValueProvider.getProperty(PropertyKeyConstants.SCSB_UI_URL) + "home";
        } else {
            String url = HelperUtil.getBean(PropertyUtil.class).getPropertyByInstitutionAndKey(institutionCode, PropertyKeyConstants.ILS.ILS_AUTH_SERVICE_LOGOUT);
            String redirectUri = propertyValueProvider.getProperty(PropertyKeyConstants.SCSB_APP_LOGOUT_REDIRECT_URI);
            casLogoutUrl = url + "" + redirectUri;
        }
        return casLogoutUrl;
    }

    /**
     * Logout from shiro.
     *
     * @param attribute the attribute
     */
    public static void logoutFromShiro(Object attribute) {
        if (null != attribute) {
            try {
                UserAuthUtil userAuthUtil = HelperUtil.getBean(UserAuthUtil.class);
                userAuthUtil.authorizedUser(ScsbConstants.SCSB_SHIRO_LOGOUT_URL, (UsernamePasswordToken) attribute);
            } catch (Exception e) {
                log.error("Logout", e);
            }
        }
    }

    /**
     * find ss anonymous user logged in.
     *
     * @param auth the auth
     * @return the boolean
     */
    public static boolean isAnonymousUser(Authentication auth) {
        if (StringUtils.equals(auth.getName(), ScsbConstants.ANONYMOUS_USER)) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            for (Iterator<? extends GrantedAuthority> iterator = authorities.iterator(); iterator.hasNext(); ) {
                GrantedAuthority grantedAuthority = iterator.next();
                if (StringUtils.equals(grantedAuthority.getAuthority(), ScsbConstants.ROLE_ANONYMOUS)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static HttpHeaders getSwaggerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ScsbCommonConstants.API_KEY, SwaggerAPIProvider.getInstance().getSwaggerApiKey());
        return headers;
    }

    public static byte[] getFileContent(File csvFile, String fileNameWithExtension, String templateName) throws IOException {
        return IOUtils.toByteArray(new FileInputStream(csvFile));
    }

    public static void setCookieProperties(Cookie cookie) {
        cookie.setMaxAge(-1);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
    }
}

