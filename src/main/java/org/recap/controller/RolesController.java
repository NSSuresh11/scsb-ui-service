package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.PermissionEntity;
import org.recap.model.jpa.RoleEntity;
import org.recap.model.search.RolesForm;
import org.recap.model.search.RolesSearchResult;
import org.recap.repository.jpa.PermissionsDetailsRepository;
import org.recap.repository.jpa.RolesDetailsRepositorty;
import org.recap.security.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hemalathas on 22/12/16.
 */
@Slf4j
@RestController
@RequestMapping("/roles")
public class RolesController extends AbstractController {


    @Autowired
    private RolesDetailsRepositorty rolesDetailsRepositorty;

    @Autowired
    private PermissionsDetailsRepository permissionsRepository;

    @Autowired
    private  UserManagementService userManagementService;

    /**
     * Gets role search results from scsb database and display them as rows in the roles UI page.
     *
     * @param rolesForm the roles form
     * @return the model and view
     */
    @PostMapping("/searchRoles")
    public RolesForm search(@RequestBody RolesForm rolesForm) {
        rolesForm.setShowResults(true);
        log.info("searchRoles calling with the following payload :: {}", rolesForm);
        return setRolesFormSearchResults(rolesForm);
    }

    /**
     * Populate permission names that are available in scsb to the permission names drop down.
     *
     * @return the model and view
     */
    @GetMapping("/populatePermissionName")
    public RolesForm populatePermissionNames() {
        return getAllPermissionNames();
    }

    /**
     * This is used to add a new role in scsb.
     *
     * @param rolesForm the roles form
     * @return the model and view
     */
    @PostMapping("/createRole")
    public RolesForm newRole(@RequestBody RolesForm rolesForm, HttpServletRequest request) {
        boolean specialCharacterCheck = isSpecialCharacterCheck(rolesForm.getNewRoleName());
        log.info("create Role calling with the following payload: {}", rolesForm);
        if (!specialCharacterCheck) {
            rolesForm.setErrorMessage(ScsbConstants.SPECIAL_CHARACTERS_NOT_ALLOWED_CREATE);
            rolesForm.setSelectedPermissionNames(getSelectedPermissionNames(rolesForm.getNewPermissionNames()));
        } else {
            HttpSession session = request.getSession(false);
            String username = (String) session.getAttribute(ScsbConstants.USER_NAME);
            RoleEntity roleEntity = saveNewRoleToDB(rolesForm, username);
            if (null != roleEntity) {
                rolesForm.setMessage(rolesForm.getNewRoleName() + ScsbConstants.ADDED_SUCCESSFULLY);
            } else {
                rolesForm.setErrorMessage(rolesForm.getNewRoleName() + ScsbConstants.ALREADY_EXISTS);
            }
            rolesForm.setNewRoleName("");
            rolesForm.setNewRoleDescription("");
        }
        rolesForm.setPermissionNameList(getAllPermissionNames().getPermissionNameList());
        rolesForm.setShowIntial(false);
        return rolesForm;
    }

    /**
     * Provide information about the role which has been selected to edit in scsb.
     *
     * @param roleId          the role id
     * @param roleName        the role name
     * @param roleDescription the role description
     * @param permissionName  the permission name
     * @return the model and view
     */
    @GetMapping("/editRole")
    public RolesForm editRole(Integer roleId, String roleName, String roleDescription, String permissionName) {
        String htmlUnescapePermissionName = HtmlUtils.htmlUnescape(permissionName);
        RolesForm rolesForm = getAllPermissionNames();
        rolesForm.setRoleId(roleId);
        rolesForm.setEditRoleName(roleName);
        rolesForm.setEditRoleDescription(roleDescription);
        rolesForm.setEditPermissionNames(htmlUnescapePermissionName);
        rolesForm.setSelectedPermissionNames(getSelectedPermissionNames(htmlUnescapePermissionName));
        rolesForm.setShowIntial(false);
        return rolesForm;
    }

