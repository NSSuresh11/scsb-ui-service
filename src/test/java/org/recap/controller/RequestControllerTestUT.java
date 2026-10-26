package org.recap.controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.recap.Main;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.CancelRequestResponse;
import org.recap.model.jpa.*;
import org.recap.model.request.ItemRequestInformation;
import org.recap.model.request.ItemResponseInformation;
import org.recap.model.request.ReplaceRequest;
import org.recap.model.search.RequestForm;
import org.recap.model.search.SearchResultRow;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.repository.jpa.*;
import org.recap.security.UserManagementService;
import org.recap.service.RequestService;
import org.recap.service.RestHeaderService;
import org.recap.util.RequestServiceUtil;
import org.recap.util.UserAuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.persistence.EntityManager;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Created by rajeshbabuk on 21/10/16.
 */

@SpringBootTest(classes = Main.class)
public class RequestControllerTestUT  {

    @InjectMocks
    RequestController requestControllerWired;

    @Mock
    BindingAwareModelMap model;

    @Mock
    RequestService requestService;

    @Mock
    RequestService requestServiceMocked;

    @Mock
    BindingResult bindingResult;

    @Mock
    RequestController requestController;

    @Mock
    RequestServiceUtil requestServiceUtil;

    @Mock
    InstitutionDetailsRepository institutionDetailsRepository;

    @Mock
    RequestTypeDetailsRepository requestTypeDetailsRepository;

    @Mock
    OwnerCodeDetailsRepository ownerCodeDetailsRepository;

    @Mock
    HttpSession session;

    @Mock
    javax.servlet.http.HttpServletRequest request;

    @Mock
    private UserAuthUtil userAuthUtil;

    @Mock
    ItemDetailsRepository itemDetailsRepository;

    @Mock
    EntityManager entityManager;

    @Mock
    RestTemplate restTemplate;

    @Value("${"+ PropertyKeyConstants.SCSB_GATEWAY_URL + "}")
    String scsbUrl;

    @Value("${" + PropertyKeyConstants.SCSB_AUTH_URL + "}")
    String scsbShiro;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    @Mock
    RestHeaderService restHeaderService;

    @Mock
    RequestItemDetailsRepository requestItemDetailsRepository;

    @Mock
    RequestStatusDetailsRepository requestStatusDetailsRepository;

    @Mock
    ItemRequestInformation itemRequestInformation;

