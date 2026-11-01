package org.recap.controller.saml;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.recap.ScsbConstants;
import org.recap.controller.AbstractController;
import org.recap.model.jpa.UsersEntity;
import org.recap.model.saml.SamlConfig;
import org.recap.repository.jpa.UserDetailsRepository;
import org.recap.security.UserInstitutionCache;
import org.recap.security.saml.SamlAuthHandler;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Controller
public class SamlCallbackController extends AbstractController {

    @Autowired
    private SamlAuthHandler samlAuthHandler;

    @Autowired
    private UserInstitutionCache  userInstitutionCache;

    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
     private UserDetailsRepository userDetailsRepository;


    @GetMapping("/auth/saml")
    public String initiateSaml(@RequestParam("institution") String institution,
                               HttpServletRequest request) {

        log.info("SAML: Initiating SSO for institution '{}'", institution);

        SamlConfig config = buildSamlConfig(institution);

     /*   if (!config.isActive()) {
            log.warn("SAML [{}]: SAML is disabled (saml.active=false)", institution);
            return "redirect:/?error=saml_disabled";
        }*/
        if (StringUtils.isBlank(config.getIdpSsoUrl())) {
            log.error("SAML [{}]: idp.sso.url not configured", institution);
            return "redirect:/?error=saml_config_missing";
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(ScsbConstants.SAML_INSTITUTION_CODE, institution);
        userInstitutionCache.addRequestSessionId(session.getId(), institution);

        String callbackUrl = buildAcsUrl(request, config);

        // Pass institution in RelayState ? ACS fallback if session is lost
        // across nodes (no sticky sessions / load balancer)
        String relayState  = URLEncoder.encode("inst=" + institution, StandardCharsets.UTF_8);
        String redirectUrl = samlAuthHandler.buildAuthnRequestUrl(config, callbackUrl, relayState);

        log.info("SAML [{}]: Redirecting to IdP SSO: {}", institution, config.getIdpSsoUrl());
        return "redirect:" + redirectUrl;
    }

    //  IdP POSTs SAMLResponse to /saml/acs

        @PostMapping({"/saml/acs", "/saml/acs/{institutionCode}"})
        public String samlAcs(@PathVariable(value = "institutionCode", required = false) String institutionCodeFromPath,
                @RequestParam("SAMLResponse") String samlResponse,
                @RequestParam(value = "RelayState", required = false) String relayState,
                HttpServletRequest request) {
        log.info("SAML ACS: Received SAMLResponse POST (relayState='{}')", relayState);
            // Resolve institution: path variable first (if present), then session, then RelayState
            String institutionCode = StringUtils.isNotBlank(institutionCodeFromPath)
                    ? institutionCodeFromPath.toUpperCase()
                    : resolveInstitution(request, relayState);

        if (StringUtils.isBlank(institutionCode)) {
            log.error("SAML ACS: Cannot determine institution ? session expired and no RelayState");
            return "redirect:/?error=saml_session_expired";
        }
        log.info("SAML ACS: institution='{}'", institutionCode);

        //  Load IdP config from DB
        SamlConfig config = buildSamlConfig(institutionCode);
        if (StringUtils.isBlank(config.getIdpSsoUrl())) {
            log.error("SAML ACS [{}]: idp.sso.url not configured ? check scsb_properties_t", institutionCode);
            return "redirect:/?error=saml_config_missing";
        }
        if (StringUtils.isBlank(config.getIdpCertificate())) {
            log.error("SAML ACS [{}]: idp.certificate not configured ? check scsb_properties_t", institutionCode);
            return "redirect:/?error=saml_config_missing";
        }
        log.info("SAML ACS [{}]: Config loaded OK", institutionCode);

        //  Validate SAMLResponse (status + signature + NameID)
        SamlAuthHandler.SamlUserInfo userInfo =
                samlAuthHandler.processSamlResponse(samlResponse, config);

        if (userInfo == null) {
            log.error("SAML ACS [{}]: processSamlResponse returned null ? see preceding logs", institutionCode);
            return "redirect:/home?error=saml_auth_failed";
        }
        log.info("SAML ACS [{}]: SAMLResponse valid, userId='{}'", institutionCode, userInfo.userId);

        // Look up user in USER_T: try full NameID first, then the short ID (strip domain)
        UsersEntity usersEntity = userDetailsRepository.findByLoginId(userInfo.userId);
        if (usersEntity == null && userInfo.userId != null && userInfo.userId.contains("@")) {
            String shortId = userInfo.userId.substring(0, userInfo.userId.indexOf('@'));
            usersEntity = userDetailsRepository.findByLoginId(shortId);
            if (usersEntity != null) {
                log.info("SAML ACS [{}]: Matched '{}' to USER_T.LOGIN_ID '{}'",
                        institutionCode, userInfo.userId, shortId);
            }
        }
        if (usersEntity == null) {
            log.error("SAML ACS [{}]: User '{}' not found in USER_T - provision the account first",
                    institutionCode, userInfo.userId);
            return "redirect:/home?error=saml_user_not_found";
        }
        final String loginId = usersEntity.getLoginId();   // ID exactly as stored in USER_T
        log.info("SAML ACS [{}]: User '{}' found in USER_T", institutionCode, loginId);

        // Session fixation protection
        HttpSession session = processSessionFixation(request, institutionCode);
        session.setAttribute(ScsbConstants.SAML_AUTHENTICATED, Boolean.TRUE);

        // Set Spring Security context
        UsernamePasswordAuthenticationToken springAuth =
                new UsernamePasswordAuthenticationToken(
                        loginId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(springAuth);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        //  Shiro authentication for privilege loading
        UsernamePasswordToken shiroToken = new UsernamePasswordToken(
                loginId + ScsbConstants.TOKEN_SPLITER + institutionCode,
                "",
                true);

        Map<String, Object> resultMap;
        try {
            resultMap = getUserAuthUtil().doAuthentication(shiroToken);
        } catch (Exception e) {
            log.error("SAML ACS [{}]: Shiro doAuthentication threw exception for user '{}': {}",
                    institutionCode, loginId, e.getMessage(), e);
            return "redirect:/home?error=saml_auth_failed";
        }

        if (resultMap == null) {
            log.error("SAML ACS [{}]: Shiro returned null resultMap for user '{}'", institutionCode, loginId);
            return "redirect:/home?error=saml_auth_failed";
        }

        Object isAuthenticated = resultMap.get(ScsbConstants.IS_USER_AUTHENTICATED);
        if (Boolean.FALSE.equals(isAuthenticated)) {
            String errMsg = (String) resultMap.get(ScsbConstants.USER_AUTH_ERRORMSG);
            log.error("SAML ACS [{}]: Shiro auth denied for user '{}': {}", institutionCode, loginId, errMsg);
            return "redirect:/home?error=saml_auth_failed";
        }

        //  Populate session
        session.setAttribute(ScsbConstants.USER_TOKEN, shiroToken);
        session.setAttribute(ScsbConstants.USER_AUTH, resultMap);
        setValuesInSession(session, resultMap);
        session.setAttribute(ScsbConstants.IS_USER_AUTHENTICATED, true);
        session.setAttribute(ScsbConstants.LOGGED_IN_INSTITUTION, institutionCode);

        log.info("SAML ACS [{}]: Login complete for user '{}'. Redirecting to /search.",
                institutionCode, loginId);
        return "redirect:/search";
    }

    // ??? SP metadata endpoints ????????????????????????????????????????????????

    @GetMapping(value = "/saml/metadata/{institutionCode}", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String spMetadataWithCode(@PathVariable("institutionCode") String institutionCode) {
        log.info("SAML metadata: request for institution '{}'", institutionCode);
        return generateMetadata(institutionCode);
    }

    @GetMapping(value = "/saml/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String spMetadataFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String institutionCode = (session != null)
                ? (String) session.getAttribute(ScsbConstants.SAML_INSTITUTION_CODE)
                : null;

        if (StringUtils.isBlank(institutionCode)) {
            log.warn("SAML metadata: no institution code in session ? use /saml/metadata/{institutionCode}");
            return "<!-- ERROR: institution code required. Use /saml/metadata/{institutionCode} -->";
        }
        return generateMetadata(institutionCode);
    }

    //  Private helpers

    /**
     * Resolve institution from session first; fall back to RelayState
     * (inst=<code> URL-encoded) for load-balanced deployments without sticky sessions.
     */
    private String resolveInstitution(HttpServletRequest request, String relayState) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String fromSession = (String) session.getAttribute(ScsbConstants.SAML_INSTITUTION_CODE);
            if (StringUtils.isNotBlank(fromSession)) {
                log.debug("SAML ACS: institution '{}' from session", fromSession);
                return fromSession;
            }
        }

        if (StringUtils.isNotBlank(relayState)) {
            try {
                String decoded = URLDecoder.decode(relayState, StandardCharsets.UTF_8);
                if (decoded.startsWith("inst=")) {
                    String fromRelay = decoded.substring(5);
                    log.info("SAML ACS: institution '{}' recovered from RelayState", fromRelay);
                    // Restore to new session
                    HttpSession newSession = request.getSession(true);
                    newSession.setAttribute(ScsbConstants.SAML_INSTITUTION_CODE, fromRelay);
                    userInstitutionCache.addRequestSessionId(newSession.getId(), fromRelay);
                    return fromRelay;
                }
            } catch (Exception e) {
                log.warn("SAML ACS: Failed to parse RelayState '{}'", relayState, e);
            }
        }
        return null;
    }

    private SamlConfig buildSamlConfig(String institutionCode) {
        return SamlConfig.builder()
                .institutionCode(institutionCode)
                .idpSsoUrl(getProperty(institutionCode, ScsbConstants.IDP_SSO_URL))
                .spEntityId(getProperty(institutionCode, ScsbConstants.SP_ENTITY_ID))
                .acsUrl(getProperty(institutionCode, ScsbConstants.ACS_URL))
                .idpCertificate(getProperty(institutionCode, ScsbConstants.IDP_CERTIFICATE))
                .spCertificate(getProperty(institutionCode, ScsbConstants.SP_CERTIFICATE))
                .spPrivateKey(getProperty(institutionCode, ScsbConstants.SP_PRIVATE_KEY))
//                .active(!"false".equalsIgnoreCase(getProperty(institutionCode, ScsbConstants.SAML_ACTIVE)))
                .build();
    }

    private String getProperty(String institutionCode, String key) {
        try {
            return propertyUtil.getPropertyByInstitutionAndKey(institutionCode, key);
        } catch (Exception e) {
            log.warn("SAML [{}]: Property '{}' not found in scsb_properties_t", institutionCode, key);
            return null;
        }
    }

    private String generateMetadata(String institutionCode) {
        SamlConfig config = buildSamlConfig(institutionCode);
        if (StringUtils.isBlank(config.getIdpSsoUrl())) {
            log.warn("SAML metadata: idp.sso.url not configured for '{}'", institutionCode);
            return "<!-- No SAML configuration found for institution: " + institutionCode + " -->";
        }
        return samlAuthHandler.generateSpMetadata(config);
    }

    private HttpSession processSessionFixation(HttpServletRequest request, String institutionCode) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            String cachedInst = userInstitutionCache.getInstitutionForRequestSessionId(old.getId());
            String effective  = StringUtils.isNotBlank(cachedInst) ? cachedInst : institutionCode;
            userInstitutionCache.removeSessionId(old.getId());
            old.invalidate();
            HttpSession fresh = request.getSession(true);
            userInstitutionCache.addRequestSessionId(fresh.getId(), effective);
            return fresh;
        }
        HttpSession fresh = request.getSession(true);
        userInstitutionCache.addRequestSessionId(fresh.getId(), institutionCode);
        return fresh;
    }

    private String buildAcsUrl(HttpServletRequest request, SamlConfig config) {
        if (config.getAcsUrl() != null && !config.getAcsUrl().isBlank()) {
            return config.getAcsUrl();
        }
        String scheme = request.getScheme();
        String host   = request.getServerName();
        int    port   = request.getServerPort();
        StringBuilder base = new StringBuilder(scheme).append("://").append(host);
        if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
            base.append(":").append(port);
        }
        return base.append("/saml/acs").toString();
    }
}