    /**
     * To save the edited role details in scsb.
     *
     * @param roleId              the role id
     * @param roleName            the role name
     * @param roleDescription     the role description
     * @param editPermissionNames the request
     * @return the model and view
     */
    @GetMapping("/saveEditedRole")
    public RolesForm saveEditedRole(@RequestParam("roleId") Integer roleId,
                                    @RequestParam("roleName") String roleName,
                                    @RequestParam("roleDescription") String roleDescription,
                                    @RequestParam("editPermissionNames") String[] editPermissionNames, HttpServletRequest request) {
        log.info("edit Role calling: {}", roleName);
        RolesForm rolesForm = new RolesForm();
        HttpSession session = request.getSession(false);
        String username = (String) session.getAttribute(ScsbConstants.USER_NAME);
        rolesForm.setRoleId(roleId);
        rolesForm.setEditRoleName(roleName);
        rolesForm.setEditRoleDescription(roleDescription);
        rolesForm.setEditPermissionName(Arrays.asList(editPermissionNames));
        Optional<RoleEntity> roleEntityByRoleId = rolesDetailsRepositorty.findById(roleId);
        if (roleEntityByRoleId.isPresent()) {
            roleEntityByRoleId.get().setId(roleId);
            roleEntityByRoleId.get().setRoleName(roleName);
            roleEntityByRoleId.get().setRoleDescription(roleDescription);
            roleEntityByRoleId.get().setLastUpdatedDate(new Date());
            roleEntityByRoleId.get().setLastUpdatedBy(username);
            RoleEntity roleEntity = saveRoleEntity(roleEntityByRoleId.get(), Arrays.asList(editPermissionNames));
            if (null != roleEntity) {
                rolesForm.setMessage(rolesForm.getEditRoleName() + ScsbConstants.EDITED_AND_SAVED);
            } else {
                rolesForm.setErrorMessage(rolesForm.getEditRoleName() + ScsbConstants.ALREADY_EXISTS);
            }
        }
        rolesForm.setPermissionNameList(getAllPermissionNames().getPermissionNameList());
        rolesForm.setSelectedPermissionNames(Arrays.asList(editPermissionNames));
        rolesForm.setShowIntial(false);
        return rolesForm;
    }

    /**
     * Provide information about the role which has been selected to delete in scsb.
     *
     * @param roleId          the role id
     * @param roleName        the role name
     * @param roleDescription the role description
     * @param permissionName  the permission name
     * @param pageSize        the page size
     * @param pageNumber      the page number
     * @param totalPageCount  the total page count
     * @return the model and view
     */
    @GetMapping("/deleteRole")
    public RolesForm deleteRole(Integer roleId, String roleName, String roleDescription, String permissionName,
                                Integer pageSize, Integer pageNumber, Integer totalPageCount) {
        String htmlUnescapePermissionName = HtmlUtils.htmlUnescape(permissionName);
        RolesForm rolesForm = getAllPermissionNames();
        rolesForm.setAfterDelPageSize(pageSize);
        rolesForm.setAfterDelPageNumber(pageNumber);
        rolesForm.setAfterDelTotalPageCount(totalPageCount);
        rolesForm.setRoleId(roleId);
        rolesForm.setRoleNameForDelete(roleName);
        rolesForm.setRoleDescriptionForDelete(roleDescription);
        rolesForm.setPermissionNamesForDelete(htmlUnescapePermissionName);
        rolesForm.setSelectedPermissionNames(getSelectedPermissionNames(htmlUnescapePermissionName));
        rolesForm.setPageSize(pageSize);
        rolesForm.setPageNumber(pageNumber);
        rolesForm.setTotalPageCount(totalPageCount);
        rolesForm.setShowIntial(false);
        return rolesForm;
    }

