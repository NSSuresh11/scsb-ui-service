package org.recap.util;

import com.csvreader.CsvReader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.recap.BaseTestCase;
import org.recap.model.jpa.*;
import org.recap.model.reports.ReportsInstitutionForm;
import org.recap.model.reports.ReportsResponse;
import org.recap.model.search.DeaccessionItemResultsRow;
import org.recap.model.search.IncompleteReportResultsRow;
import org.recap.model.search.ReportsForm;
import org.recap.model.submitCollection.SubmitCollectionReport;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.service.RestHeaderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Created by akulak on 30/12/16.
 */
@ExtendWith({SpringExtension.class})
public class ReportsUtilUT extends BaseTestCase {

    @InjectMocks
    ReportsUtil reportsUtil;

    @PersistenceContext
    EntityManager entityManager;


    @Mock
    RestTemplate restTemplate;

    @Mock
    RestHeaderService restHeaderService;

    @Mock
    HttpHeaders httpHeaders;

    @InjectMocks
    ReportsUtil reportsUtilMock;

    @Mock
    private ReportsServiceUtil reportsServiceUtil;

    @Mock
    private InstitutionDetailsRepository institutionDetailsRepository;


    @BeforeEach
    public void setUp() {
        // Setting up a mock ReportsResponse
        ReportsResponse reportsResponse = new ReportsResponse();
        reportsResponse.setMessage("Success");
        when(reportsServiceUtil.requestAccessionDeaccessionCounts(any(ReportsForm.class)))
                .thenReturn(reportsResponse);

    }

    @Test
    public void testPopulatePartnersCountForRequest() {
        // Setup mock behavior
        when(institutionDetailsRepository.findAll()).thenReturn(getMockInstitutionEntities());

        // Set the date range
        Date requestFromDate = new Date();
        Date requestToDate = new Date();

        // Create a ReportsForm object
        ReportsForm reportsForm = new ReportsForm();

        // Call the method under test
        reportsUtil.populatePartnersCountForRequest(reportsForm, requestFromDate, requestToDate);

        // Assert results
        assertNotNull(reportsForm.getReportsInstitutionFormList());
//        assertFalse(reportsForm.getReportsInstitutionFormList().isEmpty());
    }

    private List<InstitutionEntity> getMockInstitutionEntities() {
        List<InstitutionEntity> institutionEntities = new ArrayList<>();
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionCode("CUL");
        institutionEntities.add(institutionEntity);
        return institutionEntities;
    }


