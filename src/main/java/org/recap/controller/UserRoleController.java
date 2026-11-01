package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.RoleEntity;
import org.recap.model.jpa.UsersEntity;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.model.usermanagement.UserRoleForm;
import org.recap.model.usermanagement.UserRoleService;
import org.recap.repository.jpa.UserDetailsRepository;
import org.recap.security.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Created by dharmendrag on 23/12/16.
 */
@Slf4j
@RestController
@RequestMapping("/userRoles")
public class UserRoleController extends AbstractController {


    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserManagementService userManagementService;

    /**
     * Gets user details repository.
     *
     * @return the user details repository
     */
    public UserDetailsRepository getUserDetailsRepository() {
        return userDetailsRepository;
    }

    public UserRoleService getUserRoleService() {
        return userRoleService;
    }

    public UserManagementService getUserManagementService() {
        return userManagementService;
    }

    private UserRoleForm getAndSetRolesAndInstitutions(UserRoleForm userRoleForm, UserDetailsForm userDetailsForm) {
        return setUserRoleForm(userRoleForm, userDetailsForm);
    }

    /**
     * Gets user search results from scsb database and display them as rows in the search user UI page.
     *
     * @param userRoleForm the user role form
     * @return the model and view
     */
    @PostMapping("/searchUsers")
    public UserRoleForm searchUserRole(@RequestBody UserRoleForm userRoleForm, HttpServletRequest request) {
        HttpSession session = request.getSession();
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        if (authenticated) {
            try {
                userRoleForm = priorSearch(userRoleForm, request);
                userRoleForm.setShowPagination(true);
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR, e);
            }
        }
        return userRoleForm;
    }

    @PostMapping("/exportUsers")
    public UserRoleForm exportUsers(@RequestBody UserRoleForm userRoleForm, HttpServletRequest request) {
        HttpSession session = request.getSession();
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        if (authenticated) {
            try {
                userRoleForm.setIsExport(ScsbConstants.IS_EXPORT_TRUE);
                userRoleForm = priorSearch(userRoleForm, request);
                userRoleForm.setShowPagination(true);
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR, e);
            }
        }
        return userRoleForm;
    }
    /**
     * To delete the user permanently in scsb.
     *
     * @param userId         the user id
     * @param networkLoginId the network login id
     * @param pageNumber     the page number
     * @param totalPageCount the total page count
     * @param pageSize       the page size
     * @return the model and view
     */
    @GetMapping("/delete")
    public UserRoleForm deleteUser(@RequestParam("userId") Integer userId, @RequestParam("networkLoginId") String networkLoginId, @RequestParam("pageNumber") Integer pageNumber, @RequestParam("totalPageCount") Integer totalPageCount, @RequestParam("pageSize") Integer pageSize, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        UserRoleForm userRoleForm = new UserRoleForm();
        if (authenticated) {
            UsersEntity usersEntity = new UsersEntity();
            usersEntity.setId(userId);
            try {
                getUserDetailsRepository().delete(usersEntity);
                userRoleForm.setDeletedSuccessMsg(true);
                userRoleForm.setMessage(networkLoginId + ScsbConstants.DELETED_SUCCESSFULLY);
                userRoleForm = priorSearch(userRoleForm, request);
                userRoleForm.setAfterDelPageNumber(pageNumber);
                userRoleForm.setAfterDelTotalPageCount(totalPageCount);
                userRoleForm.setAfterDelPageSize(pageSize);
                userRoleForm.setShowPagination(true);
                userRoleForm.setShowResults(true);
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR, e);
            }
            userRoleForm.setShowUserSearchView(true);
        }
        return userRoleForm;
    }

    /**
     * Gets first page user search results from scsb database and display them as rows in the search user UI page.
     *
     * @param userRoleForm the user role form
     * @return the model and view
     */
    @PostMapping("/first")
    public UserRoleForm searchFirstPage(@RequestBody UserRoleForm userRoleForm, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        if (authenticated) {
            userRoleForm.resetPageNumber();
            userRoleForm = priorSearch(userRoleForm, request);
        }
        return userRoleForm;
    }

    /**
     * Gets next page user search results from scsb database and display them as rows in the search user UI page.
     *
     * @param userRoleForm the user role form
     * @return the model and view
     */
    @PostMapping("/next")
    public UserRoleForm searchNextPage(@RequestBody UserRoleForm userRoleForm, HttpServletRequest request) {
        userRoleForm.setPageNumber(userRoleForm.getPageNumber() + 1);
        return priorSearch(userRoleForm, request);
    }

    /**
     * Gets previous page user search results from scsb database and display them as rows in the search user UI page.
     *
     * @param userRoleForm the user role form
     * @return the model and view
     */
    @PostMapping("/previous")
    public UserRoleForm searchPreviousPage(@RequestBody UserRoleForm userRoleForm, HttpServletRequest request) {
        userRoleForm.setPageNumber(userRoleForm.getPageNumber() - 1);
        return priorSearch(userRoleForm, request);
    }

    /**
     * Gets last page user search results from scsb database and display them as rows in the search user UI page.
     *
     * @param userRoleForm the user role form
     * @return the model and view
     */
    @PostMapping("/last")
    public UserRoleForm searchLastPage(@RequestBody UserRoleForm userRoleForm, HttpServletRequest request) {
        userRoleForm.setPageNumber(userRoleForm.getPageNumber() - 1);
        return priorSearch(userRoleForm, request);
    }

    /**
     * To create a new user in the scsb.
     *
     * @param userRoleForm the user role form
     * @return the model and view
     */
    @PostMapping("/createUser")
    public UserRoleForm createUserRequest(@RequestBody UserRoleForm userRoleForm, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        if (authenticated) {
            UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(session, ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE);
            userRoleForm = getAndSetRolesAndInstitutions(userRoleForm, userDetailsForm);
            Object userName = session.getAttribute(ScsbConstants.USER_NAME);
            userRoleForm.setCreatedBy(String.valueOf(userName));
            UsersEntity usersEntity = getUserRoleService().saveNewUserToDB(userRoleForm);
            if (usersEntity != null) {
                userRoleForm.setShowCreateSuccess(true);
                userRoleForm.setAllowCreateEdit(true);
            }
            userRoleForm.setShowUserSearchView(false);
        }
        return userRoleForm;
    }

    /**
     * Provide information about the user which has been selected to edit in scsb.
     *
     * @param userId         the user id
     * @param networkLoginId the network login id
     * @return the model and view
     */
    @GetMapping("/editUser")
    public UserRoleForm editUser(@RequestParam("userId") Integer userId, @RequestParam("networkLoginId") String networkLoginId, HttpServletRequest request) {
        UserRoleForm userRoleForm = new UserRoleForm();
        HttpSession session = request.getSession(false);
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        if (authenticated) {
            UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE);
            Optional<UsersEntity> usersEntity = getUserDetailsRepository().findById(userId);

            if (usersEntity.isPresent()) {
                getAndSetRolesAndInstitutions(userRoleForm, userDetailsForm);
                userRoleForm.setEditNetworkLoginId(usersEntity.get().getLoginId());
                userRoleForm.setEditUserDescription(usersEntity.get().getUserDescription());
                userRoleForm.setEditUserId(userId);
                userRoleForm.setUserId(userId);
                userRoleForm.setEmailId(usersEntity.get().getEmailId());
                userRoleForm.setEditEmailId(usersEntity.get().getEmailId());
                setUserRoleFormRoleId(userRoleForm, usersEntity);
                userRoleForm.setShowSelectedForCreate(userRoleForm.getEditSelectedForCreate());
                userRoleForm.setEditInstitutionId(usersEntity.get().getInstitutionId());
                userRoleForm.setShowUserSearchView(false);
            }
        }
        return userRoleForm;
    }

    /**
     * To save the edited user details in scsb.
     *
     * @param userId          the user id
     * @param networkLoginId  the network login id
     * @param userDescription the user description
     * @param institutionId   the institution id
     * @param userEmailId     the user email id
     * @return the model and view
     */
    @GetMapping("/saveEditUserDetails")
    public UserRoleForm saveEditUserDetails(@RequestParam("userId") Integer userId, @RequestParam("networkLoginId") String networkLoginId,
                                            @RequestParam("userDescription") String userDescription,
                                            @RequestParam("institutionId") Integer institutionId,
                                            @RequestParam("userEmailId") String userEmailId,
                                            @RequestParam("roleIds") List<Integer> roleIds, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        UserRoleForm userRoleForm = new UserRoleForm();
        if (authenticated) {
            UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE);
            List<Integer> roleIdsUpdated = new ArrayList<>(roleIds);
            userRoleForm.setMessage(networkLoginId + ScsbConstants.EDITED_AND_SAVED);
            getAndSetRolesAndInstitutions(userRoleForm, userDetailsForm);
            userRoleForm.setUserId(userId);
            Object userName = session.getAttribute(ScsbConstants.USER_NAME);
            userRoleForm.setLastUpdatedBy(String.valueOf(userName));
            UsersEntity usersEntity = getUserRoleService().saveEditedUserToDB(userId, networkLoginId, userDescription, institutionId, roleIdsUpdated, userEmailId, userRoleForm);
            if (usersEntity != null) {
                userRoleForm.setShowEditSuccess(true);
                userRoleForm.setEditNetworkLoginId(usersEntity.getLoginId());
                userRoleForm.setEditUserDescription(usersEntity.getUserDescription());
                userRoleForm.setUserId(userRoleForm.getUserId());
                userRoleForm.setEditUserId(userRoleForm.getUserId());
                List<RoleEntity> roleEntityList = usersEntity.getUserRole();
                List<Integer> roleIdss = new ArrayList<>();
                if (roleEntityList != null) {
                    for (RoleEntity roleEntity : roleEntityList) {
                        roleIdss.add(roleEntity.getId());
                    }
                }
                userRoleForm.setEditSelectedForCreate(roleIdss);
                userRoleForm.setShowSelectedForCreate(userRoleForm.getEditSelectedForCreate());
                userRoleForm.setEditInstitutionId(usersEntity.getId());
                userRoleForm.setEditEmailId(usersEntity.getEmailId());
            } else {
                userRoleForm.setShowEditError(true);
                userRoleForm.setEditErromessage(networkLoginId + ScsbConstants.ALREADY_EXISTS);
                userRoleForm.setEditNetworkLoginId(networkLoginId);
                userRoleForm.setEditUserDescription(userDescription);
                userRoleForm.setEditSelectedForCreate(roleIds);
                userRoleForm.setShowSelectedForCreate(userRoleForm.getEditSelectedForCreate());
                userRoleForm.setEditInstitutionId(institutionId);
                userRoleForm.setEditEmailId(userEmailId);
            }
            userRoleForm.setShowUserSearchView(false);
        }
        return userRoleForm;
    }

    /**
     * Get back to the user search UI page from edit, delete and create user UI pages.
     *
     * @return the model and view
     */
    @GetMapping("/userRoles")
    public UserRoleForm userRoles(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean authenticated = getUserAuthUtil().isAuthenticated(session, ScsbConstants.SCSB_SHIRO_USER_ROLE_URL);
        UserRoleForm userRoleForm = new UserRoleForm();
        if (authenticated) {
            UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(session, ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE);
            userRoleForm = setUserRoleForm(userRoleForm, userDetailsForm);
        }
        userRoleForm.setShowUserSearchView(true);
        return userRoleForm;
    }

    private UserRoleForm priorSearch(UserRoleForm userRoleForm, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Integer userId = (Integer) session.getAttribute(ScsbConstants.USER_ID);
        UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(session, ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE);
        List<Object> institutions = getUserRoleService().getInstitutions(userDetailsForm.isSuperAdmin(), userDetailsForm.getLoginInstitutionId());
        List<Object> roles = getUserRoleService().getRoles(getUserManagementService().getSuperAdminRoleId(), userDetailsForm.isSuperAdmin());
        userRoleForm.setUserId(userId);
        userRoleForm.setInstitutionId(userDetailsForm.getLoginInstitutionId());
        userRoleForm.setRoles(roles);
        userRoleForm.setInstitutions(institutions);
        userRoleForm.setAllowCreateEdit(true);
        userRoleForm.setSubmitted(true);
        return searchAndSetResult(request, userRoleForm, userDetailsForm.isSuperAdmin(), userId);
    }

    private UserRoleForm searchAndSetResult(HttpServletRequest request,UserRoleForm userRoleForm, boolean superAdmin, Integer userId) {
        if (StringUtils.isBlank(userRoleForm.getSearchNetworkId()) && StringUtils.isBlank(userRoleForm.getUserEmailId()) && userRoleForm.getIsExport()) {
            log.debug("Search All Users");
            List<UsersEntity> usersEntities = getUserRoleService().findAll(userRoleForm, superAdmin);
            userRoleForm.setUserRoleFormList(setFormValues(request,usersEntities, userId));
            userRoleForm.setShowResults(false);
        } else if (StringUtils.isBlank(userRoleForm.getSearchNetworkId()) && StringUtils.isBlank(userRoleForm.getUserEmailId())) {
            log.debug("Export All Users");
            Page<UsersEntity> usersEntities = getUserRoleService().searchUsers(userRoleForm, superAdmin);
            userRoleForm = setUserRoleFormValues(request,userRoleForm, usersEntities, userId);
        } else if (StringUtils.isNotBlank(userRoleForm.getSearchNetworkId()) && StringUtils.isBlank(userRoleForm.getUserEmailId())) {
            log.debug("Search Users By NetworkId : {}", userRoleForm.getSearchNetworkId());
            Page<UsersEntity> usersEntities = getUserRoleService().searchByNetworkId(userRoleForm, superAdmin);
            userRoleForm = getUsersInformation(request,userRoleForm, userId, usersEntities, ScsbConstants.NETWORK_LOGIN_ID_DOES_NOT_EXIST);
        } else if (StringUtils.isBlank(userRoleForm.getSearchNetworkId()) && StringUtils.isNotBlank(userRoleForm.getUserEmailId())) {
            log.debug("Search Users by Email Id: {}", userRoleForm.getUserEmailId());
            Page<UsersEntity> usersEntities = getUserRoleService().searchByUserEmailId(userRoleForm, superAdmin);
            userRoleForm = getUsersInformation(request,userRoleForm, userId, usersEntities, ScsbConstants.EMAILID_ID_DOES_NOT_EXIST);
        } else if (StringUtils.isNotBlank(userRoleForm.getSearchNetworkId()) && StringUtils.isNotBlank(userRoleForm.getUserEmailId())) {
            log.debug("Search Users by Network Id : {} and Email Id : {}", userRoleForm.getSearchNetworkId(), userRoleForm.getUserEmailId());
            Page<UsersEntity> usersEntities = getUserRoleService().searchByNetworkIdAndUserEmailId(userRoleForm, superAdmin);
            userRoleForm = getUsersInformation(request,userRoleForm, userId, usersEntities, ScsbConstants.NETWORK_LOGIN_ID_AND_EMAILID_ID_DOES_NOT_EXIST);
        } else {
            userRoleForm.setShowResults(false);
        }
        return userRoleForm;
    }
    private UserRoleForm getUsersInformation(HttpServletRequest request,UserRoleForm userRoleForm, Integer userId, Page<UsersEntity> usersEntities, String message) {
        List<UsersEntity> userEntity = usersEntities.getContent();
        if (!userEntity.isEmpty()) {
            setUserRoleFormValues(request,userRoleForm, usersEntities, userId);
        } else {
            userRoleForm.setMessage(message);
            userRoleForm.setShowErrorMessage(true);
            userRoleForm.setShowResults(false);
        }
        return userRoleForm;
    }

    private List<UserRoleForm> setFormValues(HttpServletRequest request,List<UsersEntity> usersEntities, Integer userId) {
        List<UserRoleForm> userRoleFormList = new ArrayList<>();
        appendValues(request,usersEntities, userRoleFormList, userId);
        return userRoleFormList;
    }

    private void appendValues(HttpServletRequest request,Collection<UsersEntity> usersEntities, List<UserRoleForm> userRoleFormList, Integer userId) {
        for (UsersEntity usersEntity : usersEntities) {
            InstitutionEntity institutionEntity = usersEntity.getInstitutionEntity();
            List<RoleEntity> userRole = usersEntity.getUserRole();
            boolean addUsers = true;
            HttpSession session = request.getSession(false);
            Object isSuperAdmin = session.getAttribute(ScsbConstants.SUPER_ADMIN_USER);
            if (!(boolean) isSuperAdmin) {
                for (RoleEntity superAdminCheck : userRole) {
                    if (superAdminCheck.getRoleName().equals(ScsbConstants.ROLES_SUPER_ADMIN)) {
                        addUsers = false;
                    }
                }
            }
            if (addUsers) {
                UserRoleForm userRoleDetailsForm = new UserRoleForm();
                StringBuilder rolesBuffer = new StringBuilder();
                userRoleDetailsForm.setUserId(usersEntity.getId());
                userRoleDetailsForm.setInstitutionId(institutionEntity.getId());
                userRoleDetailsForm.setInstitutionName(institutionEntity.getInstitutionName());
                userRoleDetailsForm.setNetworkLoginId(usersEntity.getLoginId());
                userRoleDetailsForm.setUserDescription(usersEntity.getUserDescription());
                for (RoleEntity roleEntity : usersEntity.getUserRole()) {
                    rolesBuffer.append(roleEntity.getRoleName() + ",");
                }
                userRoleDetailsForm.setRoleName(roles(rolesBuffer.toString(), ","));
                userRoleDetailsForm.setShowEditDeleteIcon(userRoleDetailsForm.getUserId() != userId);
                userRoleFormList.add(userRoleDetailsForm);//Added all user's details
            }
        }
    }

    private String roles(String rolesBuffer, String seperator) {
        if (rolesBuffer != null && rolesBuffer.endsWith(seperator)) {
            return rolesBuffer.substring(0, rolesBuffer.length() - 1);
        }
        return null;
    }

    private UserRoleForm setUserRoleForm(UserRoleForm userRoleForm, UserDetailsForm userDetailsForm) {
        List<Object> roles = getUserRoleService().getRolesCall(getUserManagementService().getSuperAdminRoleId(), userDetailsForm.isSuperAdmin(),userDetailsForm.isUserAdministrator());
        List<Object> institutions = getUserRoleService().getInstitutions(userDetailsForm.isSuperAdmin(), userDetailsForm.getLoginInstitutionId());
        userRoleForm.setRoles(roles);
        userRoleForm.setInstitutions(institutions);
        return userRoleForm;
    }

    private UserRoleForm setUserRoleFormValues(HttpServletRequest request,UserRoleForm userRoleForm, Page<UsersEntity> usersEntities, Integer userId) {
        userRoleForm.setUserRoleFormList(setFormValues(request,usersEntities.getContent(), userId));
        userRoleForm.setShowResults(true);
        userRoleForm.setTotalRecordsCount(String.valueOf(usersEntities.getTotalElements()));
        userRoleForm.setTotalPageCount(usersEntities.getTotalPages());
        return userRoleForm;
    }

    private void setUserRoleFormRoleId(UserRoleForm userRoleForm, Optional<UsersEntity> usersEntity) {
        if (usersEntity.isPresent()) {
            List<RoleEntity> roleEntityList = usersEntity.get().getUserRole();
            List<Integer> roleIds = new ArrayList<>();
            if (roleEntityList != null) {
                for (RoleEntity roleEntity : roleEntityList) {
                    roleIds.add(roleEntity.getId());
                }
            }
            userRoleForm.setEditSelectedForCreate(roleIds);
        }
    }
}
