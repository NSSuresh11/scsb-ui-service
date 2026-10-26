package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.security.UserInstitutionCache;
import org.recap.security.UserManagementService;
import org.recap.util.PropertyUtil;
import org.recap.util.ReportsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class HomeController extends AbstractController {


    @Autowired
    private ReportsUtil reportsUtil;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private UserInstitutionCache userInstitutionCache;

    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
    private  UserManagementService userManagementService;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    /**
     * @return InstitutionsList
     */
    @GetMapping("/institutions")
    public Map<String, String> loadInstitutions() {
        Map<String, String> instList = new LinkedHashMap<>();
        List<InstitutionEntity> InstitutionCodes = fecthingInstituionsFromDB();
        for (InstitutionEntity institutionEntity : InstitutionCodes) {
            instList.put(institutionEntity.getInstitutionCode(), institutionEntity.getInstitutionName());
        }
        instList.put(supportInstitution, supportInstitution);
        return instList;
    }

    public List<InstitutionEntity> fecthingInstituionsFromDB() {
        List<InstitutionEntity> InstitutionCodes = null;
        try {
            InstitutionCodes = institutionDetailsRepository.getInstitutionCodes(supportInstitution);
        } catch (Exception e) {
            log.info("Exception occured while pulling institutions from DB :: {}", e.getMessage());
        }
        return InstitutionCodes;
    }

    /**
     *
     * @param request
     * @return permission list of user
     */
    @GetMapping(value = "/loginCheck")
    public Map<String, Object> login(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Map<String, Object> resultMap = new HashMap<>();
        boolean isAuthenticated = false;
        try {
            isAuthenticated = getUserAuthUtil().isAuthenticated(request, ScsbConstants.SCSB_SHIRO_SEARCH_URL);
            resultMap.put(ScsbConstants.USER_NAME, request.getSession().getAttribute(ScsbConstants.USER_NAME));
            resultMap.put(ScsbConstants.USER_DESC, request.getSession().getAttribute(ScsbConstants.USER_DESC));
            resultMap.put(ScsbConstants.REQUEST_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.REQUEST_PRIVILEGE));
            resultMap.put(ScsbConstants.SEARCH_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.SEARCH_PRIVILEGE));
            resultMap.put(ScsbConstants.COLLECTION_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.COLLECTION_PRIVILEGE));
            resultMap.put(ScsbConstants.DEACCESSION_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.DEACCESSION_PRIVILEGE));
            resultMap.put(ScsbConstants.REQUEST_ALL_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.REQUEST_ALL_PRIVILEGE));
            resultMap.put(ScsbCommonConstants.BULK_REQUEST_PRIVILEGE, request.getSession().getAttribute(ScsbCommonConstants.BULK_REQUEST_PRIVILEGE));
            resultMap.put(ScsbConstants.REPORTS_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.REPORTS_PRIVILEGE));
            resultMap.put(ScsbConstants.ROLE_FOR_SUPER_ADMIN, request.getSession().getAttribute(ScsbConstants.ROLE_FOR_SUPER_ADMIN));
            resultMap.put(ScsbConstants.USER_ROLE_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.USER_ROLE_PRIVILEGE));
            resultMap.put(ScsbConstants.SUPER_ADMIN_USER, request.getSession().getAttribute(ScsbConstants.SUPER_ADMIN_USER));
            resultMap.put(ScsbConstants.MONITORING, request.getSession().getAttribute(ScsbConstants.MONITORING));
            resultMap.put(ScsbConstants.LOGGING, request.getSession().getAttribute(ScsbConstants.LOGGING));
            resultMap.put(ScsbConstants.DATA_EXPORT, request.getSession().getAttribute(ScsbConstants.DATA_EXPORT));
            resultMap.put(ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE, request.getSession().getAttribute(ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE));
            resultMap.put(ScsbConstants.RESUBMIT_REQUEST_PRIVILEGE,request.getSession().getAttribute(ScsbConstants.RESUBMIT_REQUEST_PRIVILEGE));
            resultMap.put(ScsbConstants.REQUESTLOG, request.getSession().getAttribute(ScsbConstants.REQUESTLOG));
            resultMap.put(ScsbConstants.REPOSITORY, request.getSession().getAttribute(ScsbConstants.REPOSITORY));
            resultMap.put(ScsbConstants.IS_AUTHENTICATED, isAuthenticated);
        } catch (Exception e) {
            log.info("Exception Occurred while User Validation :: {}", e.getMessage());
            isAuthenticated = userManagementService.unAuthorizedUser(session, ScsbConstants.LOGIN_USER, log);
            resultMap.put(ScsbConstants.IS_AUTHENTICATED, isAuthenticated);
        }
        return resultMap;
    }
}
