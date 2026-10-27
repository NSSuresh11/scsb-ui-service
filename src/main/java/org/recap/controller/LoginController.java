package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.repository.jpa.UserDetailsRepository;
import org.recap.security.UserInstitutionCache;
import org.recap.util.HelperUtil;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;


/**
 * Created by dinakar on 23/12/20.
 */
@Slf4j
@Controller
public class LoginController extends AbstractController {


    @Autowired
    private UserInstitutionCache userInstitutionCache;

    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    /**
     * Return either login or search view. Returns search view if user authenticated. If not it will return login view.
     *
     * @param request the request
     * @return the string
     */
    @GetMapping(value = "/")
    public String loginScreen(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (null != auth && !HelperUtil.isAnonymousUser(auth)) {
            return ScsbConstants.REDIRECT_SEARCH;
        }
        return ScsbConstants.FORWARD_INDEX;
    }

    /**
     * Return home view.
     *
     * @param request the request
     * @return the string
     */
    @GetMapping(value = "/home")
    public String home(HttpServletRequest request) {
        return ScsbConstants.FORWARD_INDEX;
    }

    /**
     * Perform the SCSB authentication and authorization after user authenticated from partners IMS
     *
     * @param request the request
     * @return the view name
     */
    @GetMapping(value = "/login-scsb")
    public String login(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = processSessionFixation(request);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            String institutionFromRequest = request.getParameter("institution");
            String authType = propertyUtil.getPropertyByInstitutionAndKey(institutionFromRequest,  PropertyKeyConstants.ILS.ILS_AUTH_TYPE);
            UsernamePasswordToken token = new UsernamePasswordToken(username + ScsbConstants.TOKEN_SPLITER + institutionFromRequest, "", true);
            Map<String, Object> resultMap = getUserAuthUtil().doAuthentication(token);
            if(userHasRoles(resultMap)) {
                if (!(Boolean) resultMap.get(ScsbConstants.IS_USER_AUTHENTICATED)) {
                    String errorMessage = (String) resultMap.get(ScsbConstants.USER_AUTH_ERRORMSG);
                    log.error("User: {}, {} {}", token.getUsername(), ScsbCommonConstants.LOG_ERROR, errorMessage);
                    return ScsbConstants.REDIRECT_USER;
                }
            } else {
                return ScsbConstants.REDIRECT_USER;
            }
            setSessionValues(session, resultMap, token);

        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            log.error("Exception occurred in authentication : {}" , exception.getLocalizedMessage());
            return ScsbConstants.REDIRECT_HOME;
        }
        return ScsbConstants.REDIRECT_SEARCH;
    }

    private boolean userHasRoles(Map<String, Object> resultMap) {
        return (Boolean) resultMap.get(ScsbConstants.SEARCH_PRIVILEGE);
    }

    private HttpSession processSessionFixation(HttpServletRequest request) {

        String requestedSessionId = request.getSession().getId();

        String institutionCode = userInstitutionCache.getInstitutionForRequestSessionId(requestedSessionId);

        userInstitutionCache.removeSessionId(requestedSessionId);

        request.getSession(false).invalidate();
        HttpSession session = request.getSession(true);

        userInstitutionCache.addRequestSessionId(session.getId(), institutionCode);

        return session;
    }


    /**
     *
     * @param request HttpServletRequest
     * @return return redirect URL
     */
    @GetMapping("/logout")
    public String logoutUser(HttpServletRequest request) {
        log.info("Subject Logged out");
        HttpSession session = null;
        try {
            session = request.getSession(false);
            if (session != null) {
                boolean isSamlSession = Boolean.TRUE.equals(
                        session.getAttribute(ScsbConstants.SAML_AUTHENTICATED));

                if (isSamlSession) {
                    log.info("SAML session logout");
                    SecurityContextHolder.clearContext();
                } else {
                    getUserAuthUtil().authorizedUser(ScsbConstants.SCSB_SHIRO_LOGOUT_URL,
                            (UsernamePasswordToken) session.getAttribute(ScsbConstants.USER_TOKEN));
                }
            }
        } finally {
            if (session != null) {
                session.invalidate();
            }
        }
        return ScsbConstants.REDIRECT_HOME;
    }

    private void setSessionValues(HttpSession session, Map<String, Object> resultMap, UsernamePasswordToken token) {
        session.setAttribute(ScsbConstants.USER_TOKEN, token);
        session.setAttribute(ScsbConstants.USER_AUTH, resultMap);
        setValuesInSession(session, resultMap);
    }
}
