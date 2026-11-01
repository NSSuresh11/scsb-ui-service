package org.recap.security;

import org.apache.commons.lang3.StringUtils;
import org.recap.ScsbConstants;
import org.recap.util.HelperUtil;
import org.recap.util.UserAuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by sheiks on 20/01/17.
 */
public class SCSBSimpleUrlLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {


    private UserAuthUtil userAuthUtil;

    /**
     * Instantiates a new ReCAPSimpleUrlLogoutSuccessHandler.
     *
     * @param userAuthUtil the user auth util
     */
    public SCSBSimpleUrlLogoutSuccessHandler(UserAuthUtil userAuthUtil) {
        this.userAuthUtil = userAuthUtil;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object attribute = request.getAttribute(ScsbConstants.USER_TOKEN);
        super.onLogoutSuccess(request, response, authentication);
        HelperUtil.logoutFromShiro(attribute);
        request.removeAttribute(ScsbConstants.USER_TOKEN);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String logoutUrl = null;
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        String institution = null;
        if(requestAttributes != null) {
            institution = (String) ((ServletRequestAttributes) requestAttributes).getRequest().getAttribute(ScsbConstants.SCSB_INSTITUTION_CODE);
        }
        if (StringUtils.isNotBlank(institution)) {
            logoutUrl = HelperUtil.getLogoutUrl(institution);
        }
        return logoutUrl;
    }
}
