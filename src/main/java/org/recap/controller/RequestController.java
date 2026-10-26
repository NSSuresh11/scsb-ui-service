package org.recap.controller;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.CancelRequestResponse;
import org.recap.model.jpa.DeliveryCodeEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.OwnerCodeEntity;
import org.recap.model.jpa.RequestItemEntity;
import org.recap.model.reports.TransactionReport;
import org.recap.model.reports.TransactionReports;
import org.recap.model.request.ItemRequestInformation;
import org.recap.model.request.ItemResponseInformation;
import org.recap.model.request.ReplaceRequest;
import org.recap.model.search.RequestForm;
import org.recap.model.search.SearchResultRow;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.repository.jpa.DeliveryCodeDetailsRepository;
import org.recap.repository.jpa.ImsLocationDetailRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.OwnerCodeDetailsRepository;
import org.recap.repository.jpa.RequestItemDetailsRepository;
import org.recap.repository.jpa.UserDetailsRepository;
import org.recap.security.UserManagementService;
import org.recap.service.RequestService;
import org.recap.service.SCSBService;
import org.recap.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by rajeshbabuk on 13/10/16.
 */
@Slf4j
@RestController
@Data
@EqualsAndHashCode(callSuper = false)
@RequestMapping("/request")
public class RequestController extends ScsbController {


    @Autowired
    ReportsController reportsController;
    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private OwnerCodeDetailsRepository ownerCodeDetailsRepository;

    @Autowired
    private DeliveryCodeDetailsRepository deliveryCodeDetailsRepository;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private  UserManagementService userManagementService;

    @Autowired
    private ImsLocationDetailRepository imsLocationDetailRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private SCSBService scsbService;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    public RequestService getRequestService() {
        return requestService;
    }

    public InstitutionDetailsRepository getInstitutionDetailsRepository() {
        return institutionDetailsRepository;
    }

    /**
     * Get results from scsb database and display them as row based on the search conditions provided in the search request UI page.
     *
     * @param requestForm the request form
     * @return the model and view
     */
    @PostMapping("/searchRequests")
    public RequestForm searchRequests(@RequestBody RequestForm requestForm) {
        try {
            requestForm = setSearch(requestForm);
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            log.debug(exception.getMessage());
        }
        return requestForm;
    }