    /**
     * To delete the role permanently in scsb.
     *
     * @param rolesForm the roles form
     * @return the model and view
     */
    @PostMapping("/delete")
    public RolesForm delete(@RequestBody RolesForm rolesForm) {
        log.info("deleting Role calling : {}", rolesForm);
        Optional<RoleEntity> roleEntity = rolesDetailsRepositorty.findById(rolesForm.getRoleId());
        if (roleEntity.isPresent()) {
            try {
                rolesDetailsRepositorty.delete(roleEntity.get());
                rolesForm.setShowResults(true);
                rolesForm.setPageNumber(rolesForm.getAfterDelPageNumber());
                rolesForm.setPageSize(rolesForm.getAfterDelPageSize());
                rolesForm.setTotalPageCount(rolesForm.getAfterDelTotalPageCount());
                rolesForm.setRoleName("");
                rolesForm.setPermissionNames("");
                setRolesFormSearchResults(rolesForm);
                rolesForm.setMessage(rolesForm.getRoleNameForDelete() + ScsbConstants.DELETED_SUCCESSFULLY);
            } catch (Exception e) {
                log.error(ScsbCommonConstants.LOG_ERROR, e);
            }
        } else {
            log.error(ScsbCommonConstants.LOG_ERROR + "{}", "Role is Null");
        }
        rolesForm.setShowResults(true);
        return rolesForm;
    }

    /**
     * Gets previous page role search results from scsb database and display them as rows in the roles UI page.
     *
     * @param rolesForm the roles form
     * @return the model and view
     */
    @PostMapping("/previous")
    public RolesForm searchPrevious(@RequestBody RolesForm rolesForm) {
        rolesForm.setPageNumber(rolesForm.getPageNumber() - 1);
        return searchPage(rolesForm);
    }

    /**
     * Gets next page role search results from scsb database and display them as rows in the roles UI page.
     *
     * @param rolesForm the roles form
     * @return the model and view
     */
    @PostMapping("/next")
    public RolesForm searchNext(@RequestBody RolesForm rolesForm) {
        rolesForm.setPageNumber(rolesForm.getPageNumber() + 1);
        return searchPage(rolesForm);
    }

    /**
     * Gets first page role search results from scsb database and display them as rows in the roles UI page.
     *
     * @param rolesForm the roles form
     * @return the model and view
     */
    @PostMapping("/first")
    public RolesForm searchFirst(@RequestBody RolesForm rolesForm) {
        rolesForm.setShowResults(true);
        rolesForm.resetPageNumber();
        return setRolesForm(rolesForm);
    }

    /**
     * Gets last page role search results from scsb database and display them as rows in the roles UI page.
     *
     * @param rolesForm the roles form
     * @return the model and view
     */
    @PostMapping("/last")
    public RolesForm searchLast(@RequestBody RolesForm rolesForm) {
        rolesForm.setShowResults(true);
        rolesForm.setPageNumber(rolesForm.getPageNumber() - 1);
        return setRolesForm(rolesForm);
    }

    /**
     * Based on the selected page size the roles will be displayed in the roles UI page.
     *
     * @param rolesForm the roles form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/pageSizeChange")
    public RolesForm onPageSizeChange(@RequestBody RolesForm rolesForm) throws Exception {
        rolesForm.setShowResults(true);
        rolesForm.setPageNumber(0);
        return setRolesForm(rolesForm);
    }

    /**
     * Get back to the role search page from edit, delete and create roles UI pages.
     *
     * @param rolesForm the roles form
     * @param model     the model
     * @return the model and view
     */
    @PostMapping("/goBack")
    public ModelAndView goBack(RolesForm rolesForm, Model model) {
        rolesForm.setShowIntial(true);
        return new ModelAndView("roles", "rolesForm", rolesForm);
    }

