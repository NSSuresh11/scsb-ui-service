package org.recap.model.jpa;


import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.recap.BaseTestCase;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by pvsubrah on 6/20/16.
 */
public class BibliographicEntityUT extends BaseTestCase {

    @Autowired
    BibliographicDetailsRepository bibliographicDetailsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void findByInstitutionId() throws Exception {
        Long beforeSaveCount = bibliographicDetailsRepository.countByOwningInstitutionIdAndIsDeletedFalse(3);
        assertNotNull(beforeSaveCount);
        Random random = new Random();
        Date today = new Date();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("Mock Bib Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random));
        bibliographicEntity.setOwningInstitutionId(3);
        BibliographicEntity entity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(entity);
        assertNotNull(entity);

        String fetchedCreatedDate = df.format(entity.getCreatedDate());
        String fetchedLastUpdatedDate = df.format(entity.getLastUpdatedDate());
        String todayStringDate = df.format(today);

        assertEquals(new String(entity.getContent()), "Mock Bib Content");
        assertEquals(entity.getOwningInstitutionId().toString(), "3");
        assertEquals(fetchedCreatedDate,todayStringDate);
        assertEquals(entity.getCreatedBy(),"tst");
        assertEquals(fetchedLastUpdatedDate,todayStringDate);
        assertEquals(entity.getLastUpdatedBy(),"tst");
        assertEquals(entity.getInstitutionEntity().getInstitutionCode(),"NYPL");

        System.out.println("owning institution bibId-->" + entity.getOwningInstitutionBibId());
        Long afterSave = bibliographicDetailsRepository.countByOwningInstitutionIdAndIsDeletedFalse(3);
        assertTrue((beforeSaveCount + 1) == afterSave);
        bibliographicDetailsRepository.delete(entity);

    }

    @Test
    public void findByInstitutionIdPagable() throws Exception {
        Long beforeSaveCount = bibliographicDetailsRepository.countByOwningInstitutionIdAndIsDeletedFalse(3);
        assertNotNull(beforeSaveCount);
        if (beforeSaveCount == 0 || beforeSaveCount <= 3) {
            Random random = new Random();
            BibliographicEntity bibliographicEntity = new BibliographicEntity();
            bibliographicEntity.setContent("Mock Bib Content1".getBytes());
            bibliographicEntity.setCreatedDate(new Date());
            bibliographicEntity.setLastUpdatedDate(new Date());
            bibliographicEntity.setCreatedBy("tst");
            bibliographicEntity.setLastUpdatedBy("tst");
            bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random));
            bibliographicEntity.setOwningInstitutionId(3);
            BibliographicEntity entity1 = bibliographicDetailsRepository.save(bibliographicEntity);
            assertNotNull(entity1);

            Random random1 = new Random();
            BibliographicEntity bibliographicEntity1 = new BibliographicEntity();
            bibliographicEntity1.setContent("Mock Bib Content2".getBytes());
            bibliographicEntity1.setCreatedDate(new Date());
            bibliographicEntity1.setLastUpdatedDate(new Date());
            bibliographicEntity1.setCreatedBy("tst");
            bibliographicEntity1.setLastUpdatedBy("tst");
            bibliographicEntity1.setOwningInstitutionBibId(String.valueOf(random1));
            bibliographicEntity1.setOwningInstitutionId(3);
            BibliographicEntity entity2 = bibliographicDetailsRepository.save(bibliographicEntity1);
            assertNotNull(entity2);

            Random random2 = new Random();
            BibliographicEntity bibliographicEntity2 = new BibliographicEntity();
            bibliographicEntity2.setContent("Mock Bib Content3".getBytes());
            bibliographicEntity2.setCreatedDate(new Date());
            bibliographicEntity2.setLastUpdatedDate(new Date());
            bibliographicEntity2.setCreatedBy("tst");
            bibliographicEntity2.setLastUpdatedBy("tst");
            bibliographicEntity2.setOwningInstitutionBibId(String.valueOf(random2));
            bibliographicEntity2.setOwningInstitutionId(3);
            BibliographicEntity entity3 = bibliographicDetailsRepository.save(bibliographicEntity2);
            assertNotNull(entity3);

            Page<BibliographicEntity> byInstitutionId = bibliographicDetailsRepository.findByOwningInstitutionIdAndIsDeletedFalse(PageRequest.of(0, 3), 3);
            assertNotNull(byInstitutionId);
            assertTrue(byInstitutionId.getContent().size() == 3);
            bibliographicDetailsRepository.delete(bibliographicEntity);
            bibliographicDetailsRepository.delete(bibliographicEntity1);
            bibliographicDetailsRepository.delete(bibliographicEntity2);
        } else {
            Page<BibliographicEntity> byInstitutionId = bibliographicDetailsRepository.findByOwningInstitutionIdAndIsDeletedFalse(PageRequest.of(0, 3), 3);
            assertNotNull(byInstitutionId);
            assertTrue(byInstitutionId.getContent().size() == 3);
        }
    }

    @Test
    public void findByOwningInstitutionIdAndOwningInstitutionBibId() throws Exception {
        Random random = new Random();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("Mock Bib Content".getBytes());
        bibliographicEntity.setOwningInstitutionId(1);
        String owningInstitutionBibId = String.valueOf(random.nextInt());
        bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);
        assertNotNull(savedBibliographicEntity);

        BibliographicEntity fetchedBibliographicEntity = bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(1, owningInstitutionBibId);
        assertNotNull(fetchedBibliographicEntity);
        assertNotNull(fetchedBibliographicEntity.getId());
        assertNotNull(fetchedBibliographicEntity.getOwningInstitutionId());
        assertNotNull(fetchedBibliographicEntity.getOwningInstitutionBibId());
        assertEquals(String.valueOf(1), String.valueOf(fetchedBibliographicEntity.getOwningInstitutionId()));
        assertEquals(owningInstitutionBibId, fetchedBibliographicEntity.getOwningInstitutionBibId());
    }

    @Test
    public void saveBibSingleHoldings() throws Exception {
        Random random = new Random();
        Date today = new Date();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("mock Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random.nextInt()));



        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent("mock holdings".getBytes());
        holdingsEntity.setCreatedDate(today);
        holdingsEntity.setLastUpdatedDate(today);
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));

        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);
        assertNotNull(savedBibliographicEntity);
        assertNotNull(savedBibliographicEntity.getHoldingsEntities().get(0).getId());
        assertEquals(new String(savedBibliographicEntity.getHoldingsEntities().get(0).getContent()),"mock holdings");
        String fetchedCreatedDate = df.format(savedBibliographicEntity.getHoldingsEntities().get(0).getCreatedDate());
        String fetchedLastUpdatedDate = df.format(savedBibliographicEntity.getHoldingsEntities().get(0).getLastUpdatedDate());
        String todayStringDate = df.format(today);
        assertEquals(fetchedCreatedDate,todayStringDate);
        assertEquals(fetchedLastUpdatedDate,todayStringDate);
    }

    @Test
    public void saveBibMultipleHoldings() throws Exception {
        Random random = new Random();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("mock Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setCatalogingStatus("Complete");
        bibliographicEntity.setDeleted(false);
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random.nextInt()));


        HoldingsEntity holdingsEntity1 = new HoldingsEntity();
        holdingsEntity1.setContent("mock holdings".getBytes());
        holdingsEntity1.setCreatedDate(new Date());
        holdingsEntity1.setLastUpdatedDate(new Date());
        holdingsEntity1.setOwningInstitutionId(1);
        holdingsEntity1.setCreatedBy("tst");
        holdingsEntity1.setLastUpdatedBy("tst");
        holdingsEntity1.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));


        HoldingsEntity holdingsEntity2 = new HoldingsEntity();
        holdingsEntity2.setContent("mock holdings".getBytes());
        holdingsEntity2.setCreatedDate(new Date());
        holdingsEntity2.setLastUpdatedDate(new Date());
        holdingsEntity2.setCreatedBy("tst");
        holdingsEntity2.setOwningInstitutionId(1);
        holdingsEntity2.setLastUpdatedBy("tst");
        holdingsEntity2.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));

        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity1, holdingsEntity2));

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);
        assertNotNull(savedBibliographicEntity);
        assertEquals(savedBibliographicEntity.getCatalogingStatus(),"Complete");
        assertTrue(!savedBibliographicEntity.isDeleted());
        assertNotNull(savedBibliographicEntity.getHoldingsEntities().get(0).getId());
        assertNotNull(savedBibliographicEntity.getHoldingsEntities().get(1).getId());
    }

    @Test
    public void saveMultipleBibSingleHoldings() throws Exception {
        Random random = new Random();
        BibliographicEntity bibliographicEntity1 = new BibliographicEntity();
        bibliographicEntity1.setContent("mock Content".getBytes());
        bibliographicEntity1.setCreatedDate(new Date());
        bibliographicEntity1.setLastUpdatedDate(new Date());
        bibliographicEntity1.setCreatedBy("tst");
        bibliographicEntity1.setLastUpdatedBy("tst");
        bibliographicEntity1.setOwningInstitutionId(1);
        bibliographicEntity1.setOwningInstitutionBibId(String.valueOf(random.nextInt()));
        bibliographicEntity1.setDeleted(false);

        BibliographicEntity bibliographicEntity2 = new BibliographicEntity();
        bibliographicEntity2.setContent("mock Content".getBytes());
        bibliographicEntity2.setCreatedDate(new Date());
        bibliographicEntity2.setLastUpdatedDate(new Date());
        bibliographicEntity2.setCreatedBy("tst");
        bibliographicEntity2.setLastUpdatedBy("tst");
        bibliographicEntity2.setOwningInstitutionId(1);
        bibliographicEntity2.setOwningInstitutionBibId(String.valueOf(random.nextInt()));
        bibliographicEntity2.setDeleted(false);

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent("mock holdings".getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));
        holdingsEntity.setDeleted(false);
        holdingsEntity.setOwningInstitutionId(1);


        bibliographicEntity1.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity2.setHoldingsEntities(Arrays.asList(holdingsEntity));

        Iterable<BibliographicEntity> savedBibliographicEntities = bibliographicDetailsRepository.saveAll(Arrays.asList(bibliographicEntity1, bibliographicEntity2));
        assertNotNull(savedBibliographicEntities);
    }

    @Test
    public void saveBibSingleHoldingsSingleItem() throws Exception {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setInstitutionCode("UC");
        institutionEntity.setInstitutionName("University of Chicago");
        institutionEntity.setIlsProtocol("REST");
        institutionEntity.setId(1);
        InstitutionEntity entity = institutionDetailRepository.save(institutionEntity);
        assertNotNull(entity);

        Random random = new Random();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("mock Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(entity.getId());
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random.nextInt()));
        bibliographicEntity.setDeleted(false);

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent("mock holdings".getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId(String.valueOf(random.nextInt()));
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("0036");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("123");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setImsLocationId(1);
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));

        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);
        assertNotNull(savedBibliographicEntity);
        assertNotNull(savedBibliographicEntity.getHoldingsEntities());
        assertNotNull(savedBibliographicEntity.getItemEntities());

    }

    @Test
    public void saveBibSingleHoldingsMultipleItem() throws Exception {
        Random random = new Random();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("mock Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random.nextInt()));
        bibliographicEntity.setDeleted(false);


        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent("mock holdings".getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));
        holdingsEntity.setOwningInstitutionId(1);

        ItemEntity itemEntity1 = new ItemEntity();
        itemEntity1.setLastUpdatedDate(new Date());
        itemEntity1.setCreatedDate(new Date());
        itemEntity1.setCreatedBy("tst");
        itemEntity1.setLastUpdatedBy("tst");
        itemEntity1.setCustomerCode("1");
        itemEntity1.setItemAvailabilityStatusId(1);
        itemEntity1.setOwningInstitutionItemId(String.valueOf(random.nextInt()));
        itemEntity1.setOwningInstitutionId(1);
        itemEntity1.setBarcode("123");
        itemEntity1.setCallNumber("x.12321");
        itemEntity1.setCollectionGroupId(1);
        itemEntity1.setCallNumberType("1");
        itemEntity1.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity1.setImsLocationId(1);


        ItemEntity itemEntity2 = new ItemEntity();
        itemEntity2.setLastUpdatedDate(new Date());
        itemEntity2.setCreatedDate(new Date());
        itemEntity2.setCreatedBy("tst");
        itemEntity2.setLastUpdatedBy("tst");
        itemEntity2.setItemAvailabilityStatusId(1);
        itemEntity2.setOwningInstitutionItemId(String.valueOf(random.nextInt()));
        itemEntity2.setOwningInstitutionId(1);
        itemEntity2.setBarcode("1234");
        itemEntity2.setCallNumber("x.12321");
        itemEntity2.setCollectionGroupId(1);
        itemEntity2.setCallNumberType("1");
        itemEntity2.setCustomerCode("123");
        itemEntity2.setImsLocationId(1);
        itemEntity2.setHoldingsEntities(Arrays.asList(holdingsEntity));

        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity1, itemEntity2));


        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity1, itemEntity2));

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.save(bibliographicEntity);
        assertNotNull(savedBibliographicEntity);
    }

    @Test
    public void fetchUnicodeBibRecord() throws Exception {
        Random random = new Random();
        File bibContentFile = getUnicodeContentFile();
        String sourceBibContent = FileUtils.readFileToString(bibContentFile, "UTF-8");

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent(sourceBibContent.getBytes("UTF-8"));
        bibliographicEntity.setOwningInstitutionId(1);
        String owningInstitutionBibId = String.valueOf(random.nextInt());
        bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.save(bibliographicEntity);
        assertNotNull(savedBibliographicEntity);

        BibliographicEntity fetchedBibliographicEntity = bibliographicDetailsRepository.findByOwningInstitutionIdAndOwningInstitutionBibIdAndIsDeletedFalse(1, owningInstitutionBibId);
        assertNotNull(fetchedBibliographicEntity);
        assertNotNull(fetchedBibliographicEntity.getContent());

        String fetchedBibContent = new String(fetchedBibliographicEntity.getContent(), Charset.forName("UTF-8"));
        assertEquals(sourceBibContent, fetchedBibContent);
    }

    public File getUnicodeContentFile() throws URISyntaxException {
        URL resource = getClass().getResource("UnicodeBibContent.xml");
        return new File(resource.toURI());
    }

