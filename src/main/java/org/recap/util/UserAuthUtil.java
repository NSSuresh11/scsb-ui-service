package org.recap.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.service.RestHeaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dharmendrag on 4/1/17.
 */
@Slf4j
@Service
public class UserAuthUtil {


    @Autowired
    RestHeaderService restHeaderService;
    @Value("${" + PropertyKeyConstants.SCSB_AUTH_URL + "}")
    private String scsbShiro;

    public RestHeaderService getRestHeaderService() {
        return restHeaderService;
    }

    /**
     * Authenticate the used based on the UsernamePasswordToken.
     *
     * @param token the token
     * @return the map of the authorization values
     * @throws Exception the exception
     */
    public Map<String, Object> doAuthentication(UsernamePasswordToken token) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<UsernamePasswordToken> requestEntity = new HttpEntity<>(token, getRestHeaderService().getHttpHeaders());
        return restTemplate.postForObject(scsbShiro + ScsbConstants.SCSB_SHIRO_AUTHENTICATE_URL, requestEntity, HashMap.class);
    }

    /**
     * Authorize the login user.
     *
     * @param serviceURL the service url
     * @param token      the token
     * @return the boolean
     */
    public boolean authorizedUser(String serviceURL, UsernamePasswordToken token) {

        boolean statusResponse = false;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<UsernamePasswordToken> requestEntity = new HttpEntity<>(token, getRestHeaderService().getHttpHeaders());
            statusResponse = restTemplate.postForObject(scsbShiro + serviceURL, requestEntity, Boolean.class);
        } catch (Exception e) {
            if (serviceURL.contains(ScsbConstants.LOGOUT))
                log.info(ScsbConstants.LOG_USER_LOGOUT_SUCCESS + " :: {}", token != null ? token.getUsername() : null);
        }
        return statusResponse;
    }

    /**
     * Gets logged-in user details.
     *
     * @param recapPermission the recap permission
     * @return the user details
     */
    public UserDetailsForm getUserDetails(HttpSession session, String recapPermission) {
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setSuperAdmin((Boolean) session.getAttribute(ScsbConstants.SUPER_ADMIN_USER));
        userDetailsForm.setRepositoryUser((Boolean) session.getAttribute(ScsbConstants.REPOSITORY));
        userDetailsForm.setLoginInstitutionId((Integer) session.getAttribute(ScsbConstants.USER_INSTITUTION));
        userDetailsForm.setRecapPermissionAllowed((Boolean) session.getAttribute(recapPermission));
        userDetailsForm.setUserAdministrator((Boolean) session.getAttribute(ScsbConstants.USER_ADMINISTRATOR));
        return userDetailsForm;
    }

    /**
     * Checks whether the user session is authenticated to support the request.
     *
     * @param httpSession The current session.
     * @param roleUrl     The role url.
     * @return <code>true</code> if the user is successfully authenticated and
     * <code>false</code> if not
     */
    public boolean isAuthenticated(HttpSession httpSession, String roleUrl) {
        if (Boolean.TRUE.equals(httpSession.getAttribute(ScsbConstants.SAML_AUTHENTICATED))) {
            return isSamlSessionAuthorized(httpSession, roleUrl);
        }
        return this.authorizedUser(roleUrl, (UsernamePasswordToken) httpSession.getAttribute(ScsbConstants.USER_TOKEN));
    }

    /**
     * Checks whether the request has a valid session to serve it..
     *
     * @param httpServletRequest The current request.
     * @param roleUrl            The role url.
     * @return <code>true</code> if the user is successfully authenticated and
     * <code>false</code> if not
     */
    public boolean isAuthenticated(HttpServletRequest httpServletRequest, String roleUrl) {
        HttpSession httpSession = httpServletRequest.getSession(false);
        return this.isAuthenticated(httpSession, roleUrl);
    }


    private boolean isSamlSessionAuthorized(HttpSession session, String roleUrl) {
        if (roleUrl == null) return false;
        if (roleUrl.contains("search"))      return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.SEARCH_PRIVILEGE));
        if (roleUrl.contains("request"))     return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.REQUEST_PRIVILEGE));
        if (roleUrl.contains("collection"))  return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.COLLECTION_PRIVILEGE));
        if (roleUrl.contains("reports"))     return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.REPORTS_PRIVILEGE));
        if (roleUrl.contains("userRoles"))   return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.USER_ROLE_PRIVILEGE));
        if (roleUrl.contains("bulkRequest")) return Boolean.TRUE.equals(session.getAttribute(ScsbCommonConstants.BULK_REQUEST_PRIVILEGE));
        if (roleUrl.contains("monitoring"))  return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.MONITORING));
        if (roleUrl.contains("dataExport"))  return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.DATA_EXPORT));
        if (roleUrl.contains("roles"))       return Boolean.TRUE.equals(session.getAttribute(ScsbConstants.SUPER_ADMIN_USER));
        return false;
    }

}