    /**
     * Gets role entities result set from scsb database based on the search condition given in the search roles UI page.
     *
     * @param rolesForm the roles form
     */
    public RolesForm setRolesFormSearchResults(RolesForm rolesForm) {
        List<RolesSearchResult> rolesSearchResults = new ArrayList<>();
        rolesForm.reset();
        if (rolesForm.getRoleName().equalsIgnoreCase(ScsbConstants.ROLES_SUPER_ADMIN) || rolesForm.getPermissionNames().equalsIgnoreCase(ScsbConstants.ROLES_SUPER_ADMIN)) {
            if (rolesForm.getRoleName().equalsIgnoreCase(ScsbConstants.ROLES_SUPER_ADMIN)) {
                rolesForm.setErrorMessage(ScsbConstants.INVALID_ROLE_NAME);
            } else {
                rolesForm.setErrorMessage(ScsbConstants.INVALID_PERMISSION);
            }

        } else if (!StringUtils.isEmpty(rolesForm.getRoleName()) && StringUtils.isEmpty(rolesForm.getPermissionNames())) {
            if (isSpecialCharacterCheck(rolesForm.getRoleName())) {
                Pageable pageable = PageRequest.of(rolesForm.getPageNumber(), rolesForm.getPageSize());
                Page<RoleEntity> rolesEntityListByPagination = rolesDetailsRepositorty.findByRoleName(pageable, rolesForm.getRoleName());
                List<RoleEntity> roleEntityList = rolesEntityListByPagination.getContent();
                rolesForm.setTotalRecordCount(NumberFormat.getNumberInstance().format(rolesEntityListByPagination.getTotalElements()));
                rolesForm.setTotalPageCount(rolesEntityListByPagination.getTotalPages());
                if (!roleEntityList.isEmpty()) {
                    for (RoleEntity roleEntity : roleEntityList) {
                        rolesForm.setTotalRecordCount(String.valueOf(1));
                        RolesSearchResult rolesSearchResult = getRolesSearchResult(roleEntity);
                        rolesSearchResults.add(rolesSearchResult);
                        rolesForm.setRolesSearchResults(rolesSearchResults);
                    }
                } else {
                    rolesForm.setErrorMessage(ScsbConstants.INVALID_ROLE_NAME);
                }
            } else {
                rolesForm.setErrorMessage(ScsbConstants.SPECIAL_CHARACTERS_NOT_ALLOWED);
            }
            rolesForm.setPageSize(10);
        } else if (!StringUtils.isEmpty(rolesForm.getPermissionNames()) && StringUtils.isEmpty(rolesForm.getRoleName())) {
            if (isSpecialCharacterCheck(rolesForm.getPermissionNames())) {
                Pageable pageable = PageRequest.of(rolesForm.getPageNumber(), rolesForm.getPageSize());
                PermissionEntity pemissionEntity = permissionsRepository.findByPermissionName(rolesForm.getPermissionNames());
                getResultsForNonEmptyRolePermissionName(rolesForm, rolesSearchResults, pageable, pemissionEntity);
            }
        } else if (!StringUtils.isEmpty(rolesForm.getRoleName()) && !StringUtils.isEmpty(rolesForm.getPermissionNames())) {
            if (isSpecialCharacterCheck(rolesForm.getPermissionNames())) {
                RoleEntity roleEntity = rolesDetailsRepositorty.findByRoleName(rolesForm.getRoleName());
                if (null != roleEntity) {
                    boolean isExist = false;
                    RolesSearchResult rolesSearchResult = new RolesSearchResult();
                    for (PermissionEntity permissionEnt : roleEntity.getPermissions()) {
                        if (rolesForm.getPermissionNames().equalsIgnoreCase(permissionEnt.getPermissionName())) {
                            isExist = true;
                        }
                    }
                    if (isExist) {
                        StringBuilder allPermissions = new StringBuilder();
                        for (PermissionEntity permissionEnt : roleEntity.getPermissions()) {
                            allPermissions.append(permissionEnt.getPermissionName());
                            allPermissions.append(", ");
                        }
                        rolesForm.setTotalPageCount(1);
                        rolesForm.setTotalRecordCount(String.valueOf(1));
                        rolesForm.setPageSize(10);
                        rolesSearchResult.setRoleId(roleEntity.getId());
                        rolesSearchResult.setPermissionName(allPermissions.toString());
                        rolesSearchResult.setRolesName(roleEntity.getRoleName());
                        rolesSearchResult.setRolesDescription(roleEntity.getRoleDescription());
                        rolesSearchResults.add(rolesSearchResult);
                        rolesForm.setRolesSearchResults(rolesSearchResults);
                    } else {
                        rolesForm.setErrorMessage(ScsbConstants.WRONG_PERMISSION);
                    }
                } else {
                    rolesForm.setErrorMessage(ScsbConstants.INVALID_ROLE_NAME);
                }
            } else {
                rolesForm.setErrorMessage(ScsbConstants.INVALID_ROLE_NAME);
            }
            rolesForm.setPageSize(10);
        } else if (StringUtils.isEmpty(rolesForm.getRoleName()) && StringUtils.isEmpty(rolesForm.getPermissionNames())) {
            Pageable pageable = PageRequest.of(rolesForm.getPageNumber(), rolesForm.getPageSize());
            Page<RoleEntity> rolesEntityListByPagination = rolesDetailsRepositorty.getRolesWithoutSuperAdmin(pageable);
            searchRolesEntity(rolesForm, rolesSearchResults, rolesEntityListByPagination);
        }
        return rolesForm;
    }

