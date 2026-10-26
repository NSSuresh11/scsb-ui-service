package org.recap.repository.jpa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.recap.BaseTestCase;
import org.recap.model.jpa.PermissionEntity;
import org.recap.model.jpa.RoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by hemalathas on 22/12/16.
 */
public class RolesDetailsRepositortyUT extends BaseTestCase {

    @Autowired
    RolesDetailsRepositorty rolesDetailsRepositorty;

    @Autowired
    PermissionsDetailsRepository permissionsRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    public void testByRoleName(){
        RoleEntity roleEntity = rolesDetailsRepositorty.findByRoleName("Search");
        assertNotNull(roleEntity);
        Integer roleId = roleEntity.getId();
        assertNotNull(roleId);
    }

   @Test
    public void createRole() {
       RoleEntity roleEntity = new RoleEntity();
       roleEntity.setRoleName("roleut");
       roleEntity.setRoleDescription("role desc test");
       roleEntity.setCreatedDate(new Date());
       roleEntity.setCreatedBy("test");
       roleEntity.setLastUpdatedDate(new Date());
       roleEntity.setLastUpdatedBy("test");

       PermissionEntity permissionEntity = new PermissionEntity();
       permissionEntity.setPermissionName("Create User");

       PermissionEntity permissionEntity1 = permissionsRepository.findByPermissionName(permissionEntity.getPermissionName());
       assertNotNull(permissionEntity1);

       Set<PermissionEntity> permissionEntities = new HashSet<>();
       permissionEntities.add(permissionEntity1);
       roleEntity.setPermissions(permissionEntities);

       RoleEntity savedRoleEntity = rolesDetailsRepositorty.save(roleEntity);
       assertEquals(roleEntity.getRoleName(),savedRoleEntity.getRoleName());
       assertEquals(roleEntity.getRoleDescription(),savedRoleEntity.getRoleDescription());
   }

    @Test
    public void editRole() {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleName("role ut");
        roleEntity.setRoleDescription("role desc test");
        roleEntity.setCreatedDate(new Date());
        roleEntity.setCreatedBy("test");
        roleEntity.setLastUpdatedDate(new Date());
        roleEntity.setLastUpdatedBy("test");

        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setPermissionName("Create User");

        PermissionEntity permissionEntity1 = permissionsRepository.findByPermissionName(permissionEntity.getPermissionName());
        assertNotNull(permissionEntity1);

        Set<PermissionEntity> permissionEntities = new HashSet<>();
        permissionEntities.add(permissionEntity1);
        roleEntity.setPermissions(permissionEntities);

        RoleEntity savedRoleEntity = rolesDetailsRepositorty.save(roleEntity);

        RoleEntity editRoleEntity = new RoleEntity();
        editRoleEntity.setId(savedRoleEntity.getId());
        roleEntity.setRoleName("edit ut");
        editRoleEntity.setRoleDescription("edit ut");

        PermissionEntity editPermissionEntity = new PermissionEntity();
        editPermissionEntity.setPermissionName("WriteCGD");

        Set<PermissionEntity> editpermissionEntities = new HashSet<>();
        editpermissionEntities.add(permissionEntity1);
        roleEntity.setPermissions(editpermissionEntities);
        editRoleEntity.setPermissions(editpermissionEntities);

        RoleEntity editSaveRoleEntity = rolesDetailsRepositorty.save(editRoleEntity);
        assertEquals(editRoleEntity.getRoleName(),editSaveRoleEntity.getRoleName());
        assertEquals(editRoleEntity.getRoleDescription(),editSaveRoleEntity.getRoleDescription());

    }

    @Test
    public void deleteRole(){
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleName("role ut");
        roleEntity.setRoleDescription("role desc test");
        roleEntity.setCreatedDate(new Date());
        roleEntity.setCreatedBy("test");
        roleEntity.setLastUpdatedDate(new Date());
        roleEntity.setLastUpdatedBy("test");

        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setPermissionName("CreateUser");

        Set<PermissionEntity> permissionEntities = new HashSet<>();
        permissionEntities.add(permissionEntity);
        roleEntity.setPermissions(permissionEntities);

        RoleEntity savedRoleEntity = rolesDetailsRepositorty.save(roleEntity);

        rolesDetailsRepositorty.deleteById(savedRoleEntity.getId());

    }

    @Test
    public void displayRoleDetailsWithoutSuperAdmin(){
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleName("role ut");
        roleEntity.setRoleDescription("role desc test");
        roleEntity.setCreatedDate(new Date());
        roleEntity.setCreatedBy("test");
        roleEntity.setLastUpdatedDate(new Date());
        roleEntity.setLastUpdatedBy("test");

        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setPermissionName("SuperAdmin");

        Set<PermissionEntity> permissionEntities = new HashSet<>();
        permissionEntities.add(permissionEntity);
        roleEntity.setPermissions(permissionEntities);

        RoleEntity savedRoleEntity = rolesDetailsRepositorty.save(roleEntity);

        Pageable pageable = PageRequest.of(0,1);
        Page<RoleEntity> rolesWithoutSuperAdmin = rolesDetailsRepositorty.getRolesWithoutSuperAdmin(pageable);
        List<RoleEntity> roleContent = rolesWithoutSuperAdmin.getContent();
        for (RoleEntity entity : roleContent) {
            Assertions.assertNotEquals("SuperAdmin", entity.getRoleName());
        }
    }

    @Test
    public void testFindByRoleID(){
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleName("role ut");
        roleEntity.setRoleDescription("role desc test");
        roleEntity.setCreatedDate(new Date());
        roleEntity.setCreatedBy("test");
        roleEntity.setLastUpdatedDate(new Date());
        roleEntity.setLastUpdatedBy("test");

        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setPermissionName("SuperAdmin");

        Set<PermissionEntity> permissionEntities = new HashSet<>();
        permissionEntities.add(permissionEntity);
        roleEntity.setPermissions(permissionEntities);

        RoleEntity savedRoleEntity = rolesDetailsRepositorty.save(roleEntity);
        assertEquals(roleEntity.getRoleName(),savedRoleEntity.getRoleName());
        assertEquals(roleEntity.getRoleDescription(),savedRoleEntity.getRoleDescription());
        Set<PermissionEntity> permissionList = savedRoleEntity.getPermissions();
        for(PermissionEntity permission : permissionList){
            assertEquals(permissionEntity.getPermissionName(),permission.getPermissionName());
        }

    }


}