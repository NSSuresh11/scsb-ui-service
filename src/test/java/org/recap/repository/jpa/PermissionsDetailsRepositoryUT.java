package org.recap.repository.jpa;

import org.junit.jupiter.api.Test;
import org.recap.BaseTestCase;
import org.recap.model.jpa.PermissionEntity;
import org.recap.model.jpa.RoleEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by dharmendrag on 7/2/17.
 */
public class PermissionsDetailsRepositoryUT extends BaseTestCase {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void createPermissionRepository(){
        PermissionEntity permissionEntity=new PermissionEntity();
        permissionEntity.setPermissionName("EditRoles");
        permissionEntity.setPermissionDesc("Permission to Edit Roles");
        getRoleEntity(permissionEntity);

        PermissionEntity savedPermission=permissionsRepository.saveAndFlush(permissionEntity);
        entityManager.refresh(savedPermission);

        assertNotNull(savedPermission.getId());
        assertEquals(permissionEntity.getPermissionName(),savedPermission.getPermissionName());
        assertEquals(permissionEntity.getPermissionDesc(),savedPermission.getPermissionDesc());
        assertEquals(permissionEntity.getRoleEntityList(),savedPermission.getRoleEntityList());

        PermissionEntity findByName=permissionsRepository.findByPermissionName(permissionEntity.getPermissionName());
        assertEquals(savedPermission.getId(),findByName.getId());

    }

    private void getRoleEntity(PermissionEntity permissionEntity){
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleName("CUL Admin");
        roleEntity.setRoleDescription("Admin for CUL Institution");
        roleEntity.setCreatedDate(new Date());
        roleEntity.setCreatedBy("superadmin");
        roleEntity.setLastUpdatedDate(new Date());
        roleEntity.setLastUpdatedBy("superadmin");

        RoleEntity savedRole=roleRepository.saveAndFlush(roleEntity);
        entityManager.refresh(savedRole);

        permissionEntity.setRoleEntityList(Arrays.asList(savedRole));
    }

}