    private void getResultsForNonEmptyRolePermissionName(RolesForm rolesForm, List<RolesSearchResult> rolesSearchResults, Pageable pageable, PermissionEntity pemissionEntity) {
        if (pemissionEntity != null) {
            List<Integer> roleIdList = rolesDetailsRepositorty.getIdforPermissionName(pemissionEntity.getId());
            Page<RoleEntity> roleEntity = null;
            if (roleIdList != null) {
                roleEntity = rolesDetailsRepositorty.findByRoleIDs(pageable, roleIdList);
                List<RoleEntity> roleEntityList = roleEntity.getContent();
                for (RoleEntity entity : roleEntityList) {
                    RolesSearchResult rolesSearchResult = getRolesSearchResult(entity);
                    rolesSearchResults.add(rolesSearchResult);
                }
            }
            rolesForm.setRolesSearchResults(rolesSearchResults);
            if (roleEntity != null) {
                rolesForm.setTotalRecordCount(String.valueOf(roleEntity.getTotalElements()));
                rolesForm.setTotalPageCount(roleEntity.getTotalPages());
            }
        } else {
            rolesForm.setErrorMessage(ScsbConstants.INVALID_PERMISSION);
        }
    }

    private List<String> getSelectedPermissionNames(String permissionName) {
        List<String> permissionNames = new ArrayList<>();
        if (!StringUtils.isEmpty(permissionName)) {
            StringTokenizer stringTokenizer = new StringTokenizer(permissionName, ",");
            while (stringTokenizer.hasMoreTokens()) {
                String token = stringTokenizer.nextToken();
                String trim = token.trim();
                if (!StringUtils.isEmpty(trim)) {
                    permissionNames.add(trim);
                }
            }
        }
        return permissionNames;
    }


    /**
     * Get values from role entity and assign those values to the role search result.
     *
     * @param roleEntity the role entity
     * @return the roles search result
     */
    public RolesSearchResult getRolesSearchResult(RoleEntity roleEntity) {
        StringBuilder permission = new StringBuilder();
        RolesSearchResult rolesSearchResult = new RolesSearchResult();
        for (PermissionEntity permissionEntity : roleEntity.getPermissions()) {
            permission.append(permissionEntity.getPermissionName());
            permission.append(", ");
        }
        String permissionName = getSelectedPermissionNames(permission.toString()).toString().replaceAll("\\[", "").replaceAll("\\]", "");
        rolesSearchResult.setPermissionName(permissionName);
        rolesSearchResult.setRolesName(roleEntity.getRoleName());
        rolesSearchResult.setRolesDescription(roleEntity.getRoleDescription());
        rolesSearchResult.setRoleId(roleEntity.getId());
        return rolesSearchResult;
    }


