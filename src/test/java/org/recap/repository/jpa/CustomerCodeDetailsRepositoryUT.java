package org.recap.repository.jpa;

import org.junit.jupiter.api.Test;
import org.recap.BaseTestCase;
import org.recap.model.jpa.OwnerCodeEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by rajeshbabuk on 18/10/16.
 */
public class CustomerCodeDetailsRepositoryUT extends BaseTestCase {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void findByOwnerCode() throws Exception {
        OwnerCodeEntity customerCodeEntityfindByOwnerCodeIn = getOwnerCodeEntity("ZZ", "Desc ZZ", 3, "ZZ,YY");
        OwnerCodeEntity saveCustomerCodeEntity = ownerCodeDetailsRepository.saveAndFlush(customerCodeEntityfindByOwnerCodeIn);
        entityManager.refresh(saveCustomerCodeEntity);
        assertNotNull(saveCustomerCodeEntity);
        assertNotNull(saveCustomerCodeEntity.getId());
        assertNotNull(saveCustomerCodeEntity.getOwnerCode());

        OwnerCodeEntity byCustomerCode = ownerCodeDetailsRepository.findByOwnerCode(saveCustomerCodeEntity.getOwnerCode());
        assertNotNull(byCustomerCode);
        assertNotNull(byCustomerCode.getOwnerCode());
        assertEquals("ZZ", byCustomerCode.getOwnerCode());
        assertNotNull(byCustomerCode.getInstitutionEntity());
    }

    @Test
    public void findByOwnerCodeIn() throws Exception {
        OwnerCodeEntity customerCodeEntity1 = getOwnerCodeEntity("ZZ", "Desc ZZ", 3, "ZZ,YY");
        OwnerCodeEntity saveCustomerCodeEntity1 = ownerCodeDetailsRepository.saveAndFlush(customerCodeEntity1);
        entityManager.refresh(saveCustomerCodeEntity1);
        assertNotNull(saveCustomerCodeEntity1);
        assertNotNull(saveCustomerCodeEntity1.getId());
        assertNotNull(saveCustomerCodeEntity1.getOwnerCode());

        OwnerCodeEntity customerCodeEntity2 = getOwnerCodeEntity("YY", "Desc YY", 3, "ZZ,YY");
        OwnerCodeEntity saveCustomerCodeEntity2 = ownerCodeDetailsRepository.saveAndFlush(customerCodeEntity2);
        entityManager.refresh(saveCustomerCodeEntity2);
        assertNotNull(saveCustomerCodeEntity2);
        assertNotNull(saveCustomerCodeEntity2.getId());
        assertNotNull(saveCustomerCodeEntity2.getOwnerCode());

        List<OwnerCodeEntity> byCustomerCodeIn = ownerCodeDetailsRepository.findByOwnerCodeIn(Arrays.asList("ZZ", "YY"));
        assertNotNull(byCustomerCodeIn);
        assertEquals(2, byCustomerCodeIn.size());
        assertNotNull(byCustomerCodeIn.get(0));
        assertNotNull(byCustomerCodeIn.get(1));
        for(OwnerCodeEntity customerCodeEntity:byCustomerCodeIn){
            if(customerCodeEntity.getOwnerCode().equals("ZZ")){
                assertEquals("ZZ", customerCodeEntity.getOwnerCode());
            } else {
                assertEquals("YY", customerCodeEntity.getOwnerCode());
            }
        }
    }

    private OwnerCodeEntity getOwnerCodeEntity(String customerCode, String description, Integer institutionId, String deliveryRestrictions) {
        OwnerCodeEntity customerCodeEntity = new OwnerCodeEntity();
        customerCodeEntity.setOwnerCode(customerCode);
        customerCodeEntity.setDescription(description);
        customerCodeEntity.setInstitutionId(institutionId);
        return customerCodeEntity;
    }
}
