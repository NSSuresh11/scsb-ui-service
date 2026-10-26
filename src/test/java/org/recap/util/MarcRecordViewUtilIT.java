package org.recap.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.recap.Main;
import org.recap.ScsbCommonConstants;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.HoldingsEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.search.BibliographicMarcForm;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.repository.jpa.BibliographicDetailsRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({SpringExtension.class})
@SpringBootTest(classes = Main.class)
public class MarcRecordViewUtilIT {

    @PersistenceContext
    private EntityManager entityManager;

    @InjectMocks
    MarcRecordViewUtil marcRecordViewUtil;

    @Mock
    public BibliographicDetailsRepository bibliographicDetailsRepository;


    @Test
    public void buildBibliographicMarcForm() throws Exception {
        try {
            BibliographicEntity bibliographicEntity = getBibEntityWithHoldingsAndItem();
            BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
            entityManager.refresh(savedBibliographicEntity);
            assertNotNull(savedBibliographicEntity);
            assertNotNull(savedBibliographicEntity.getHoldingsEntities());
            assertNotNull(savedBibliographicEntity.getItemEntities());

            Integer bibId = savedBibliographicEntity.getId();
            Integer itemId = savedBibliographicEntity.getItemEntities().get(0).getId();

            UserDetailsForm userDetailsForm = new UserDetailsForm();
            userDetailsForm.setSuperAdmin(false);
            userDetailsForm.setLoginInstitutionId(2);
            userDetailsForm.setRepositoryUser(false);

            BibliographicMarcForm bibliographicMarcForm = marcRecordViewUtil.buildBibliographicMarcForm(bibId, itemId, userDetailsForm);
            assertNotNull(bibliographicMarcForm);
            assertNotNull(bibliographicMarcForm.getBibId());
            assertNotNull(bibliographicMarcForm.getItemId());
            assertEquals(bibId, bibliographicMarcForm.getBibId());
            assertEquals(itemId, bibliographicMarcForm.getItemId());
            assertEquals("al-Ḥuṭayʼah : fī sīratihi wa-nafsīyatihi wa-shiʻrihi / bi-qalam Īlīyā Ḥāwī.", bibliographicMarcForm.getTitle());
            assertEquals("Ḥāwī, Īlīyā Salīm.   ", bibliographicMarcForm.getAuthor());
            assertEquals("Dār al-Thaqāfah,", bibliographicMarcForm.getPublisher());
            assertEquals("1970.", bibliographicMarcForm.getPublishedDate());
            assertEquals("PUL", bibliographicMarcForm.getOwningInstitution());
            assertEquals("010203", bibliographicMarcForm.getBarcode());
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public BibliographicEntity getBibEntityWithHoldingsAndItem() throws Exception {
        Random random = new Random();
        File bibContentFile = getBibContentFile();
        File holdingsContentFile = getHoldingsContentFile();
        String sourceBibContent = Files.readString(Paths.get(bibContentFile.toURI()));
        String sourceHoldingsContent = Files.readString(Paths.get(holdingsContentFile.toURI()));

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent(sourceBibContent.getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(1);
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random.nextInt()));
        bibliographicEntity.setDeleted(false);
        bibliographicEntity.setCatalogingStatus(ScsbCommonConstants.COMPLETE_STATUS);

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(sourceHoldingsContent.getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));
        holdingsEntity.setDeleted(false);

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId(String.valueOf(random.nextInt()));
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("010203");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("123");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity.setDeleted(false);
        itemEntity.setCatalogingStatus(ScsbCommonConstants.COMPLETE_STATUS);
        itemEntity.setImsLocationId(1);

        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));

        return bibliographicEntity;
    }

    public File getBibContentFile() throws URISyntaxException {
        URL resource = getClass().getResource("BibContent.xml");
        return new File(resource.toURI());
    }

    public File getHoldingsContentFile() throws URISyntaxException {
        URL resource = getClass().getResource("HoldingsContent.xml");
        return new File(resource.toURI());
    }

}