    private boolean isSpecialCharacterCheck(String inputString) {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9_ ]*");
        Matcher matcher = pattern.matcher(inputString);
        return matcher.matches();
    }

    /**
     * This method is used to paginate the role search results.
     *
     * @param rolesForm the roles form
     */
    public RolesForm findByPagination(RolesForm rolesForm) {
        List<RolesSearchResult> rolesSearchResults = new ArrayList<>();
        Pageable pageable = PageRequest.of(rolesForm.getPageNumber(), rolesForm.getPageSize());
        if (!StringUtils.isEmpty(rolesForm.getRoleName()) && StringUtils.isEmpty(rolesForm.getPermissionNames())) {
            Page<RoleEntity> rolesEntityListByPagination = rolesDetailsRepositorty.findByRoleName(pageable, rolesForm.getRoleName());
            searchRolesEntity(rolesForm, rolesSearchResults, rolesEntityListByPagination);
        } else if (StringUtils.isEmpty(rolesForm.getRoleName()) && !StringUtils.isEmpty(rolesForm.getPermissionNames())) {
            Pageable pageable1 = PageRequest.of(rolesForm.getPageNumber(), rolesForm.getPageSize());
            PermissionEntity permissionEntity = permissionsRepository.findByPermissionName(rolesForm.getPermissionNames());
            getResultsForNonEmptyRolePermissionName(rolesForm, rolesSearchResults, pageable1, permissionEntity);

        } else if (StringUtils.isEmpty(rolesForm.getRoleName()) && StringUtils.isEmpty(rolesForm.getPermissionNames())) {
            Page<RoleEntity> rolesEntityListByPagination = rolesDetailsRepositorty.getRolesWithoutSuperAdmin(pageable);
            searchRolesEntity(rolesForm, rolesSearchResults, rolesEntityListByPagination);
        }
        return rolesForm;
    }

    /**
     * Save a new role entity in the scsb database.
     *
     * @param rolesForm the roles form
     * @param username  the username
     * @return the role entity
     */
    public RoleEntity saveNewRoleToDB(RolesForm rolesForm, String username) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleName(rolesForm.getNewRoleName().trim());
        roleEntity.setRoleDescription(rolesForm.getNewRoleDescription());
        roleEntity.setCreatedDate(new Date());
        roleEntity.setCreatedBy(username);
        roleEntity.setLastUpdatedDate(new Date());
        roleEntity.setLastUpdatedBy(username);
        List<String> permissionNameList = splitStringAndGetList(rolesForm.getNewPermissionNames());
        return saveRoleEntity(roleEntity, permissionNameList);
    }

    private RoleEntity saveRoleEntity(RoleEntity roleEntity1, List<String> permissionNameList) {
        PermissionEntity permissionEntity;
        RoleEntity roleEntity = null;
        Set rolesSet = new HashSet();
        try {
            for (String permissionName : permissionNameList) {
                permissionEntity = permissionsRepository.findByPermissionName(permissionName);
                rolesSet.add(permissionEntity);
            }
            roleEntity1.setPermissions(rolesSet);
            roleEntity = rolesDetailsRepositorty.save(roleEntity1);
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        return roleEntity;
    }

    private List<String> splitStringAndGetList(String inputString) {
        String[] splittedString = inputString.split(",");
        return Arrays.asList(splittedString);
    }

    private RolesForm getAllPermissionNames() {
        RolesForm rolesForm = new RolesForm();
        List<String> permissionNameList = new ArrayList<>();
        List<PermissionEntity> permissionEntityList = permissionsRepository.findAll();
        for (PermissionEntity permissionEntity : permissionEntityList) {
            String permissionName = permissionEntity.getPermissionName();
            permissionNameList.add(permissionName);
        }
        rolesForm.setPermissionNameList(permissionNameList);
        return rolesForm;
    }

    private void searchRolesEntity(RolesForm rolesForm, List<RolesSearchResult> rolesSearchResults, Page<RoleEntity> rolesEntityListByPagination) {
        List<RoleEntity> rolesEntity = rolesEntityListByPagination.getContent();
        rolesForm.setTotalRecordCount(NumberFormat.getNumberInstance().format(rolesEntityListByPagination.getTotalElements()));
        rolesForm.setTotalPageCount(rolesEntityListByPagination.getTotalPages());
        for (RoleEntity roleEntity : rolesEntity) {
            RolesSearchResult rolesSearchResult = getRolesSearchResult(roleEntity);
            rolesSearchResults.add(rolesSearchResult);
        }
        rolesForm.setRolesSearchResults(rolesSearchResults);

    }

    private RolesForm searchPage(RolesForm rolesForm) {
        rolesForm.setShowResults(true);
        return setRolesForm(rolesForm);
    }

    private RolesForm setRolesForm(RolesForm rolesForm) {
        findByPagination(rolesForm);
        return rolesForm;
    }
}
