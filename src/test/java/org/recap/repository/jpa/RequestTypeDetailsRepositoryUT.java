package org.recap.repository.jpa;

import org.junit.jupiter.api.Test;
import org.recap.BaseTestCase;
import org.recap.model.jpa.RequestTypeEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by dharmendrag on 7/2/17.
 */
public class RequestTypeDetailsRepositoryUT extends BaseTestCase {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void createRequestType(){

        RequestTypeEntity requestTypeEntity=new RequestTypeEntity();
        requestTypeEntity.setRequestTypeCode("BI");
        requestTypeEntity.setRequestTypeDesc("BORROW INDIRECT");

        RequestTypeEntity savedRequestType=requestTypeDetailsRepository.saveAndFlush(requestTypeEntity);
        entityManager.refresh(savedRequestType);

        assertNotNull(savedRequestType.getId());
        assertEquals(requestTypeEntity.getRequestTypeCode(),savedRequestType.getRequestTypeCode());
        assertEquals(requestTypeEntity.getRequestTypeDesc(),savedRequestType.getRequestTypeDesc());

    }
}