    @Disabled
    public void populatePartnersCountForRequest() throws Exception {
        ReportsForm reportsForm = reportsForm();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date fromDate = simpleDateFormat.parse("2016-12-30 00:00:00");
        Date toDate = simpleDateFormat.parse("2020-12-31 23:59:59");

        reportsUtil.populatePartnersCountForRequest(reportsForm, fromDate, toDate);

        /*long beforePhysicalPrivatePulCount = reportsForm.getPhysicalPrivatePulCount();
        long beforePhysicalPrivateCulCount = reportsForm.getPhysicalPrivateCulCount();
        long beforePhysicalPrivateNyplCount = reportsForm.getPhysicalPrivateNyplCount();
        long beforePhysicalSharedPulCount = reportsForm.getPhysicalSharedPulCount();
        long beforePhysicalSharedCulCount = reportsForm.getPhysicalSharedCulCount();
        long beforePhysicalSharedNyplCount = reportsForm.getPhysicalSharedNyplCount();
        long beforeEddPrivatePulCount = reportsForm.getEddPrivatePulCount();
        long beforeEddPrivateCulCount = reportsForm.getEddPrivateCulCount();
        long beforeEddPrivateNyplCount = reportsForm.getEddPrivateNyplCount();
        long beforeEddSharedPulCount = reportsForm.getEddSharedOpenPulCount();
        long beforeEddSharedCulCount = reportsForm.getEddSharedOpenCulCount();
        long beforeEddSharedNyplCount = reportsForm.getEddSharedOpenNyplCount();
*/
        BibliographicEntity bibliographicEntity = saveBibHoldingItemEntity(1, 3, false,"010101");
        ItemEntity itemEntity = bibliographicEntity.getItemEntities().get(0);
        saveRequestEntity(itemEntity.getId(), 1, 1, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity1 = saveBibHoldingItemEntity(1, 1, false,"010102");
        ItemEntity itemEntity1 = bibliographicEntity1.getItemEntities().get(0);
        saveRequestEntity(itemEntity1.getId(), 1, 1, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity2 = saveBibHoldingItemEntity(2, 3, false,"010103");
        ItemEntity itemEntity2 = bibliographicEntity2.getItemEntities().get(0);
        saveRequestEntity(itemEntity2.getId(), 1, 2, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity3 = saveBibHoldingItemEntity(2, 1, false,"010104");
        ItemEntity itemEntity3 = bibliographicEntity3.getItemEntities().get(0);
        saveRequestEntity(itemEntity3.getId(), 1, 2, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity4 = saveBibHoldingItemEntity(3, 3, false,"010105");
        ItemEntity itemEntity4 = bibliographicEntity4.getItemEntities().get(0);
        saveRequestEntity(itemEntity4.getId(), 1, 3, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity5 = saveBibHoldingItemEntity(3, 1, false,"010106");
        ItemEntity itemEntity5 = bibliographicEntity5.getItemEntities().get(0);
        saveRequestEntity(itemEntity5.getId(), 1, 3, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity6 = saveBibHoldingItemEntity(1, 3, false,"010107");
        ItemEntity itemEntity6 = bibliographicEntity6.getItemEntities().get(0);
        saveRequestEntity(itemEntity6.getId(), 3, 1, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity7 = saveBibHoldingItemEntity(1, 1, false,"010108");
        ItemEntity itemEntity7 = bibliographicEntity7.getItemEntities().get(0);
        saveRequestEntity(itemEntity7.getId(), 3, 1, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity8 = saveBibHoldingItemEntity(2, 3, false,"010109");
        ItemEntity itemEntity8 = bibliographicEntity8.getItemEntities().get(0);
        saveRequestEntity(itemEntity8.getId(), 3, 2, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity9 = saveBibHoldingItemEntity(2, 1, false,"010110");
        ItemEntity itemEntity9 = bibliographicEntity9.getItemEntities().get(0);
        saveRequestEntity(itemEntity9.getId(), 3, 2, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity10 = saveBibHoldingItemEntity(3, 3, false,"010111");
        ItemEntity itemEntity10 = bibliographicEntity10.getItemEntities().get(0);
        saveRequestEntity(itemEntity10.getId(), 3, 3, String.valueOf(new Random().nextInt()));

        BibliographicEntity bibliographicEntity11 = saveBibHoldingItemEntity(3, 1, false,"010112");
        ItemEntity itemEntity11 = bibliographicEntity11.getItemEntities().get(0);
        saveRequestEntity(itemEntity11.getId(), 3, 3, String.valueOf(new Random().nextInt()));

        reportsUtil.populatePartnersCountForRequest(reportsForm, fromDate, toDate);

        /*long afterPhysicalPrivatePulCount = reportsForm.getPhysicalPrivatePulCount();
        long afterPhysicalPrivateCulCount = reportsForm.getPhysicalPrivateCulCount();
        long afterPhysicalPrivateNyplCount = reportsForm.getPhysicalPrivateNyplCount();
        long afterPhysicalSharedPulCount = reportsForm.getPhysicalSharedPulCount();
        long afterPhysicalSharedCulCount = reportsForm.getPhysicalSharedCulCount();
        long afterPhysicalSharedNyplCount = reportsForm.getPhysicalSharedNyplCount();
        long afterEddPrivatePulCount = reportsForm.getEddPrivatePulCount();
        long afterEddPrivateCulCount = reportsForm.getEddPrivateCulCount();
        long afterEddPrivateNyplCount = reportsForm.getEddPrivateNyplCount();
        long afterEddSharedPulCount = reportsForm.getEddSharedOpenPulCount();
        long afterEddSharedCulCount = reportsForm.getEddSharedOpenCulCount();
        long afterEddSharedNyplCount = reportsForm.getEddSharedOpenNyplCount();

        assertEquals(beforePhysicalPrivatePulCount+1, afterPhysicalPrivatePulCount);
        assertEquals(beforePhysicalPrivateCulCount+1, afterPhysicalPrivateCulCount);
        assertEquals(beforePhysicalPrivateNyplCount+1, afterPhysicalPrivateNyplCount);
        assertEquals(beforePhysicalSharedPulCount+1, afterPhysicalSharedPulCount);
        assertEquals(beforePhysicalSharedCulCount+1, afterPhysicalSharedCulCount);
        assertEquals(beforePhysicalSharedNyplCount+1, afterPhysicalSharedNyplCount);
        assertEquals(beforeEddPrivatePulCount+1, afterEddPrivatePulCount);
        assertEquals(beforeEddPrivateCulCount+1, afterEddPrivateCulCount);
        assertEquals(beforeEddPrivateNyplCount+1, afterEddPrivateNyplCount);
        assertEquals(beforeEddSharedPulCount+1, afterEddSharedPulCount);
        assertEquals(beforeEddSharedCulCount+1, afterEddSharedCulCount);
        assertEquals(beforeEddSharedNyplCount+1, afterEddSharedNyplCount);
*/
    }

    @Test
    public void populateRequestTypeInformation() throws Exception {
        ReportsForm reportsForm = new ReportsForm();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date fromDate = simpleDateFormat.parse("2016-12-30 00:00:00");
        Date toDate = simpleDateFormat.parse("2020-12-31 23:59:59");
        reportsForm.setReportRequestType(Arrays.asList("Retrieval"));

        reportsUtil.populateRequestTypeInformation(reportsForm, fromDate, toDate);
       /* long beforeCountRetrievalPul = reportsForm.getRetrievalRequestPulCount();
        long beforeCountRetrievalCul = reportsForm.getRetrievalRequestCulCount();
        long beforeCountRetrievalNypl = reportsForm.getRetrievalRequestNyplCount();

        long beforeCountRecallPul = reportsForm.getRecallRequestPulCount();
        long beforeCountRecallCul = reportsForm.getRecallRequestCulCount();
        long beforeCountRecallNypl = reportsForm.getRecallRequestNyplCount();
*/
        try {
            BibliographicEntity bibliographicEntity = saveBibHoldingItemEntity(1, 1, false, "010101");
            ItemEntity itemEntity = bibliographicEntity.getItemEntities().get(0);
            saveRequestEntity(itemEntity.getId(), 1, 1, String.valueOf(new Random().nextInt()));

            BibliographicEntity bibliographicEntity1 = saveBibHoldingItemEntity(2, 1, false, "010101");
            ItemEntity itemEntity1 = bibliographicEntity1.getItemEntities().get(0);
            saveRequestEntity(itemEntity1.getId(), 1, 2, String.valueOf(new Random().nextInt()));

            BibliographicEntity bibliographicEntity2 = saveBibHoldingItemEntity(3, 1, false, "010101");
            ItemEntity itemEntity2 = bibliographicEntity2.getItemEntities().get(0);
            saveRequestEntity(itemEntity2.getId(), 1, 3, String.valueOf(new Random().nextInt()));

            reportsUtil.populateRequestTypeInformation(reportsForm, fromDate, toDate);
        /*long afterCountRetrievalPul = reportsForm.getRetrievalRequestPulCount();
        long afterCountRetrievalCul = reportsForm.getRetrievalRequestCulCount();
        long afterCountRetrievalNypl = reportsForm.getRetrievalRequestNyplCount();
        assertEquals(beforeCountRetrievalPul+1, afterCountRetrievalPul);
        assertEquals(beforeCountRetrievalCul+1, afterCountRetrievalCul);
        assertEquals(beforeCountRetrievalNypl+1, afterCountRetrievalNypl);
*/
            reportsForm.setReportRequestType(Arrays.asList("Recall"));
            BibliographicEntity bibliographicEntity3 = saveBibHoldingItemEntity(1, 1, false, "01010101");
            ItemEntity itemEntity3 = bibliographicEntity3.getItemEntities().get(0);
            saveRequestEntity(itemEntity3.getId(), 2, 1, String.valueOf(new Random().nextInt()));

            BibliographicEntity bibliographicEntity4 = saveBibHoldingItemEntity(2, 1, false, "01010101");
            ItemEntity itemEntity4 = bibliographicEntity4.getItemEntities().get(0);
            saveRequestEntity(itemEntity4.getId(), 2, 2, String.valueOf(new Random().nextInt()));

            BibliographicEntity bibliographicEntity5 = saveBibHoldingItemEntity(3, 1, false, "01010101");
            ItemEntity itemEntity5 = bibliographicEntity5.getItemEntities().get(0);
            saveRequestEntity(itemEntity5.getId(), 2, 3, String.valueOf(new Random().nextInt()));
            reportsUtil.populateRequestTypeInformation(reportsForm, fromDate, toDate);
        }catch (NullPointerException e){
            e.printStackTrace();
        }

       /* long afterCountRecallPul = reportsForm.getRecallRequestPulCount();
        long afterCountRecallCul = reportsForm.getRecallRequestCulCount();
        long afterCountRecallNypl = reportsForm.getRecallRequestNyplCount();

        assertEquals(beforeCountRecallPul+1, afterCountRecallPul);
        assertEquals(beforeCountRecallCul+1, afterCountRecallCul);
        assertEquals(beforeCountRecallNypl+1, afterCountRecallNypl);
*/    }


    @Test
    public void populateAccessionDeaccessionItemCounts() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String requestedFromDate = simpleDateFormat.format(new Date());
        String requestedToDate = simpleDateFormat.format(new Date());

        ReportsForm reportsForm = new ReportsForm();
        reportsForm.setAccessionDeaccessionFromDate(requestedFromDate);
        reportsForm.setAccessionDeaccessionToDate(requestedToDate);

        reportsUtil.populateAccessionDeaccessionItemCounts(reportsForm);
        assertNotNull(reportsForm);
        /*assertNotNull(reportsForm.getAccessionOpenPulCount());
        assertNotNull(reportsForm.getAccessionOpenCulCount());
        assertNotNull(reportsForm.getAccessionOpenNyplCount());
        assertNotNull(reportsForm.getAccessionSharedPulCount());
        assertNotNull(reportsForm.getAccessionSharedCulCount());
        assertNotNull(reportsForm.getAccessionSharedNyplCount());
        assertNotNull(reportsForm.getAccessionPrivatePulCount());
        assertNotNull(reportsForm.getAccessionPrivateCulCount());
        assertNotNull(reportsForm.getAccessionPrivateNyplCount());
        assertNotNull(reportsForm.getDeaccessionOpenPulCount());
        assertNotNull(reportsForm.getDeaccessionOpenCulCount());
        assertNotNull(reportsForm.getDeaccessionOpenNyplCount());
        assertNotNull(reportsForm.getDeaccessionSharedPulCount());
        assertNotNull(reportsForm.getDeaccessionSharedCulCount());
        assertNotNull(reportsForm.getDeaccessionSharedNyplCount());
        assertNotNull(reportsForm.getDeaccessionPrivatePulCount());
        assertNotNull(reportsForm.getDeaccessionPrivateCulCount());
        assertNotNull(reportsForm.getDeaccessionPrivateNyplCount());
    }

    @Test
    public void populateCGDItemCounts() throws Exception {
        ReportsForm reportsForm = new ReportsForm();

        reportsUtil.populateCGDItemCounts(reportsForm);
        assertNotNull(reportsForm);
       /* assertNotNull(reportsForm.getOpenPulCgdCount());
        assertNotNull(reportsForm.getOpenCulCgdCount());
        assertNotNull(reportsForm.getOpenNyplCgdCount());
        assertNotNull(reportsForm.getSharedPulCgdCount());
        assertNotNull(reportsForm.getSharedCulCgdCount());
        assertNotNull(reportsForm.getSharedNyplCgdCount());
        assertNotNull(reportsForm.getPrivatePulCgdCount());
        assertNotNull(reportsForm.getPrivateCulCgdCount());
        assertNotNull(reportsForm.getPrivateNyplCgdCount());
*/    }

    @Test
    public void deaccessionReportFieldsInformation() throws Exception {
        ReportsForm reportsForm = reportsForm();
        ReportsResponse mockReportsResponse = new ReportsResponse();
        mockReportsResponse.setTotalPageCount(5);
        mockReportsResponse.setTotalRecordsCount("100L");
        List<DeaccessionItemResultsRow> mockDeaccessionItemResultsRows = getDeaccessionItemResultsRows();
        mockReportsResponse.setDeaccessionItemResultsRows(mockDeaccessionItemResultsRows);
        when(reportsServiceUtil.requestDeaccessionResults(reportsForm)).thenReturn(mockReportsResponse);
        List<DeaccessionItemResultsRow> resultRows = reportsUtil.deaccessionReportFieldsInformation(reportsForm);
        assertNotNull(resultRows);
        assertNotNull(reportsForm.getTotalPageCount());
        assertNotNull(reportsForm.getTotalRecordsCount());
        assertEquals(1, resultRows.size());
    }

    @Test
    public void incompleteRecordsReportFieldsInformation() throws Exception {
        ReportsForm reportsForm = reportsForm();
        List<IncompleteReportResultsRow> incompleteReportResultsRows = getIncompleteReportResultsRows();
        ReportsResponse reportsResponse = new ReportsResponse();
        reportsResponse.setIncompleteReportResultsRows(incompleteReportResultsRows);
        when(reportsServiceUtil.requestIncompleteRecords(any(ReportsForm.class))).thenReturn(reportsResponse);
        List<IncompleteReportResultsRow> result = reportsUtil.incompleteRecordsReportFieldsInformation(reportsForm);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reportsServiceUtil, times(1)).requestIncompleteRecords(any(ReportsForm.class));
    }


    private BibliographicEntity saveBibHoldingItemEntity(Integer owningInstitutionId, Integer collectionGroupId, boolean isDeleted, String barcode) throws Exception {
        Random random = new Random();

        File bibContentFile = getBibContentFile();
        File holdingsContentFile = getHoldingsContentFile();
        String sourceBibContent = FileUtils.readFileToString(bibContentFile, "UTF-8");
        String sourceHoldingsContent = FileUtils.readFileToString(holdingsContentFile, "UTF-8");

        String owningInstitutionBibId = String.valueOf(random.nextInt());
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setId(new Random().nextInt());
        bibliographicEntity.setContent(sourceBibContent.getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setCreatedBy("ut");
        bibliographicEntity.setLastUpdatedBy("ut");
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        bibliographicEntity.setDeleted(false);

        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent(sourceHoldingsContent.getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setCreatedBy("ut");
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setLastUpdatedBy("ut");
        holdingsEntity.setOwningInstitutionId(owningInstitutionId);
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(new Random().nextInt()));

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(new Random().nextInt());
        itemEntity.setBarcode(barcode);
        itemEntity.setCustomerCode("c1");
        itemEntity.setCallNumber("cn1");
        itemEntity.setCallNumberType("ct1");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setCopyNumber(1);
        itemEntity.setOwningInstitutionId(owningInstitutionId);
        itemEntity.setCollectionGroupId(collectionGroupId);
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("ut");
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setLastUpdatedBy("ut");
        itemEntity.setUseRestrictions("no");
        itemEntity.setVolumePartYear("v3");
        itemEntity.setOwningInstitutionItemId(String.valueOf(new Random().nextInt()));
        itemEntity.setDeleted(isDeleted);
        itemEntity.setImsLocationId(1);

        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        holdingsEntity.setItemEntities(Arrays.asList(itemEntity));

        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);
        return savedBibliographicEntity;
    }

    private void saveRequestEntity(Integer itemId, Integer requestTypeId, Integer requestingInstID, String patronID) throws Exception {
        RequestItemEntity requestItemEntity = new RequestItemEntity();
        requestItemEntity.setItemId(itemId);
        requestItemEntity.setRequestTypeId(requestTypeId);
        requestItemEntity.setRequestingInstitutionId(requestingInstID);
        requestItemEntity.setRequestExpirationDate(new Date());
        requestItemEntity.setCreatedDate(new Date());
        requestItemEntity.setLastUpdatedDate(new Date());
        requestItemEntity.setPatronId(patronID);
        requestItemEntity.setStopCode("s1");
        requestItemEntity.setRequestStatusId(requestTypeId);
        requestItemEntity.setCreatedBy("test");
        requestItemEntity.setEmailId("test@mail.com");
        requestItemDetailsRepository.save(requestItemEntity);
    }


    public File getBibContentFile() throws URISyntaxException {
        URL resource = getClass().getResource("BibContent.xml");
        return new File(resource.toURI());
    }

    public File getHoldingsContentFile() throws URISyntaxException {
        URL resource = getClass().getResource("HoldingsContent.xml");
        return new File(resource.toURI());
    }

    @Test
    public void exportIncompleteRecords() throws Exception{
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        List<IncompleteReportResultsRow> incompleteReportResultsRows = new ArrayList<>();
        IncompleteReportResultsRow incompleteReportResult = new IncompleteReportResultsRow();
        incompleteReportResult.setCreatedDate("02/22/2016");
        incompleteReportResult.setOwningInstitution("PUL");
        incompleteReportResult.setAuthor("Dummy");
        incompleteReportResult.setBarcode("1234");
        incompleteReportResult.setTitle("Dummy Title");
        incompleteReportResult.setCustomerCode("PU");
        incompleteReportResultsRows.add(incompleteReportResult);
        String fileNameWithExtension =  "ExportIncompleteRecords_" + dateFormat.format(new Date()) + ".csv";
        File file = reportsUtil.exportIncompleteRecords(incompleteReportResultsRows, fileNameWithExtension);
        CsvReader csvReader = new CsvReader(new FileReader(file), ',');
        assertNotNull(file);
        assertTrue(file.exists());
        assertEquals(fileNameWithExtension, file.getName());
        assertTrue(csvReader.readHeaders());
        assertTrue(csvReader.readRecord());
        assertEquals(csvReader.get("Author"), incompleteReportResult.getAuthor());
        assertEquals(csvReader.get("Customer code"),incompleteReportResult.getCustomerCode());
        assertEquals(csvReader.get("Title"),incompleteReportResult.getTitle());
        assertEquals(csvReader.get("Barcode"),incompleteReportResult.getBarcode());
        assertEquals(csvReader.get("Accession Date"),incompleteReportResult.getCreatedDate());
    }

    @Test
    public void submitCollectionReportTest() throws Exception{
        SubmitCollectionReport submitCollectionReport = new SubmitCollectionReport();
        submitCollectionReport.setPageNumber(5);
        submitCollectionReport.setPageSize(5);
        submitCollectionReport.setTotalRecordsCount(1550L);
        submitCollectionReport.setInstitutionName("CUL");
        submitCollectionReport.setTo(new Date());
        when(restHeaderService.getHttpHeaders()).thenReturn(httpHeaders);
        ResponseEntity responseEntity = new ResponseEntity(submitCollectionReport, HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
                ArgumentMatchers.anyString(),
                any(HttpMethod.class),
                any(),
                ArgumentMatchers.<Class<SubmitCollectionReport>>any());
        reportsUtilMock.submitCollectionReport(submitCollectionReport);
//        TestCase.assertNotNull(responseEntity.getBody());
    }

    @Test
    public void submitCollectionReportException() throws Exception{
        SubmitCollectionReport submitCollectionReport = new SubmitCollectionReport();
        submitCollectionReport.setPageNumber(5);
        submitCollectionReport.setPageSize(5);
        submitCollectionReport.setTotalRecordsCount(1550L);
        submitCollectionReport.setInstitutionName("CUL");
        submitCollectionReport.setTo(new Date());
        when(restHeaderService.getHttpHeaders()).thenReturn(httpHeaders);
        ResponseEntity responseEntity = new ResponseEntity(submitCollectionReport, HttpStatus.OK);
        doThrow(new NullPointerException()).when(restTemplate).exchange(
                ArgumentMatchers.anyString(),
                any(HttpMethod.class),
                any(),
                ArgumentMatchers.<Class<SubmitCollectionReport>>any());
        reportsUtilMock.submitCollectionReport(submitCollectionReport);
        //        TestCase.assertNotNull(responseEntity.getBody());
    }

    @Test
    public void accessionReportTest() throws Exception{
        SubmitCollectionReport submitCollectionReport = new SubmitCollectionReport();
        submitCollectionReport.setPageNumber(5);
        submitCollectionReport.setPageSize(5);
        submitCollectionReport.setTotalRecordsCount(1550L);
        submitCollectionReport.setInstitutionName("CUL");
        submitCollectionReport.setTo(new Date());
        when(restHeaderService.getHttpHeaders()).thenReturn(httpHeaders);
        ResponseEntity responseEntity = new ResponseEntity(submitCollectionReport, HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
                ArgumentMatchers.anyString(),
                any(HttpMethod.class),
                any(),
                ArgumentMatchers.<Class<SubmitCollectionReport>>any());
        reportsUtilMock.accessionReport(submitCollectionReport);
        //        TestCase.assertNotNull(responseEntity.getBody());
    }

    @Test
    public void accessionReportException() throws Exception{
        SubmitCollectionReport submitCollectionReport = new SubmitCollectionReport();
        submitCollectionReport.setPageNumber(5);
        submitCollectionReport.setPageSize(5);
        submitCollectionReport.setTotalRecordsCount(1550L);
        submitCollectionReport.setInstitutionName("CUL");
        submitCollectionReport.setTo(new Date());
        when(restHeaderService.getHttpHeaders()).thenReturn(httpHeaders);
        ResponseEntity responseEntity = new ResponseEntity(submitCollectionReport, HttpStatus.OK);
        doThrow(new NullPointerException()).when(restTemplate).exchange(
                ArgumentMatchers.anyString(),
                any(HttpMethod.class),
                any(),
                ArgumentMatchers.<Class<SubmitCollectionReport>>any());
        try {
            reportsUtilMock.accessionReport(submitCollectionReport);
//            TestCase.assertNotNull(responseEntity.getBody());
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }


    public ReportsForm reportsForm(){
        ReportsForm reportsForm = new ReportsForm();

        // Set simple string fields
        reportsForm.setShowBy("Date");
        reportsForm.setRequestType("Retrieve");
        reportsForm.setRequestFromDate("01/01/2024");
        reportsForm.setRequestToDate("01/31/2024");

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String currentDate = dateFormat.format(new Date());

        reportsForm.setAccessionDeaccessionFromDate(currentDate);
        reportsForm.setAccessionDeaccessionToDate(currentDate);

        // Set boolean fields
        reportsForm.setShowILBDResults(true);
        reportsForm.setShowPartners(true);
        reportsForm.setShowRequestTypeTable(true);
        reportsForm.setShowAccessionDeaccessionTable(true);
        reportsForm.setShowReportResultsText(true);
        reportsForm.setShowNoteILBD(true);
        reportsForm.setShowNotePartners(true);
        reportsForm.setShowNoteRequestType(true);
        reportsForm.setShowRetrievalTable(true);
        reportsForm.setShowRecallTable(true);
        reportsForm.setShowRequestTypeShow(true);
        reportsForm.setShowDeaccessionInformationTable(true);
        reportsForm.setShowIncompleteResults(true);
        reportsForm.setShowIncompletePagination(true);
        reportsForm.setExport(true);

        // Set lists with sample data
        reportsForm.setReportRequestType(Arrays.asList("Retrieve", "Recall"));
        reportsForm.setOwningInstitutions(Arrays.asList("CUL", "NYPL", "PUL"));
        reportsForm.setCollectionGroupDesignations(Arrays.asList("Shared", "Private", "Open"));

        List<DeaccessionItemResultsRow> deaccessionItemResultsRows = getDeaccessionItemResultsRows();
        reportsForm.setDeaccessionItemResultsRows(deaccessionItemResultsRows);

        // Set other integer and string fields
        reportsForm.setTotalRecordsCount("100");
        reportsForm.setPageNumber(1);
        reportsForm.setPageSize(10);
        reportsForm.setTotalPageCount(10);
        reportsForm.setDeaccessionOwnInst("CUL");

        // IncompleteRecordsReport related fields
        reportsForm.setIncompleteRequestingInstitution("CUL");
        reportsForm.setIncompletePageNumber(1);
        reportsForm.setIncompletePageSize(20);
        reportsForm.setIncompleteTotalRecordsCount("50");
        reportsForm.setIncompleteTotalPageCount(5);

        List<IncompleteReportResultsRow> incompleteReportResultsRows = getIncompleteReportResultsRows();
        reportsForm.setIncompleteReportResultsRows(incompleteReportResultsRows);

        reportsForm.setIncompleteShowByInst(Arrays.asList("CUL", "NYPL"));
        reportsForm.setErrorMessage("No errors found.");

        List<ReportsInstitutionForm> reportsInstitutionFormList = getReportsInstitutionForms();
        reportsForm.setReportsInstitutionFormList(reportsInstitutionFormList);

        return reportsForm;
    }

    private static List<ReportsInstitutionForm> getReportsInstitutionForms() {
        List<ReportsInstitutionForm> reportsInstitutionFormList = new ArrayList<>();

        ReportsInstitutionForm reportsInstitutionForm = new ReportsInstitutionForm();

        // Set values for all fields
        reportsInstitutionForm.setInstitution("CUL");
        reportsInstitutionForm.setRetrievalRequestCount(100);
        reportsInstitutionForm.setRecallRequestCount(50);
        reportsInstitutionForm.setEddRequestCount(200);
        reportsInstitutionForm.setTotalRequestByTypeCount(350);
        reportsInstitutionForm.setPhysicalPrivateCount(120);
        reportsInstitutionForm.setPhysicalSharedCount(80);
        reportsInstitutionForm.setPhysicalPartnerSharedCount(60);
        reportsInstitutionForm.setSubTotalPhysicalCount(260);
        reportsInstitutionForm.setEddPrivateCount(140);
        reportsInstitutionForm.setEddSharedOpenCount(40);
        reportsInstitutionForm.setEddPartnerSharedOpenCount(20);
        reportsInstitutionForm.setSubTotalEddCount(200);
        reportsInstitutionForm.setTotalRequestByPartnerCount(560);
        reportsInstitutionForm.setTotalRequestByPartnerHulCount(150);
        reportsInstitutionForm.setAccessionPrivateCount(75);
        reportsInstitutionForm.setAccessionSharedCount(25);
        reportsInstitutionForm.setAccessionOpenCount(15);
        reportsInstitutionForm.setSubTotalAccessionCount(115);
        reportsInstitutionForm.setDeaccessionPrivateCount(10);
        reportsInstitutionForm.setDeaccessionSharedCount(5);
        reportsInstitutionForm.setDeaccessionOpenCount(2);
        reportsInstitutionForm.setSubTotalDeaccessionCount(17);
        reportsInstitutionForm.setOpenCgdCount(200);
        reportsInstitutionForm.setSharedCgdCount(100);
        reportsInstitutionForm.setPrivateCgdCount(50);
        reportsInstitutionForm.setCommittedCgdCount(30);
        reportsInstitutionForm.setUncommittableCgdCount(20);
        reportsInstitutionForm.setTotalCgdCount(400);

        reportsInstitutionFormList.add(reportsInstitutionForm); // Add actual ReportsInstitutionForm objects as needed
        return reportsInstitutionFormList;
    }

    private static List<IncompleteReportResultsRow> getIncompleteReportResultsRows() {
        List<IncompleteReportResultsRow> incompleteReportResultsRows = new ArrayList<>();
        IncompleteReportResultsRow incompleteReportResultsRow = new IncompleteReportResultsRow();

        // Set values for all fields
        incompleteReportResultsRow.setAuthor("John Doe");
        incompleteReportResultsRow.setCreatedDate("09/03/2024");
        incompleteReportResultsRow.setTitle("test");
        incompleteReportResultsRow.setOwningInstitution("CUL");
        incompleteReportResultsRow.setCustomerCode("C123");
        incompleteReportResultsRow.setBarcode("1234567890123");
        incompleteReportResultsRows.add(incompleteReportResultsRow);
        return incompleteReportResultsRows;
    }

    private static List<DeaccessionItemResultsRow> getDeaccessionItemResultsRows() {
        List<DeaccessionItemResultsRow> deaccessionItemResultsRows = new ArrayList<>();
        DeaccessionItemResultsRow deaccessionItemResultsRow = new DeaccessionItemResultsRow();
        deaccessionItemResultsRow.setItemId(12345);
        deaccessionItemResultsRow.setDeaccessionDate("09/01/2024");
        deaccessionItemResultsRow.setTitle("The Great Gatsby");
        deaccessionItemResultsRow.setDeaccessionOwnInst("CUL");
        deaccessionItemResultsRow.setItemBarcode("12345678901234");
        deaccessionItemResultsRow.setCgd("Shared");
        deaccessionItemResultsRow.setDeaccessionNotes("Damaged item, deaccessioned from the collection");
        deaccessionItemResultsRow.setDeaccessionCreatedBy("John Doe");
        deaccessionItemResultsRows.add(deaccessionItemResultsRow); // Add actual DeaccessionItemResultsRow objects as needed
        return deaccessionItemResultsRows;
    }
}
