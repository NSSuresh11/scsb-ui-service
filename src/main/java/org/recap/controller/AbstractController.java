package org.recap.controller;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.repository.jpa.UserDetailsRepository;
import org.recap.service.RestHeaderService;
import org.recap.util.RequestServiceUtil;
import org.recap.util.UserAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class AbstractController {

    @Value("${" + PropertyKeyConstants.SCSB_GATEWAY_URL + "}")
    private String scsbUrl;

    @Value("${" + PropertyKeyConstants.SCSB_AUTH_URL + "}")
    private String scsbShiro;

    @Autowired
    private RestHeaderService restHeaderService;

    @Autowired
    private RequestServiceUtil requestServiceUtil;

    @Autowired
    private UserAuthUtil userAuthUtil;

    /**
     * Get rest template .
     *
     * @return the rest template
     */
    @Nonnull
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }

    /**
     * Gets scsb shiro.
     *
     * @return the scsb shiro
     */
    public String getScsbShiro() {
        return scsbShiro;
    }

    /**
     * Gets scsb url.
     *
     * @return the scsb url
     */
    public String getScsbUrl() {
        return scsbUrl;
    }

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    protected void setValuesInSession(HttpSession session, Map<String, Object> authMap) {
        session.setAttribute(ScsbConstants.USER_NAME, authMap.get(ScsbConstants.USER_NAME));
        session.setAttribute(ScsbConstants.USER_DESC, userDetailsRepository.findByLoginId(authMap.get(ScsbConstants.USER_NAME).toString()).getUserDescription());
        session.setAttribute(ScsbConstants.USER_ID, authMap.get(ScsbConstants.USER_ID));
        session.setAttribute(ScsbConstants.USER_INSTITUTION, authMap.get(ScsbConstants.USER_INSTITUTION));
        session.setAttribute(ScsbConstants.SUPER_ADMIN_USER, authMap.get(ScsbConstants.SUPER_ADMIN_USER));
        session.setAttribute(ScsbConstants.USER_ADMINISTRATOR, authMap.get(ScsbConstants.USER_ADMINISTRATOR));
        session.setAttribute(ScsbConstants.REPOSITORY, authMap.get(ScsbConstants.REPOSITORY));
        session.setAttribute(ScsbConstants.REQUEST_PRIVILEGE, authMap.get(ScsbConstants.REQUEST_PRIVILEGE));
        session.setAttribute(ScsbConstants.COLLECTION_PRIVILEGE, authMap.get(ScsbConstants.COLLECTION_PRIVILEGE));
        session.setAttribute(ScsbConstants.REPORTS_PRIVILEGE, authMap.get(ScsbConstants.REPORTS_PRIVILEGE));
        session.setAttribute(ScsbConstants.SEARCH_PRIVILEGE, authMap.get(ScsbConstants.SEARCH_PRIVILEGE));
        session.setAttribute(ScsbConstants.USER_ROLE_PRIVILEGE, authMap.get(ScsbConstants.USER_ROLE_PRIVILEGE));
        session.setAttribute(ScsbConstants.REQUEST_ALL_PRIVILEGE, authMap.get(ScsbConstants.REQUEST_ALL_PRIVILEGE));
        session.setAttribute(ScsbConstants.REQUEST_ITEM_PRIVILEGE, authMap.get(ScsbConstants.REQUEST_ITEM_PRIVILEGE));
        session.setAttribute(ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE, authMap.get(ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE));
        session.setAttribute(ScsbConstants.DEACCESSION_PRIVILEGE, authMap.get(ScsbConstants.DEACCESSION_PRIVILEGE));
        session.setAttribute(ScsbCommonConstants.BULK_REQUEST_PRIVILEGE, authMap.get(ScsbCommonConstants.BULK_REQUEST_PRIVILEGE));
        session.setAttribute(ScsbCommonConstants.RESUBMIT_REQUEST_PRIVILEGE, authMap.get(ScsbCommonConstants.RESUBMIT_REQUEST_PRIVILEGE));
        session.setAttribute(ScsbConstants.MONITORING, authMap.get(ScsbConstants.MONITORING));
        session.setAttribute(ScsbConstants.LOGGING, authMap.get(ScsbConstants.LOGGING));
        session.setAttribute(ScsbConstants.REQUESTLOG, authMap.get(ScsbConstants.REQUESTLOG));
        session.setAttribute(ScsbConstants.DATA_EXPORT, authMap.get(ScsbConstants.DATA_EXPORT));
        Object isSuperAdmin = session.getAttribute(ScsbConstants.SUPER_ADMIN_USER);
        if (isSuperAdmin != null && (boolean) isSuperAdmin) {
            session.setAttribute(ScsbConstants.ROLE_FOR_SUPER_ADMIN, true);
        } else {
            session.setAttribute(ScsbConstants.ROLE_FOR_SUPER_ADMIN, false);
        }
    }

}