    /**
     * To know the request information of an item once the request is placed through the create request UI page.
     *
     * @param requestForm the request form
     * @return the model and view
     */
    @PostMapping("/goToSearchRequest")
    public RequestForm goToSearchRequest(@RequestBody RequestForm requestForm, HttpServletRequest request) {
        try {
            UserDetailsForm userDetails = getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE);
            requestForm.resetPageNumber();
            setFormValues(requestForm, userDetails);
            requestForm.setStatus("");
            requestForm.setItemBarcode("");
            requestForm = searchAndSetResults(requestForm);
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            log.debug(exception.getMessage());
        }
        return requestForm;
    }

    /**
     * Get first page results from scsb database and display them as row in the search request UI page.
     *
     * @param requestForm the request form
     * @return the model and view
     */
    @PostMapping("/first")
    public RequestForm searchFirst(@RequestBody RequestForm requestForm) {
        requestForm.setPageNumber(0);
        return setSearch(requestForm);
    }

    /**
     * Get last page results from scsb database and display them as row in the search request UI page.
     *
     * @param requestForm the request form
     * @return RequestForm
     */
    @PostMapping("/last")
    public RequestForm searchLast(@RequestBody RequestForm requestForm) {
        requestForm.setPageNumber(requestForm.getTotalPageCount() - 1);
        return searchAndSetResults(requestForm);
    }

    /**
     * Get previous page results from scsb database and display them as rows in the search request UI page.
     *
     * @param requestForm the request form
     * @return RequestForm
     */
    @PostMapping("/previous")
    public RequestForm searchPrevious(@RequestBody RequestForm requestForm) {
        requestForm.setPageNumber(requestForm.getPageNumber() - 1);
        return search(requestForm);
    }

    /**
     * Get next page results from scsb database and display them as rows in the search request UI page.
     *
     * @param requestForm the request form
     * @return the RequestForm
     */
    @PostMapping("/next")
    public RequestForm searchNext(@RequestBody RequestForm requestForm) {
        requestForm.setPageNumber(requestForm.getPageNumber() + 1);
        return search(requestForm);
    }

    /**
     * Based on the selected page size value that many request search results will be displayed in the search request UI page.
     *
     * @param requestForm the request form
     * @return RequestForm
     */
    @PostMapping("/requestPageSizeChange")
    public RequestForm onRequestPageSizeChange(@RequestBody RequestForm requestForm) {
        requestForm.setPageNumber(getPageNumberOnPageSizeChange(requestForm));
        return searchAndSetResults(requestForm);
    }

    /**
     * Populate default values to the request type and requesting institution drop downs in the create request UI page.
     *
     * @return the RequestForm
     */
    @GetMapping("/loadCreateRequest")
    public RequestForm loadCreateRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(session, ScsbConstants.REQUEST_PRIVILEGE);
        RequestForm requestForm = getRequestService().setDefaultsToCreateRequest(userDetailsForm);
        return requestForm;
    }

    /**
     * Load default values to the status and institution drop downs in search request UI page.
     *
     * @return the RequestForm
     */
    @GetMapping("/loadSearchRequest")
    public RequestForm loadSearchRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UserDetailsForm userDetails = getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE);
        RequestForm requestForm = new RequestForm();
        setFormValues(requestForm, userDetails);
        return requestForm;
    }

    /**
     * Based on the given barcode, this method gets the item information from scsb database to display it in the create request UI page.
     *
     * @param requestForm the request form
     * @return the string
     * @throws JSONException the json exception
     */

    @PostMapping("/populateItem")
    public String populateItem(@RequestBody RequestForm requestForm, HttpServletRequest request) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        try {
            return requestService.populateItemForRequest(requestForm, request);
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
            return jsonObject.put(ScsbConstants.ERROR_MESSAGE, e.getMessage()).toString();
        }
    }

    /**
     * This method passes information about the requesting item to the scsb-circ micro service to place a request in scsb.
     *
     * @param requestForm the request form
     * @return the model and view
     * @throws JSONException the json exception
     */
    @PostMapping("/createRequest")
    public RequestForm createRequest(@RequestBody RequestForm requestForm, HttpServletRequest request) throws JSONException {

        try {
            HttpSession session = request.getSession(false);
            String username = (String) session.getAttribute(ScsbConstants.USER_NAME);
            String stringJson = populateItem(requestForm, request);
            if (stringJson != null) {
                JSONObject responseJsonObject = new JSONObject(stringJson);
                Object errorMessage = responseJsonObject.has(ScsbConstants.ERROR_MESSAGE) ? responseJsonObject.get(ScsbConstants.ERROR_MESSAGE) : null;
                Object noPermissionErrorMessage = responseJsonObject.has(ScsbConstants.NO_PERMISSION_ERROR_MESSAGE) ? responseJsonObject.get(ScsbConstants.NO_PERMISSION_ERROR_MESSAGE) : null;
                Object itemTitle = responseJsonObject.has(ScsbConstants.REQUESTED_ITEM_TITLE) ? responseJsonObject.get(ScsbConstants.REQUESTED_ITEM_TITLE) : null;
                Object itemOwningInstitution = responseJsonObject.has(ScsbConstants.REQUESTED_ITEM_OWNING_INSTITUTION) ? responseJsonObject.get(ScsbConstants.REQUESTED_ITEM_OWNING_INSTITUTION) : null;
                Object deliveryLocations = responseJsonObject.has(ScsbConstants.DELIVERY_LOCATION) ? responseJsonObject.get(ScsbConstants.DELIVERY_LOCATION) : null;
                Object requestTypes = responseJsonObject.has(ScsbConstants.REQUEST_TYPES) ? responseJsonObject.get(ScsbConstants.REQUEST_TYPES) : null;
                List<OwnerCodeEntity> customerCodeEntities = new ArrayList<>();
                requestService.prepareRequestForm(requestForm, itemTitle, itemOwningInstitution, deliveryLocations, requestTypes, customerCodeEntities);
                if (noPermissionErrorMessage != null) {
                    requestForm.setErrorMessage((String) noPermissionErrorMessage);
                    requestForm.setShowRequestErrorMsg(true);
                    return requestForm;
                } else if (errorMessage != null) {
                    requestForm.setErrorMessage((String) errorMessage);
                    requestForm.setShowRequestErrorMsg(true);
                    return requestForm;
                }
            }

            String requestItemUrl = getScsbUrl() + ScsbConstants.REQUEST_ITEM_URL;

            ItemRequestInformation itemRequestInformation = getItemRequestInformation();
            itemRequestInformation.setUsername(username);
            itemRequestInformation.setItemBarcodes(Arrays.asList(requestForm.getItemBarcodeInRequest().split(",")));
            itemRequestInformation.setPatronBarcode(requestForm.getPatronBarcodeInRequest());
            itemRequestInformation.setRequestingInstitution(requestForm.getRequestingInstitution());
            itemRequestInformation.setEmailAddress(requestForm.getPatronEmailAddress());
            itemRequestInformation.setTitle(requestForm.getItemTitle());
            itemRequestInformation.setTitleIdentifier(requestForm.getItemTitle());
            itemRequestInformation.setItemOwningInstitution(requestForm.getItemOwningInstitution());
            itemRequestInformation.setRequestType(requestForm.getRequestType());
            itemRequestInformation.setRequestNotes(requestForm.getRequestNotes());
            itemRequestInformation.setStartPage(requestForm.getStartPage());
            itemRequestInformation.setEndPage(requestForm.getEndPage());
            itemRequestInformation.setAuthor(requestForm.getArticleAuthor());
            itemRequestInformation.setChapterTitle(requestForm.getArticleTitle());
            itemRequestInformation.setIssue(requestForm.getIssue());
            if (requestForm.getVolumeNumber() != null) {
                itemRequestInformation.setVolume(requestForm.getVolumeNumber());
            } else {
                itemRequestInformation.setVolume("");
            }

            if (StringUtils.isNotBlank(requestForm.getDeliveryLocationInRequest())) {
                InstitutionEntity institutionEntity = getInstitutionDetailsRepository().findByInstitutionCode(requestForm.getRequestingInstitution());
                DeliveryCodeEntity deliveryCodeEntity = deliveryCodeDetailsRepository.findByDeliveryCodeAndOwningInstitutionIdAndActive(requestForm.getDeliveryLocationInRequest(), institutionEntity.getId(), 'Y');
                if (null != deliveryCodeEntity) {
                    itemRequestInformation.setDeliveryLocation(deliveryCodeEntity.getDeliveryCode());
                }
            }

            HttpEntity<ItemRequestInformation> requestEntity = new HttpEntity<>(itemRequestInformation, getRestHeaderService().getHttpHeaders());
            log.info("Item Request Info DL : {}", itemRequestInformation.getDeliveryLocation());
            ResponseEntity<ItemResponseInformation> itemResponseEntity = getRestTemplate().exchange(requestItemUrl, HttpMethod.POST, requestEntity, ItemResponseInformation.class);
            ItemResponseInformation itemResponseInformation = itemResponseEntity.getBody();
            if (null != itemResponseInformation && !itemResponseInformation.isSuccess()) {
                requestForm.setErrorMessage(itemResponseInformation.getScreenMessage());
                requestForm.setDisableRequestingInstitution(false);
                requestForm.setShowRequestErrorMsg(true);
            }
        } catch (HttpClientErrorException httpException) {
            log.error(ScsbCommonConstants.LOG_ERROR, httpException);
            String responseBodyAsString = httpException.getResponseBodyAsString();
            requestForm.setErrorMessage(responseBodyAsString);
            requestForm.setShowRequestErrorMsg(true);
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            requestForm.setErrorMessage(exception.getMessage());
            requestForm.setShowRequestErrorMsg(true);
        }

        requestForm.setRequestingInstitutions(requestForm.getInstitutionList());
        if (requestForm.getInstitutionList().size() == 1) {
            requestForm.setDisableRequestingInstitution(true);
        }
        if (requestForm.getErrorMessage() == null) {
            requestForm.setSubmitted(true);
            requestForm.setDisableRequestingInstitution(true);
        }
        return requestForm;
    }

    /**
     * Cancel the request which is placed in scsb.
     *
     * @param requestForm the request form
     * @return the string
     */
    @PostMapping("/cancelRequest")
    public String cancelRequest(@RequestBody RequestForm requestForm) {
        JSONObject jsonObject = new JSONObject();
        String requestStatus = null;
        String requestNotes = null;
        try {
            HttpEntity requestEntity = new HttpEntity<>(getRestHeaderService().getHttpHeaders());
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getScsbUrl() + ScsbConstants.URL_REQUEST_CANCEL).queryParam(ScsbCommonConstants.REQUEST_ID, requestForm.getRequestId());
            HttpEntity<CancelRequestResponse> responseEntity = getRestTemplate().exchange(builder.build().encode().toUri(), HttpMethod.POST, requestEntity, CancelRequestResponse.class);
            if(responseEntity != null) {
                CancelRequestResponse cancelRequestResponse = responseEntity.getBody();
                jsonObject.put(ScsbCommonConstants.MESSAGE, cancelRequestResponse.getScreenMessage());
                jsonObject.put(ScsbCommonConstants.STATUS, cancelRequestResponse.isSuccess());
            }
            Optional<RequestItemEntity> requestItemEntity = requestItemDetailsRepository.findById(requestForm.getRequestId());
            if (requestItemEntity.isPresent()) {
                requestStatus = requestItemEntity.get().getRequestStatusEntity().getRequestStatusDescription();
                requestNotes = requestItemEntity.get().getNotes();
            }
            jsonObject.put(ScsbCommonConstants.REQUEST_STATUS, requestStatus);
            jsonObject.put(ScsbConstants.REQUEST_NOTES, requestNotes);
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            log.debug(exception.getMessage());
        }
        return jsonObject.toString();
    }

    /**
     * Resubmit the exception request. Creates a new request with the same data.
     */
    @PostMapping("/resubmitRequest")
    public String resubmitRequest(@RequestBody RequestForm requestForm) {
        JSONObject jsonObject = new JSONObject();
        try {
            ReplaceRequest replaceRequest = new ReplaceRequest();
            replaceRequest.setReplaceRequestByType(ScsbCommonConstants.REQUEST_IDS);
            replaceRequest.setRequestStatus(ScsbConstants.EXCEPTION);
            String requestId = String.valueOf(requestForm.getRequestId());
            replaceRequest.setRequestIds(requestId);
            HttpEntity request = new HttpEntity<>(replaceRequest, getRestHeaderService().getHttpHeaders());
            Map resultMap = getRestTemplate().postForObject(getScsbUrl() + ScsbConstants.URL_REQUEST_RESUBMIT, request, Map.class);
            jsonObject.put(ScsbCommonConstants.BARCODE, requestForm.getItemBarcodeHidden());
            if (resultMap.containsKey(requestId)) {
                String message = (String) resultMap.get(requestId);
                jsonObject.put(ScsbCommonConstants.MESSAGE, resultMap.get(requestId));
                jsonObject.put(ScsbCommonConstants.STATUS, StringUtils.isNotBlank(message) && message.contains(ScsbCommonConstants.SUCCESS));
            } else if (resultMap.containsKey(ScsbCommonConstants.INVALID_REQUEST)) {
                jsonObject.put(ScsbCommonConstants.MESSAGE, resultMap.get(ScsbCommonConstants.INVALID_REQUEST));
                jsonObject.put(ScsbCommonConstants.STATUS, false);
            } else if (resultMap.containsKey(ScsbCommonConstants.FAILURE)) {
                jsonObject.put(ScsbCommonConstants.MESSAGE, resultMap.get(ScsbCommonConstants.FAILURE));
                jsonObject.put(ScsbCommonConstants.STATUS, false);
            }
        } catch (Exception exception) {
            log.error(ScsbCommonConstants.LOG_ERROR, exception);
            log.debug(exception.getMessage());
        }
        log.info(jsonObject.toString());
        return jsonObject.toString();
    }

    private RequestForm searchAndSetResults(RequestForm requestForm) {
        List<SearchResultRow> searchResultRows = new ArrayList<>();
        try {
            Page<RequestItemEntity> requestItemEntities = getRequestServiceUtil().searchRequests(requestForm);
            searchResultRows = buildSearchResultRows(requestItemEntities.getContent(), requestForm);
            if (CollectionUtils.isNotEmpty(searchResultRows)) {
                requestForm.setSearchResultRows(searchResultRows);
                if (!requestItemEntities.isEmpty()) {
                    requestForm.setTotalRecordsCount(NumberFormat.getNumberInstance().format(requestItemEntities.getTotalElements()));
                    requestForm.setTotalPageCount(requestItemEntities.getTotalPages());
                }
            } else {
                requestForm.setSearchResultRows(Collections.emptyList());
                requestForm.setMessage(ScsbCommonConstants.SEARCH_RESULT_ERROR_NO_RECORDS_FOUND);
            }
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        requestForm.setShowResults(true);
        return requestForm;
    }

    private List<SearchResultRow> buildSearchResultRows(List<RequestItemEntity> requestItemEntities, RequestForm requestForm) {
        if (CollectionUtils.isNotEmpty(requestItemEntities)) {
            List<SearchResultRow> searchResultRows = new ArrayList<>();
            for (RequestItemEntity requestItemEntity : requestItemEntities) {
                ItemEntity itemEntity = requestItemEntity.getItemEntity();
                try {
                    if (requestForm.getInstitutionList().size() == 1 && (itemEntity.getInstitutionEntity().getInstitutionCode().equalsIgnoreCase(requestForm.getInstitution()) && !itemEntity.getOwningInstitutionId().equals(requestItemEntity.getRequestingInstitutionId()))) {
                        populateRequestResultsForRecall(searchResultRows, requestItemEntity);
                    } else {
                        populateRequestResults(searchResultRows, requestItemEntity);
                    }
                } catch (Exception e) {
                    log.error(ScsbCommonConstants.LOG_ERROR, e);
                }
            }
            return searchResultRows;
        }
        return Collections.emptyList();
    }

    private void populateRequestResultsForRecall(List<SearchResultRow> searchResultRows, RequestItemEntity requestItemEntity) {
        SearchResultRow searchResultRow = setSearchResultRow(requestItemEntity);
        searchResultRow.setShowItems(false);
        searchResultRow.setImsLocation(requestItemEntity.getItemEntity().getImsLocationEntity().getImsLocationCode());
        setBibData(requestItemEntity, searchResultRow, searchResultRows);
    }

    private void populateRequestResults(List<SearchResultRow> searchResultRows, RequestItemEntity requestItemEntity) {
        SearchResultRow searchResultRow = setSearchResultRow(requestItemEntity);
        searchResultRow.setShowItems(true);
        try {
            searchResultRow.setRequestCreatedBy((userDetailsRepository.findByLoginId(requestItemEntity.getCreatedBy())).getUserDescription());
        } catch (NullPointerException e) {
            searchResultRow.setRequestCreatedBy(requestItemEntity.getCreatedBy());
        }
        if (StringUtils.isNotBlank(requestItemEntity.getEmailId())) {
            searchResultRow.setPatronEmailId(securityUtil.getDecryptedValue(requestItemEntity.getEmailId()));
        } else {
            searchResultRow.setPatronEmailId(requestItemEntity.getEmailId());
        }
        searchResultRow.setPatronBarcode(requestItemEntity.getPatronId());
        searchResultRow.setDeliveryLocation(requestItemEntity.getStopCode());
        searchResultRow.setImsLocation(requestItemEntity.getItemEntity().getImsLocationEntity().getImsLocationCode());
        setBibData(requestItemEntity, searchResultRow, searchResultRows);
    }
    private Integer getPageNumberOnPageSizeChange(RequestForm requestForm) {
        int totalRecordsCount;
        Integer pageNumber = requestForm.getPageNumber();
        try {
            totalRecordsCount = NumberFormat.getNumberInstance().parse(requestForm.getTotalRecordsCount()).intValue();
            int totalPagesCount = (int) Math.ceil((double) totalRecordsCount / (double) requestForm.getPageSize());
            if (totalPagesCount > 0 && pageNumber >= totalPagesCount) {
                pageNumber = totalPagesCount - 1;
            }
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        return pageNumber;
    }

    /**
     * Gets item request information.
     *
     * @return the item request information
     */
    public ItemRequestInformation getItemRequestInformation() {
        return new ItemRequestInformation();
    }

    /**
     * To change the status information of requested item asynchronously in the search request UI page.
     *
     * @param request the request
     * @return the string
     */
    @PostMapping(value = "/refreshStatus")
    public String refreshStatus(@RequestBody String request) {
        return requestService.getRefreshedStatus(request,ScsbConstants.FALSE);
    }

    private RequestForm setFormValuesToDisableSearchInstitution(@Valid @ModelAttribute("requestForm") RequestForm requestForm, UserDetailsForm userDetails, List<String> institutionList) {
        Optional<InstitutionEntity> institutionEntity = getInstitutionDetailsRepository().findById(userDetails.getLoginInstitutionId());
        if (userDetails.isSuperAdmin() || userDetails.isRepositoryUser() || ((institutionEntity.isPresent()) && (institutionEntity.get().getInstitutionCode().equalsIgnoreCase(supportInstitution)))) {
            getRequestService().getInstitutionForSuperAdmin(institutionList);
            requestForm.setInstitutionList(institutionList);
        } else {
            requestForm.setDisableSearchInstitution(true);
            if (institutionEntity.isPresent()) {
                requestForm.setInstitutionList(Arrays.asList(institutionEntity.get().getInstitutionCode()));
                requestForm.setInstitution(institutionEntity.get().getInstitutionCode());
                requestForm.setSearchInstitutionHdn(institutionEntity.get().getInstitutionCode());
            }
        }
        return requestForm;
    }

    /**
     *
     * @param institution
     * @param fromDate
     * @param toDate
     * @return Request Form
     */
    @GetMapping("/exportExceptionReports")
    public ResponseEntity<RequestForm> exportExceptionReports(@RequestParam("institutionCode") String institution, @RequestParam("fromDate") String fromDate, @RequestParam("toDate") String toDate) {
        RequestForm requestForm = new RequestForm();
        return exceptionRports(institution, fromDate, toDate, requestForm, ScsbConstants.IS_EXPORT_TRUE);
    }

    /**
     * @return requestForm
     */
    @GetMapping("/exportExceptionReportsWithDateRange")
    public ResponseEntity<RequestForm> exportExceptionReportsWithDateRange(@RequestParam("institutionCode") String institutionCode, @RequestParam("fromDate") String fromDate, @RequestParam("toDate") String toDate) {
        RequestForm requestForm = new RequestForm();
        return exceptionRports(institutionCode, fromDate, toDate, requestForm, ScsbConstants.IS_EXPORT_FALSE);
    }

    /**
     * @return requestForm
     */
    @GetMapping("/exportExceptionReportsPageSizeChange")
    public ResponseEntity<RequestForm> pageSizeChange(@RequestParam("institutionCode") String institutionCode, @RequestParam("fromDate") String fromDate, @RequestParam("toDate") String toDate, @RequestParam("pageSize") String pageSize) {
        RequestForm requestForm = new RequestForm();
        requestForm.setPageSize(Integer.parseInt(pageSize));
        return exceptionRports(institutionCode, fromDate, toDate, requestForm, ScsbConstants.IS_EXPORT_FALSE);
    }

    /**
     * @return requestForm
     */
    @GetMapping("/exportExceptionNextCall")
    public ResponseEntity<RequestForm> nextCallException(@RequestParam("institutionCode") String institutionCode, @RequestParam("fromDate") String fromDate, @RequestParam("toDate") String toDate, @RequestParam("pageNumber") String pageNumber, @RequestParam("pageSize") String pageSize) {
        RequestForm requestForm = new RequestForm();
        requestForm.setPageNumber(Integer.parseInt(pageNumber));
        requestForm.setPageSize(Integer.parseInt(pageSize));
        return exceptionRports(institutionCode, fromDate, toDate, requestForm, ScsbConstants.IS_EXPORT_FALSE);
    }

    private ResponseEntity<RequestForm> exceptionRports(String institutionCode, String fromDate, String toDate, RequestForm requestForm, boolean isExport) {
        requestForm.setInstitution(institutionCode);
        requestForm.setInstitutionList(institutionDetailsRepository.getInstitutionCodeForSuperAdmin(supportInstitution).stream().map(InstitutionEntity::getInstitutionCode).collect(Collectors.toList()));
        Page<RequestItemEntity> requestItemEntities = null;
        List<SearchResultRow> searchResultRows = null;
        List<RequestItemEntity> requestItemEntitiesList = null;
        Map<String,Date> dateMap = null;
        try {
            dateMap = scsbService.dateFormatter(fromDate,toDate);
            if (!isExport) {
                requestItemEntities = getRequestServiceUtil().exportExceptionReportsWithDate(institutionCode, dateMap.get("fromDate"), dateMap.get("toDate"), requestForm.getPageNumber(), requestForm.getPageSize());
                searchResultRows = buildSearchResultRows(requestItemEntities.getContent(), requestForm);
            } else {
                requestItemEntitiesList = getRequestServiceUtil().exportExceptionReports(institutionCode,dateMap.get("fromDate"), dateMap.get("toDate"));
                searchResultRows = buildSearchResultRows(requestItemEntitiesList, requestForm);
            }
        } catch (Exception e) {
            log.info(ScsbConstants.EXCEPTION_LOGS_REQUEST_EXCEPTIONS, e.getMessage());
        }
        if (CollectionUtils.isNotEmpty(searchResultRows)) {
            if (!isExport) {
                requestForm.setTotalPageCount(requestItemEntities.getTotalPages());
                requestForm.setTotalRecordsCount(NumberFormat.getNumberInstance().format(requestItemEntities.getTotalElements()));
            }
            requestForm.setSearchResultRows(searchResultRows);
        } else {
            requestForm.setSearchResultRows(Collections.emptyList());
            requestForm.setMessage(ScsbCommonConstants.SEARCH_RESULT_ERROR_NO_RECORDS_FOUND);
        }
        requestForm.setShowResults(true);
        return new ResponseEntity<>(requestForm, HttpStatus.OK);
    }

    /**
     *
     * @param transactionReports
     * @return Transaxtion Reports
     */
    @PostMapping("/transactionData")
    public ResponseEntity<TransactionReports> pullTransactionRportCount(@RequestBody TransactionReports transactionReports) {
        return new ResponseEntity<>(transactionReportData(transactionReports), HttpStatus.OK);
    }

    /**
     *
     * @param transactionReports
     * @return Transaxtion Reports
     */
    @PostMapping("/transactionReports")
    public ResponseEntity<TransactionReports>  pullTransactionReports(@RequestBody TransactionReports transactionReports){
        return new ResponseEntity<>(transactionReportData(transactionReports), HttpStatus.OK);
    }
    /**
     *
     * @param transactionReports
     * @return Transaxtion Reports
     */
    @PostMapping("/transactionReportsExport")
    public ResponseEntity<TransactionReports>  pullTransactionReportsExport(@RequestBody TransactionReports transactionReports){
        return new ResponseEntity<>(transactionReportData(transactionReports), HttpStatus.OK);
    }
    private TransactionReports transactionReportData(TransactionReports transactionReports){
        List<TransactionReport> transactionReportsList = null;
        Map<String, Date> dateMap = null;
        try {
            dateMap = scsbService.dateFormatter(transactionReports.getFromDate(), transactionReports.getToDate());
            if ((transactionReports.getTrasactionCallType().equalsIgnoreCase(ScsbConstants.COUNT))) {
                transactionReportsList = getRequestServiceUtil().getTransactionReportCount(transactionReports,dateMap.get("fromDate"), dateMap.get("toDate"));
            } else if((transactionReports.getTrasactionCallType().equalsIgnoreCase(ScsbConstants.REPORTS))){
                transactionReportsList =  getRequestServiceUtil().getTransactionReports(transactionReports,dateMap.get("fromDate"), dateMap.get("toDate"));
            } else {
                transactionReportsList =  getRequestServiceUtil().getTransactionReportsExport(transactionReports,dateMap.get("fromDate"), dateMap.get("toDate"));
            }
        } catch (Exception e) {
            log.info(ScsbConstants.EXCEPTION_LOGS_TRANSACTION,e.getMessage());
        }
        if (CollectionUtils.isNotEmpty(transactionReportsList)) {
            transactionReports.setTransactionReportList(transactionReportsList);
            transactionReports.setTotalPageCount(findTotatlPageCount(transactionReports));
        } else {
            transactionReports.setMessage(ScsbCommonConstants.SEARCH_RESULT_ERROR_NO_RECORDS_FOUND);
        }
        return transactionReports;
    }

    private RequestForm search(RequestForm requestForm) {
        return searchAndSetResults(disableRequestSearchInstitutionDropDown(requestForm));
    }

    private void setBibData(RequestItemEntity requestItemEntity, SearchResultRow searchResultRow, List<SearchResultRow> searchResultRows) {
        ItemEntity itemEntity = requestItemEntity.getItemEntity();
        if (null != itemEntity && CollectionUtils.isNotEmpty(itemEntity.getBibliographicEntities())) {
            searchResultRow.setBibId(itemEntity.getBibliographicEntities().get(0).getId());
        }
        searchResultRows.add(searchResultRow);
    }

    private RequestForm setFormValues(RequestForm requestForm, UserDetailsForm userDetails) {
        List<String> requestStatuses = new ArrayList<>();
        List<String> institutionList = new ArrayList<>();
        getRequestService().findAllRequestStatus(requestStatuses);
        requestForm.setRequestStatuses(requestStatuses);
        return setFormValuesToDisableSearchInstitution(requestForm, userDetails, institutionList);
    }

    private RequestForm setSearch(RequestForm requestForm) {
        requestForm.setPageNumber(0);
        return searchAndSetResults(requestForm);
    }
    private  int findTotatlPageCount(TransactionReports transactionReports){
        return ((transactionReports.getTotalRecordsCount()%transactionReports.getPageSize()) == 0)? (transactionReports.getTotalRecordsCount()/transactionReports.getPageSize()) :
                (transactionReports.getTotalRecordsCount()/transactionReports.getPageSize())+1;
    }
}