    @Mock
    UserManagementService userManagementService;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
//        this.mockMvc = MockMvcBuilders.standaloneSetup(requestController).build();
    }

    public BindingAwareModelMap getModel() {
        return model;
    }

    public String getScsbUrl() {
        return scsbUrl;
    }

    public String getScsbShiro() {
        return scsbShiro;
    }

    @Test
    public void searchRequests() throws Exception {
        RequestForm requestForm = getRequestForm();
        String institution ="PUL";
        Page<RequestItemEntity> requestItemEntities = new PageImpl<RequestItemEntity>(new ArrayList<>());
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setInstitutionCode("PUL");
        institutionEntity.setInstitutionName("Princeton");
        when(requestController.getRequestServiceUtil()).thenReturn(requestServiceUtil);
    }

    @Test
    public void searchPrevious() throws Exception {
        RequestForm requestForm = new RequestForm();
        Page<RequestItemEntity> requestItemEntities = new PageImpl<RequestItemEntity>(new ArrayList<>());
        when(requestController.getRequestServiceUtil()).thenReturn(requestServiceUtil);
        when(requestServiceUtil.searchRequests(requestForm)).thenReturn(requestItemEntities);
        when(requestController.searchPrevious(requestForm)).thenCallRealMethod();
    }

    @Test
    public void searchNext() throws Exception {
        RequestForm requestForm = new RequestForm();
        Page<RequestItemEntity> requestItemEntities = new PageImpl<RequestItemEntity>(new ArrayList<>());
        when(requestController.getRequestServiceUtil()).thenReturn(requestServiceUtil);
        when(requestServiceUtil.searchRequests(requestForm)).thenReturn(requestItemEntities);
    }
    @Test
    public void onRequestPageSizeChange() throws Exception {
        RequestForm requestForm = getRequestForm();
        Page<RequestItemEntity> requestItemEntities = new PageImpl<RequestItemEntity>(new ArrayList<>());
        when(requestController.getRequestServiceUtil()).thenReturn(requestServiceUtil);
        when(requestServiceUtil.searchRequests(requestForm)).thenReturn(requestItemEntities);
        when(requestController.onRequestPageSizeChange(requestForm)).thenCallRealMethod();
    }
    @Test
    public void searchFirst() throws Exception {
        RequestForm requestForm = new RequestForm();
        Page<RequestItemEntity> requestItemEntities = new PageImpl<RequestItemEntity>(new ArrayList<>());
        when(requestController.getRequestServiceUtil()).thenReturn(requestServiceUtil);
        when(requestServiceUtil.searchRequests(requestForm)).thenReturn(requestItemEntities);
    }

    @Test
    public void searchLast() throws Exception {
        RequestForm requestForm = getRequestForm();
        Page<RequestItemEntity> requestItemEntityPage = getPage();
        RequestItemEntity requestItemEntity = getRequestItemEntity();
        requestItemEntity.setRequestingInstitutionId(2);
        SearchResultRow searchResultRow = getSearchResultRow();
        when(requestController.getRequestServiceUtil()).thenReturn(requestServiceUtil);
        when(requestServiceUtil.searchRequests(requestForm)).thenReturn(requestItemEntityPage);
        when(requestController.setSearchResultRow(requestItemEntity)).thenReturn(searchResultRow);
    }

    @Test
    public void loadCreateRequest() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        Mockito.when(requestController.getUserAuthUtil()).thenReturn(userAuthUtil);
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        UserDetailsForm userDetailsForm = getUserDetails();
        Mockito.when(requestController.getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE)).thenReturn(userDetailsForm);
        RequestForm requestForm = getRequestForm();
        Mockito.when(requestController.getRequestService().setDefaultsToCreateRequest(userDetailsForm)).thenCallRealMethod();
    }

    @Test
    public void testLoadCreateRequestForSamePatron(){

        when(request.getSession(false)).thenReturn(session);
        Mockito.when(requestController.getUserAuthUtil()).thenReturn(userAuthUtil);
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        UserDetailsForm userDetailsForm = getUserDetails();
        Mockito.when(requestController.getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE)).thenReturn(userDetailsForm);
        RequestForm requestForm = getRequestForm();
        Mockito.when(requestController.getRequestService().setDefaultsToCreateRequest(userDetailsForm)).thenCallRealMethod();
    }

    @Test
    public void goToSearchRequest(){
        RequestForm requestForm = new RequestForm();
        Page<RequestItemEntity> requestItemEntityPage = getPage();
        RequestItemEntity requestItemEntity = getRequestItemEntity();
        requestItemEntity.setRequestingInstitutionId(2);
        SearchResultRow searchResultRow = getSearchResultRow();
        when(requestController.setSearchResultRow(requestItemEntity)).thenReturn(searchResultRow);
        Mockito.when(requestController.getInstitutionDetailsRepository()).thenReturn(institutionDetailsRepository);
        Mockito.when(requestController.getRequestServiceUtil()).thenReturn(requestServiceUtil);
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        Mockito.when(requestController.getUserAuthUtil()).thenReturn(userAuthUtil);
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setSuperAdmin(false);
        Mockito.when(requestController.getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE)).thenReturn(userDetailsForm);
        when(requestServiceUtil.searchRequests(requestForm)).thenReturn(requestItemEntityPage);
        RequestStatusEntity requestStatusEntity = new RequestStatusEntity();
        requestStatusEntity.setRequestStatusDescription("RETRIEVAL ORDER PLACED");
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setInstitutionCode("PUL");
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        List<String> requestStatusCodeList = getRequestStatusCodeList();
        List<String> institutionCodeList = getInstitutionCodeList();
        Mockito.when(requestController.getInstitutionDetailsRepository()).thenReturn(institutionDetailsRepository);
        Mockito.when(requestController.getUserAuthUtil()).thenReturn(userAuthUtil);
        Mockito.when(requestController.getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE)).thenReturn(userDetailsForm);
        Mockito.when(requestController.getInstitutionDetailsRepository()).thenReturn(institutionDetailsRepository);
        Mockito.when(requestController.getInstitutionDetailsRepository().findById(userDetailsForm.getLoginInstitutionId())).thenReturn(Optional.of(institutionEntity));
        Mockito.when(requestController.getInstitutionDetailsRepository().getInstitutionCodeForSuperAdmin(supportInstitution)).thenReturn(Arrays.asList(institutionEntity));
    }


    @Test
    public void populateItem() throws Exception {
        RequestForm requestForm = new RequestForm();
        BibliographicEntity bibliographicEntity = saveBibSingleHoldingsSingleItem();
        OwnerCodeEntity customerCodeEntity = new OwnerCodeEntity();
        customerCodeEntity.setOwnerCode("CU12513083");
        customerCodeEntity.setId(5);
        customerCodeEntity.setInstitutionId(1);
        customerCodeEntity.setDescription("Rare Books");
        String barcode = bibliographicEntity.getItemEntities().get(0).getBarcode();
        String customerCode = bibliographicEntity.getItemEntities().get(0).getCustomerCode();
        requestForm.setItemBarcodeInRequest(barcode);
        List<ItemEntity> itemEntityArrayList;
        itemEntityArrayList = bibliographicEntity.getItemEntities();
        Mockito.when(requestController.getUserAuthUtil()).thenReturn(userAuthUtil);
        when(request.getSession()).thenReturn(session);
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        UserDetailsForm userDetailsForm = getUserDetails();
        Mockito.when(requestController.getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE)).thenReturn(userDetailsForm);
        Mockito.when(requestController.getUserAuthUtil().getUserDetails(request.getSession(), ScsbConstants.REQUEST_PRIVILEGE)).thenReturn(getUserDetails());
        List<RequestTypeEntity> requestTypeEntityList=new ArrayList<>();
        RequestTypeEntity requestTypeEntity = new RequestTypeEntity();
        requestTypeEntity.setRequestTypeCode("RETRIEVAL");
        requestTypeEntity.setRequestTypeDesc("RETRIEVAL");
        requestTypeEntity.setId(1);
        requestTypeEntityList.add(requestTypeEntity);
    }

    @Test
    public void checkGetterServices(){
        Mockito.when(requestController.getRequestServiceUtil()).thenCallRealMethod();
        Mockito.when(requestController.getUserAuthUtil()).thenCallRealMethod();
        Mockito.when(requestController.getInstitutionDetailsRepository()).thenCallRealMethod();
        Mockito.when(requestController.getOwnerCodeDetailsRepository()).thenCallRealMethod();
        Mockito.when(requestController.getScsbShiro()).thenCallRealMethod();
        Mockito.when(requestController.getScsbUrl()).thenCallRealMethod();
        Mockito.when(requestController.getRequestItemDetailsRepository()).thenCallRealMethod();
        Mockito.when(requestController.getRestTemplate()).thenCallRealMethod();
        Mockito.when(requestController.getRequestService()).thenCallRealMethod();

        assertNotEquals(requestController.getRequestServiceUtil(),requestServiceUtil);
        assertNotEquals(requestController.getUserAuthUtil(),userAuthUtil);
        assertNotEquals(requestController.getInstitutionDetailsRepository(),institutionDetailsRepository);
        assertNotEquals(requestController.getOwnerCodeDetailsRepository(),requestServiceUtil);
        assertNotEquals(requestController.getScsbShiro(),requestServiceUtil);
//        assertNotEquals(requestController.getScsbUrl(),scsbUrl);
        assertNotEquals(requestController.getRequestItemDetailsRepository(),requestItemDetailsRepository);
        assertNotEquals(requestController.getRestTemplate(),restTemplate);
        assertNotEquals(requestController.getRequestService(),requestService);
    }

    @Test
    public void testCreateRequest() throws Exception {
        RequestForm requestForm = getRequestForm();
        ResponseEntity responseEntity = new ResponseEntity(ScsbCommonConstants.VALID_REQUEST,HttpStatus.OK);
        ResponseEntity responseEntity1 = new ResponseEntity<ItemResponseInformation>(getItemResponseInformation(),HttpStatus.OK);
        String message;
        JSONObject json = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject1 = new JSONObject();
        JSONArray array = new JSONArray();
        jsonObject.put("1","PA");
        jsonObject.put("2","PB");
        array.put("RECALL");
        array.put("RETRIEVE");
        json.put("error", "No Error");
        json.put("noPermissionErrorMessage", "No");
        json.put("itemTitle", "testName");
        json.put("itemOwningInstitution", "CUL");
        json.put("deliveryLocation",jsonObject);
        json.put("requestTypes",array);
        message = json.toString();
        Mockito.when(request.getSession(false)).thenReturn(session);
        ItemRequestInformation itemRequestInformation = getItemRequestInformation();
        HttpEntity<ItemRequestInformation> requestEntity = new HttpEntity<>(itemRequestInformation, restHeaderService.getHttpHeaders());
        String validateRequestItemUrl = getScsbUrl() + ScsbConstants.VALIDATE_REQUEST_ITEM_URL;
        String requestItemUrl = scsbUrl + ScsbConstants.REQUEST_ITEM_URL;
        OwnerCodeEntity customerCodeEntity = new OwnerCodeEntity();
        customerCodeEntity.setOwnerCode("PG");
        Mockito.when(requestController.getItemRequestInformation()).thenReturn(itemRequestInformation);
        Mockito.when((String) session.getAttribute(ScsbConstants.USER_NAME)).thenReturn("Admin");
        Mockito.when(requestController.getRestTemplate()).thenReturn(restTemplate);
        Mockito.when(requestController.getScsbShiro()).thenReturn(scsbShiro);
        Mockito.when(requestController.getScsbUrl()).thenReturn(scsbUrl);
        Mockito.when(requestController.getRestHeaderService()).thenReturn(restHeaderService);
        Mockito.when(requestController.getOwnerCodeDetailsRepository()).thenReturn(ownerCodeDetailsRepository);
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        Mockito.when(requestController.getOwnerCodeDetailsRepository().findByDescription(requestForm.getDeliveryLocationInRequest())).thenReturn(customerCodeEntity);
        Mockito.when(requestController.getRestTemplate().exchange(requestItemUrl, HttpMethod.POST, requestEntity, ItemResponseInformation.class)).thenReturn(responseEntity1);
        Mockito.when(requestController.getRestTemplate().exchange(validateRequestItemUrl, HttpMethod.POST, requestEntity, String.class)).thenReturn(responseEntity);
        Mockito.when(requestController.createRequest(requestForm,request)).thenCallRealMethod();
    }
    @Test
    public void testCreateRequest1() throws Exception {
        RequestForm requestForm = getRequestForm();
        ResponseEntity responseEntity = new ResponseEntity(ScsbCommonConstants.VALID_REQUEST,HttpStatus.OK);
        ResponseEntity responseEntity1 = new ResponseEntity<ItemResponseInformation>(getItemResponseInformation(),HttpStatus.OK);
        String message =null;
        Mockito.when(request.getSession(false)).thenReturn(session);
        ItemRequestInformation itemRequestInformation = getItemRequestInformation();
        HttpEntity<ItemRequestInformation> requestEntity = new HttpEntity<>(itemRequestInformation, restHeaderService.getHttpHeaders());
        String validateRequestItemUrl = getScsbUrl() + ScsbConstants.VALIDATE_REQUEST_ITEM_URL;
        String requestItemUrl = scsbUrl + ScsbConstants.REQUEST_ITEM_URL;
        OwnerCodeEntity customerCodeEntity = new OwnerCodeEntity();
        customerCodeEntity.setOwnerCode("PG");
        Mockito.when(requestController.getItemRequestInformation()).thenReturn(itemRequestInformation);
        Mockito.when((String) session.getAttribute(ScsbConstants.USER_NAME)).thenReturn("Admin");
        Mockito.when(requestController.getRestTemplate()).thenReturn(restTemplate);
        Mockito.when(requestController.getScsbShiro()).thenReturn(scsbShiro);
        Mockito.when(requestController.getScsbUrl()).thenReturn(scsbUrl);
        Mockito.when(requestController.getRestHeaderService()).thenReturn(restHeaderService);
        Mockito.when(requestController.getOwnerCodeDetailsRepository()).thenReturn(ownerCodeDetailsRepository);
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        Mockito.when(requestController.getOwnerCodeDetailsRepository().findByDescription(requestForm.getDeliveryLocationInRequest())).thenReturn(customerCodeEntity);
        Mockito.when(requestController.getRestTemplate().exchange(requestItemUrl, HttpMethod.POST, requestEntity, ItemResponseInformation.class)).thenReturn(responseEntity1);
        Mockito.when(requestController.getRestTemplate().exchange(validateRequestItemUrl, HttpMethod.POST, requestEntity, String.class)).thenReturn(responseEntity);
        Mockito.when(requestController.createRequest(requestForm,request)).thenCallRealMethod();
    }
    @Test
    public void testRequestResubmit()throws Exception{
        ReplaceRequest replaceRequest = getReplaceRequest();
        RequestForm requestForm = getRequestForm();
        ItemRequestInformation itemRequestInfo = new ItemRequestInformation();
        HttpEntity requestEntity = new HttpEntity<>(restHeaderService.getHttpHeaders());
        ResponseEntity responseEntity1 = new ResponseEntity<ReplaceRequest>(replaceRequest,HttpStatus.OK);
        Mockito.when(requestController.getItemRequestInformation()).thenReturn(itemRequestInfo);
        String requestItemUrl = scsbUrl + ScsbConstants.URL_REQUEST_RESUBMIT;
        Mockito.when(requestController.getRestTemplate()).thenReturn(restTemplate);
        Mockito.when(requestController.getScsbShiro()).thenReturn(scsbShiro);
        Mockito.when(requestController.getScsbUrl()).thenReturn(scsbUrl);
        Mockito.when(requestController.getRestHeaderService()).thenReturn(restHeaderService);
        Mockito.when(requestController.getRestTemplate().postForEntity( scsbUrl + ScsbConstants.URL_REQUEST_RESUBMIT,itemRequestInfo,Map.class)).thenThrow(new RestClientException("Exception occured"));
        Mockito.when(requestController.getRestTemplate().exchange(requestItemUrl, HttpMethod.POST, requestEntity, ReplaceRequest.class)).thenReturn(responseEntity1);
        Mockito.when(requestController.resubmitRequest(requestForm)).thenCallRealMethod();
        String response = requestController.resubmitRequest(requestForm);
        assertNotNull(response);
    }

   /* @Test
    public void testCancelRequest() throws Exception {
        RequestForm requestForm = getRequestForm();
        HttpEntity requestEntity = new HttpEntity<>(restHeaderService.getHttpHeaders());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(scsbUrl + ScsbConstants.URL_REQUEST_CANCEL).queryParam(ScsbCommonConstants.REQUEST_ID, requestForm.getRequestId());
        CancelRequestResponse cancelRequestResponse = new CancelRequestResponse();
        cancelRequestResponse.setSuccess(true);
        cancelRequestResponse.setScreenMessage("Request cancelled.");
        ResponseEntity<CancelRequestResponse> responseEntity = new ResponseEntity<CancelRequestResponse>(cancelRequestResponse,HttpStatus.OK);
        RequestItemEntity requestItemEntity =getRequestItemEntity();
        RequestStatusEntity requestStatusEntity = new RequestStatusEntity();
        requestStatusEntity.setRequestStatusDescription("Cancelled");
        requestItemEntity.setRequestStatusEntity(requestStatusEntity);
        Mockito.when(requestController.getRestTemplate()).thenReturn(restTemplate);
        Mockito.when(requestController.getScsbShiro()).thenReturn(scsbShiro);
        Mockito.when(requestController.getScsbUrl()).thenReturn(scsbUrl);
        Mockito.when(requestController.getRestHeaderService()).thenReturn(restHeaderService);
        Mockito.when(requestController.getRequestItemDetailsRepository()).thenReturn(requestItemDetailsRepository);
        Mockito.when(requestController.getRequestItemDetailsRepository().findById(requestForm.getRequestId())).thenReturn(Optional.of(requestItemEntity));
        Mockito.when(requestController.getRestTemplate().exchange(builder.build().encode().toUri(), HttpMethod.POST, requestEntity, CancelRequestResponse.class)).thenReturn(responseEntity);
        Mockito.when(requestController.cancelRequest(requestForm)).thenCallRealMethod();
        String response = requestController.cancelRequest(requestForm);
        assertNotNull(response);
        assertTrue(response.contains("Request cancelled."));
    }*/

    @Test
    public void testCancelRequest() throws Exception {
        // Set the scsbUrl manually for the test
        String testScsbUrl = "http://localhost:8080/scsb"; // Replace with the correct test URL
        ReflectionTestUtils.setField(requestController, "scsbUrl", testScsbUrl);

        RequestForm requestForm = getRequestForm();
        HttpEntity requestEntity = new HttpEntity<>(restHeaderService.getHttpHeaders());

        // Use the testScsbUrl to ensure the URL is properly formed
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(testScsbUrl + ScsbConstants.URL_REQUEST_CANCEL)
                .queryParam(ScsbCommonConstants.REQUEST_ID, requestForm.getRequestId());

        CancelRequestResponse cancelRequestResponse = new CancelRequestResponse();
        cancelRequestResponse.setSuccess(true);
        cancelRequestResponse.setScreenMessage("Request cancelled.");
        ResponseEntity<CancelRequestResponse> responseEntity = new ResponseEntity<>(cancelRequestResponse, HttpStatus.OK);

        RequestItemEntity requestItemEntity = getRequestItemEntity();
        RequestStatusEntity requestStatusEntity = new RequestStatusEntity();
        requestStatusEntity.setRequestStatusDescription("Cancelled");
        requestItemEntity.setRequestStatusEntity(requestStatusEntity);

        Mockito.when(requestController.getRestTemplate()).thenReturn(restTemplate);
        Mockito.when(requestController.getScsbShiro()).thenReturn(scsbShiro);
        Mockito.when(requestController.getScsbUrl()).thenReturn(testScsbUrl);
        Mockito.when(requestController.getRestHeaderService()).thenReturn(restHeaderService);
        Mockito.when(requestController.getRequestItemDetailsRepository()).thenReturn(requestItemDetailsRepository);
        Mockito.when(requestController.getRequestItemDetailsRepository().findById(requestForm.getRequestId())).thenReturn(Optional.of(requestItemEntity));
        Mockito.when(requestController.getRestTemplate().exchange(builder.build().encode().toUri(), HttpMethod.POST, requestEntity, CancelRequestResponse.class)).thenReturn(responseEntity);
        Mockito.when(requestController.cancelRequest(requestForm)).thenCallRealMethod();

        String response = requestController.cancelRequest(requestForm);

        assertNotNull(response);
        assertTrue(response.contains("Request cancelled."));
    }

    @Test
    public void testLoadSearchRequest(){
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setRecapPermissionAllowed(true);
        userDetailsForm.setRepositoryUser(true);
        userDetailsForm.setSuperAdmin(true);
        RequestStatusEntity requestStatusEntity = new RequestStatusEntity();
        requestStatusEntity.setRequestStatusDescription("RETRIEVAL ORDER PLACED");
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setInstitutionCode("PUL");
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        List<String> requestStatusCodeList = getRequestStatusCodeList();
        List<String> institutionCodeList = getInstitutionCodeList();
        List<String> requestStatuses = new ArrayList<>();
        Mockito.when(requestController.getInstitutionDetailsRepository()).thenReturn(institutionDetailsRepository);
        Mockito.doCallRealMethod().when(requestServiceMocked).getInstitutionForSuperAdmin(institutionCodeList);
        Mockito.doNothing().when(requestServiceMocked).findAllRequestStatusExceptProcessing(requestStatuses);
        Mockito.doNothing().when(requestServiceMocked).findAllRequestStatus(requestStatuses);
        Mockito.when(requestController.getUserAuthUtil()).thenReturn(userAuthUtil);
        Mockito.when(requestController.getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE)).thenReturn(userDetailsForm);
        Mockito.when(requestController.getInstitutionDetailsRepository()).thenReturn(institutionDetailsRepository);
        Mockito.when(requestController.getInstitutionDetailsRepository().findById(userDetailsForm.getLoginInstitutionId())).thenReturn(Optional.of(institutionEntity));
        Mockito.when(requestStatusDetailsRepository.findAll()).thenReturn(Arrays.asList(requestStatusEntity));
        Mockito.when(requestController.getInstitutionDetailsRepository().getInstitutionCodeForSuperAdmin(supportInstitution)).thenReturn(Arrays.asList(institutionEntity));
        Mockito.when(requestController.loadSearchRequest(request)).thenCallRealMethod();
    }


    @Test
    public void testRefreshStatus(){
        Mockito.when(requestController.getRequestService()).thenReturn(requestService);
        String status="status[]";
        String[] statusValue={"16-0"};
        MockHttpServletRequest mockedRequest = new MockHttpServletRequest();
        mockedRequest.addParameter(status, statusValue);
        RequestItemEntity requestItemEntity=getRequestItemEntity();
        Mockito.when(requestItemDetailsRepository.findByIdIn(Arrays.asList(requestItemEntity.getId()))).thenReturn(Arrays.asList(requestItemEntity));;
    }

    private RequestItemEntity getRequestItemEntity(){
        RequestStatusEntity requestStatusEntity=new RequestStatusEntity();
        requestStatusEntity.setRequestStatusDescription("RECALL");
        RequestItemEntity requestItemEntity = new RequestItemEntity();
        requestItemEntity.setRequestStatusId(15);
        requestItemEntity.setId(16);
        requestItemEntity.setRequestStatusEntity(requestStatusEntity);
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setInstitutionCode("UC");
        institutionEntity.setInstitutionName("University of Chicago");
        requestItemEntity.setInstitutionEntity(institutionEntity);
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("CU12513083");
        itemEntity.setCatalogingStatus("Complete");
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("Mock Bib Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionBibId("2");
        bibliographicEntity.setOwningInstitutionId(3);
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        itemEntity.setInstitutionEntity(institutionEntity);;
        requestItemEntity.setItemEntity(itemEntity);
        requestItemEntity.setRequestingInstitutionId(2);
        return requestItemEntity;
    }

    private ReplaceRequest getReplaceRequest(){
        ReplaceRequest replaceRequest = new ReplaceRequest();
        RequestForm requestForm = getRequestForm();
        replaceRequest.setEndRequestId("10");
        replaceRequest.setFromDate((new Date()).toString());
        replaceRequest.setToDate((new Date()).toString());
        replaceRequest.setReplaceRequestByType(ScsbCommonConstants.REQUEST_IDS);
        replaceRequest.setRequestStatus(ScsbConstants.EXCEPTION);
        String requestId = String.valueOf(requestForm.getRequestId());
        replaceRequest.setRequestIds(requestId);
        replaceRequest.setStartRequestId("1");
        return replaceRequest;
    }
    private RequestForm getRequestForm(){
        RequestForm requestForm = new RequestForm();
        requestForm.setRequestId(1);
        requestForm.setPatronBarcode("43265854");
        requestForm.setSubmitted(true);
        requestForm.setItemBarcode("32101074849843");
        requestForm.setStatus("active");
        requestForm.setDeliveryLocation("PB");
        requestForm.setVolumeNumber("1");
        requestForm.setMessage("testing");
        requestForm.setErrorMessage("error");
        requestForm.setIssue("issue");
        requestForm.setTotalPageCount(1);
        requestForm.setTotalRecordsCount("10");
        requestForm.setPageSize(1);
        requestForm.setPageNumber(1);
        requestForm.setInstitutionList(Arrays.asList("CU"));
        requestForm.setRequestingInstitutions(Arrays.asList("PUL"));
        requestForm.setRequestTypes(Arrays.asList("Recall"));
        requestForm.setItemBarcodeInRequest("123");
        requestForm.setPatronBarcodeInRequest("46259871");
        requestForm.setRequestingInstitution("CUL");
        requestForm.setPatronEmailAddress("test@email.com");
        requestForm.setInstitution("CU");
        requestForm.setItemTitle("test");
        requestForm.setItemOwningInstitution("PUL");
        requestForm.setRequestType("recall");
        requestForm.setRequestNotes("test");
        requestForm.setStartPage("2");
        requestForm.setEndPage("5");
        requestForm.setArticleAuthor("john");
        requestForm.setArticleTitle("test");
        requestForm.setDeliveryLocationInRequest("PB");
        return requestForm;
    }

    private ItemRequestInformation getItemRequestInformation(){
        ItemRequestInformation itemRequestInformation = new ItemRequestInformation();
        itemRequestInformation.setUsername("Admin");
        itemRequestInformation.setItemBarcodes(Arrays.asList("123"));
        itemRequestInformation.setPatronBarcode("46259871");
        itemRequestInformation.setRequestingInstitution("PUL");
        itemRequestInformation.setEmailAddress("test@email.com");
        itemRequestInformation.setTitle("test");
        itemRequestInformation.setTitleIdentifier("test");
        itemRequestInformation.setItemOwningInstitution("PUL");
        itemRequestInformation.setRequestType("recall");
        itemRequestInformation.setRequestNotes("test");
        itemRequestInformation.setStartPage("2");
        itemRequestInformation.setEndPage("5");
        itemRequestInformation.setAuthor("john");
        itemRequestInformation.setChapterTitle("test");
        itemRequestInformation.setDeliveryLocation("PB");
        return itemRequestInformation;
    }

    private ItemResponseInformation getItemResponseInformation(){
        ItemResponseInformation itemResponseInformation = new ItemResponseInformation();
        itemResponseInformation.setPatronBarcode("46259871");
        itemResponseInformation.setItemBarcode("123");
        itemResponseInformation.setSuccess(true);
        return itemResponseInformation;
    }

    private UserDetailsForm getUserDetails(){
        UserDetailsForm userDetailsForm=new UserDetailsForm();
        userDetailsForm.setLoginInstitutionId(2);
        userDetailsForm.setSuperAdmin(false);
        userDetailsForm.setRecapPermissionAllowed(false);
        userDetailsForm.setRepositoryUser(false);
        return userDetailsForm;
    }

    public BibliographicEntity saveBibSingleHoldingsSingleItem() throws Exception {
        Random random = new Random();
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
        itemEntity.setBarcode("CU12513083");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(3);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("PG");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setCatalogingStatus("Complete");
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        itemEntity.setBibliographicEntities(Arrays.asList(bibliographicEntity));
        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));
        return bibliographicEntity;

    }

    private List<String> getRequestStatusCodeList(){
        List<String> requestStatusEntityList=new ArrayList<>();
        RequestStatusEntity requestStatusEntity=new RequestStatusEntity();
        requestStatusEntity.setRequestStatusCode("RETRIEVAL_ORDER_PLACED");
        requestStatusEntity.setRequestStatusDescription("RETRIEVAL ORDER PLACED");
        requestStatusEntity.setId(1);
        requestStatusEntityList.add(requestStatusEntity.getRequestStatusCode());
        RequestStatusEntity requestStatusEntity1=new RequestStatusEntity();
        requestStatusEntity1.setRequestStatusCode("RECALL_ORDER_PLACED");
        requestStatusEntity1.setRequestStatusDescription("RECALL_ORDER_PLACED");
        requestStatusEntity1.setId(2);
        requestStatusEntityList.add(requestStatusEntity1.getRequestStatusCode());
        return requestStatusEntityList;
    }

    private List<String> getInstitutionCodeList(){
        List<String> institutionCodeList=new ArrayList<>();
        InstitutionEntity institutionEntity=new InstitutionEntity();
        institutionEntity.setInstitutionCode("PUL");
        institutionCodeList.add(institutionEntity.getInstitutionCode());
        InstitutionEntity institutionEntity1=new InstitutionEntity();
        institutionEntity1.setInstitutionCode("CUL");
        institutionCodeList.add(institutionEntity1.getInstitutionCode());
        InstitutionEntity institutionEntity2=new InstitutionEntity();
        institutionEntity2.setInstitutionCode("NYPL");
        institutionCodeList.add(institutionEntity2.getInstitutionCode());
        return institutionCodeList;
    }

    public Page getPage(){
        Page<RequestItemEntity> page = new Page<RequestItemEntity>() {
            @Override
            public int getTotalPages() {
                return 0;
            }

            @Override
            public long getTotalElements() {
                return 2;
            }

            @Override
            public <R> Page<R> map(Function<? super RequestItemEntity, ? extends R> converter) {
                return null;
            }

            @Override
            public int getNumber() {
                return 0;
            }

            @Override
            public int getSize() {
                return 0;
            }

            @Override
            public int getNumberOfElements() {
                return 0;
            }

            @Override
            public List<RequestItemEntity> getContent() {

                return Arrays.asList(getRequestItemEntity());
            }

            @Override
            public boolean hasContent() {
                return false;
            }

            @Override
            public Sort getSort() {
                return null;
            }

            @Override
            public boolean isFirst() {
                return false;
            }

            @Override
            public boolean isLast() {
                return false;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable nextPageable() {
                return null;
            }

            @Override
            public Pageable previousPageable() {
                return null;
            }

            @Override
            public Iterator<RequestItemEntity> iterator() {
                return null;
            }
        };
        return page;
    }

    private SearchResultRow getSearchResultRow(){
        SearchResultRow searchResultRow = new SearchResultRow();
        searchResultRow.setTitle("TestTitle");
        searchResultRow.setAuthor("TestAuthor");
        searchResultRow.setOwningInstitution("NYPL");
        searchResultRow.setBibId(1);
        searchResultRow.setPublisher("test");
        searchResultRow.setPublisherDate(new Date().toString());
        searchResultRow.setCustomerCode("PB");
        searchResultRow.setCollectionGroupDesignation("Open");
        searchResultRow.setUseRestriction("others");
        searchResultRow.setBarcode("332455546936368");
        searchResultRow.setSummaryHoldings("test");
        searchResultRow.setAvailability("available");
        searchResultRow.setLeaderMaterialType("test");
        searchResultRow.setSelected(true);
        searchResultRow.setShowItems(false);
        searchResultRow.setSelectAllItems(false);
        searchResultRow.setItemId(1);
        searchResultRow.setSearchItemResultRows(new ArrayList<>());
        searchResultRow.setShowAllItems(false);
        searchResultRow.setRequestId(1);
        searchResultRow.setPatronBarcode("452356654");
        searchResultRow.setRequestingInstitution("PUL");
        searchResultRow.setDeliveryLocation("PB");
        searchResultRow.setRequestType("Recall");
        searchResultRow.setRequestNotes("test");
        searchResultRow.setRequestCreatedBy("test");
        searchResultRow.setPatronEmailId("test@email.com");
        searchResultRow.setCreatedDate(new Date());
        searchResultRow.setStatus("success");
        return searchResultRow;
    }

}
