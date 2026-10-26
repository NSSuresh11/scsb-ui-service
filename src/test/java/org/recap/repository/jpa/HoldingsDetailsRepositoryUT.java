package org.recap.repository.jpa;

import org.junit.jupiter.api.Test;
import org.recap.BaseTestCase;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by dharmendrag on 6/2/17.
 */
public class HoldingsDetailsRepositoryUT extends BaseTestCase {

    Random random = new Random();

    @Autowired
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    HoldingsDetailsRepository holdingsDetailsRepository;

    @Autowired
    ItemDetailsRepository itemDetailsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void findById(){
        String owningInstitutionBibId = String.valueOf(random.nextInt());
        int owningInstitutionId = 1;

        HoldingsEntity holdingsEntity=new HoldingsEntity();
        InstitutionEntity institutionEntity=new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionName("Princeton");
        institutionEntity.setInstitutionCode("PUL");
        institutionEntity.setId(1);
        institutionEntity.setIlsProtocol("REST");


        holdingsEntity.setContent("mock holdings".getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setCreatedBy("etl");
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setLastUpdatedBy("etl");
        holdingsEntity.setOwningInstitutionId(owningInstitutionId);
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));
        holdingsEntity.setOwningInstitutionId(1);


        setBibliographicEntity(owningInstitutionBibId,owningInstitutionId,holdingsEntity);

        setItemEntity(owningInstitutionBibId,owningInstitutionId,holdingsEntity);

        HoldingsEntity savedHoldings=holdingsDetailsRepository.saveAndFlush(holdingsEntity);
        entityManager.refresh(savedHoldings);

        assertNotNull(savedHoldings.getId());
        assertEquals(holdingsEntity.getContent().length,savedHoldings.getContent().length);
        assertEquals(holdingsEntity.getLastUpdatedBy(),savedHoldings.getLastUpdatedBy());

    }

    private void setBibliographicEntity(String owningInstitutionBibId,int owningInstitutionId,HoldingsEntity holdingsEntity){
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("Mock Bib Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setCreatedBy("etl");
        bibliographicEntity.setLastUpdatedBy("etl");
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        bibliographicEntity.setDeleted(false);

        BibliographicEntity savedBibliographicEntity=bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);

        holdingsEntity.setBibliographicEntities(Arrays.asList(savedBibliographicEntity));
    }

    private void setItemEntity(String owningInstitutionBibId,int owningInstitutionId,HoldingsEntity holdingsEntity){

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setCallNumberType("0");
        itemEntity.setCallNumber("callNum");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("etl");
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setLastUpdatedBy("etl");
        itemEntity.setBarcode("1231");
        itemEntity.setOwningInstitutionItemId(".i1231");
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCustomerCode("PA");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setImsLocationId(1);

        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));

    }
}