/*
    @Test
    public void testBibliographicPK(){
        BibliographicPK bibliographicPK = new BibliographicPK();
        BibliographicPK bibliographicPK1 = new BibliographicPK(1,".b000213654");
        bibliographicPK.setOwningInstitutionBibId(".b000213654");
        bibliographicPK.setOwningInstitutionId(1);
        assertNotNull(bibliographicPK.getOwningInstitutionId());
        assertNotNull(bibliographicPK.getOwningInstitutionBibId());
    }

    @Test
    public void testHoldingsPK(){
        HoldingsPK holdingsPK = new HoldingsPK();
        HoldingsPK holdingsPK1 = new HoldingsPK(1,".b0000000001");
        holdingsPK.setOwningInstitutionHoldingsId(".b0000000001");
        holdingsPK.setOwningInstitutionId(1);
        assertNotNull(holdingsPK.getOwningInstitutionHoldingsId());
        assertNotNull(holdingsPK.getOwningInstitutionId());
    }

    @Test
    public void testItemPK(){
        ItemPK itemPK = new ItemPK();
        itemPK.setOwningInstitutionId(1);
        itemPK.setOwningInstitutionItemId(".b02314364");
        ItemPK itemPK1 = new ItemPK(1,".b02314364");
        assertNotNull(itemPK.getOwningInstitutionId());
        assertNotNull(itemPK.getOwningInstitutionItemId());
    }
*/

}
