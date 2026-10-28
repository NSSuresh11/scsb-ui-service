package org.recap.service;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.marc4j.marc.Record;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.controller.AbstractController;
import org.recap.model.jpa.BibliographicEntity;
import org.recap.model.jpa.BulkRequestItemEntity;
import org.recap.model.jpa.DeliveryCodeEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.OwnerCodeEntity;
import org.recap.model.jpa.RequestItemEntity;
import org.recap.model.jpa.RequestStatusEntity;
import org.recap.model.jpa.RequestTypeEntity;
import org.recap.model.request.RequestLogReportRequest;
import org.recap.model.search.RequestForm;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.repository.jpa.BulkRequestDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.OwnerCodeDetailsRepository;
import org.recap.repository.jpa.RequestItemDetailsRepository;
import org.recap.repository.jpa.RequestStatusDetailsRepository;
import org.recap.repository.jpa.RequestTypeDetailsRepository;
import org.recap.util.BibJSONUtil;
import org.recap.util.HelperUtil;
import org.recap.util.PropertyUtil;
import org.recap.util.RequestServiceUtil;
import org.recap.util.UserAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by akulak on 20/4/17.
 */
@Slf4j
@Service
public class RequestService extends AbstractController {


    @Autowired
    private RequestServiceUtil requestServiceUtil;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private RequestTypeDetailsRepository requestTypeDetailsRepository;

    @Autowired
    private OwnerCodeDetailsRepository ownerCodeDetailsRepository;

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private RequestStatusDetailsRepository requestStatusDetailsRepository;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    @Autowired
    private UserAuthUtil userAuthUtil;

    @Autowired
    private RequestService requestService;

    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
    private BulkRequestDetailsRepository bulkRequestDetailsRepository;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    @Nullable
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }

    /**
     * Gets request item details repository.
     *
     * @return the request item details repository
     */
    public RequestItemDetailsRepository getRequestItemDetailsRepository() {
        return requestItemDetailsRepository;
    }

    /**
     * Gets request service util.
     *
     * @return the request service util
     */
    public RequestServiceUtil getRequestServiceUtil() {
        return requestServiceUtil;
    }


    /**
     * Gets user auth util.
     *
     * @return the user auth util
     */
    public UserAuthUtil getUserAuthUtil() {
        return userAuthUtil;
    }

    /**
     * Gets institution details repository.
     *
     * @return the institution details repository
     */
    public InstitutionDetailsRepository getInstitutionDetailsRepository() {
        return institutionDetailsRepository;
    }


    /**
     * Gets request type details repository.
     *
     * @return the request type details repository
     */
    public RequestTypeDetailsRepository getRequestTypeDetailsRepository() {
        return requestTypeDetailsRepository;
    }


    /**
     * Gets customer code details repository.
     *
     * @return the customer code details repository
     */
    public OwnerCodeDetailsRepository getOwnerCodeDetailsRepository() {
        return ownerCodeDetailsRepository;
    }


    /**
     * Gets item details repository.
     *
     * @return the item details repository
     */
    public ItemDetailsRepository getItemDetailsRepository() {
        return itemDetailsRepository;
    }

    /**
     * Gets request service.
     *
     * @return the request service
     */
    public RequestService getRequestService() {
        return requestService;
    }

    /**
     * Gets request status details repository.
     *
     * @return the request status details repository
     */
    public RequestStatusDetailsRepository getRequestStatusDetailsRepository() {
        return requestStatusDetailsRepository;
    }


    /**
     * Get delivery locations from scsb database for the given customer code and
     * populate those values to get displayed in the request UI page for delivery locations drop down.
     *
     * @param requestForm          the request form
     * @param deliveryLocationsMap the delivery locations map
     * @param userDetailsForm      the user details form
     * @param itemEntity           the item entity
     * @param institutionId        the institution id
     */
    public void processCustomerAndDeliveryCodes(RequestForm requestForm, Map<String, String> deliveryLocationsMap, UserDetailsForm userDetailsForm, ItemEntity itemEntity, Integer institutionId) {
        String ownerCode = itemEntity.getCustomerCode();
        OwnerCodeEntity ownerCodeEntity = ownerCodeDetailsRepository.findByOwnerCodeAndInstitutionId(ownerCode, institutionId);
        InstitutionEntity requestingInstitutionEntity = institutionDetailsRepository.findByInstitutionCode(requestForm.getRequestingInstitution());
        if (ownerCodeEntity != null) {
            List<DeliveryCodeEntity> insDeliveryCodeEntities = new ArrayList<>();
            List<Object[]> instDeliveryCodeObjects = ownerCodeDetailsRepository.findInstitutionDeliveryRestrictionsByOwnerCodeIdAndRequestingInstId(ownerCodeEntity.getId(), requestingInstitutionEntity.getId());
            insDeliveryCodeEntities = prepareDeliveryCodeEntities(insDeliveryCodeEntities, instDeliveryCodeObjects);
            addDeliveryLocationsToMap(deliveryLocationsMap, insDeliveryCodeEntities,requestingInstitutionEntity.getInstitutionCode());
            if (userDetailsForm.isRepositoryUser()) {
                List<DeliveryCodeEntity> imsDeliveryCodeEntities = new ArrayList<>();
                List<Object[]> imsDeliveryCodeObjects = ownerCodeDetailsRepository.findImsLocationDeliveryRestrictionsByOwnerCodeIdAndRequestingInstId(ownerCodeEntity.getId(), requestingInstitutionEntity.getId(), itemEntity.getImsLocationId());
                imsDeliveryCodeEntities = prepareDeliveryCodeEntities(imsDeliveryCodeEntities, imsDeliveryCodeObjects);
                addDeliveryLocationsToMap(deliveryLocationsMap, imsDeliveryCodeEntities, requestingInstitutionEntity.getInstitutionCode());
            }
        }
    }

    public List<DeliveryCodeEntity> prepareDeliveryCodeEntities(List<DeliveryCodeEntity> deliveryCodeEntities, List<Object[]> deliveryCodeObjects) {
        for (Object[] obj : deliveryCodeObjects) {
            DeliveryCodeEntity deliveryCodeEntity = new DeliveryCodeEntity();
            deliveryCodeEntity.setId(Integer.parseInt(obj[0].toString()));
            deliveryCodeEntity.setDeliveryCode(obj[1] != null ? obj[1].toString() : null);
            deliveryCodeEntity.setDescription(obj[2] != null ? obj[2].toString() : null);
            deliveryCodeEntity.setAddress(obj[3] != null ? obj[3].toString() : null);
            deliveryCodeEntity.setOwningInstitutionId(obj[4] != null ? Integer.parseInt(obj[4].toString()) : null);
            deliveryCodeEntity.setImsLocationId(obj[5] != null ? Integer.parseInt(obj[5].toString()) : null);
            deliveryCodeEntity.setDeliveryCodeTypeId(obj[6] != null ? Integer.parseInt(obj[6].toString()) : null);
            deliveryCodeEntities.add(deliveryCodeEntity);
        }
        return deliveryCodeEntities;
    }

    private void addDeliveryLocationsToMap(Map<String, String> deliveryLocationsMap, List<DeliveryCodeEntity> deliveryCodeEntities, String institution) {
        String suppressLocation = propertyUtil.getPropertyByInstitutionAndKey(institution, PropertyKeyConstants.UI_SUPPRESS_DELIVERY_LOCATION);

        if (CollectionUtils.isNotEmpty(deliveryCodeEntities)) {
            Collections.sort(deliveryCodeEntities);
            for (DeliveryCodeEntity deliveryCodeEntity : deliveryCodeEntities) {
                if (deliveryCodeEntity != null) {
                    deliveryLocationsMap.put(deliveryCodeEntity.getDeliveryCode(), deliveryCodeEntity.getDescription());
                }
            }
        }
        if (suppressLocation != null && suppressLocation.trim().length() > 0) {
            String[] locationArr = suppressLocation.split(",");
            for (String location : locationArr) {
                deliveryLocationsMap.remove(location);
            }
        }
    }

    /**
     * Sort the given delivery locations in natural order.
     *
     * @param deliveryLocationsMap the delivery locations map
     * @return the map
     */
    public Map<String, String> sortDeliveryLocations(Map<String, String> deliveryLocationsMap) {
        LinkedHashMap<String, String> sortedDeliverLocationMap = new LinkedHashMap<>();
        Set<Map.Entry<String, String>> entries = deliveryLocationsMap.entrySet();
        Comparator<Map.Entry<String, String>> valueComparator = (e1, e2) -> {
            String v1 = e1.getValue();
            String v2 = e2.getValue();
            return v1.compareTo(v2);
        };
        List<Map.Entry<String, String>> listOfEntries = new ArrayList<>(entries);
        Collections.sort(listOfEntries, valueComparator);
        LinkedHashMap<String, String> sortedByValue = new LinkedHashMap<>(listOfEntries.size());
        for (Map.Entry<String, String> entry : listOfEntries) {
            sortedByValue.put(entry.getKey(), entry.getValue());
        }
        Set<Map.Entry<String, String>> entrySetSortedByValue = sortedByValue.entrySet();
        for (Map.Entry<String, String> mapping : entrySetSortedByValue) {
            sortedDeliverLocationMap.put(mapping.getKey(), mapping.getValue());
        }
        return sortedDeliverLocationMap;
    }

    public String getRefreshedStatus(String request, boolean isBulkRequestRefresh) {
        JSONObject jsonObject = new JSONObject();
        Map<Integer, Integer> map = new HashMap<>();
        Map<String, String> responseMap = new HashMap<>();
        Map<String, String> responseMapForNotes = new HashMap<>();
        Map<String, String> responseMapForFileNames = new HashMap<>();
        List<Integer> requestIdList = new ArrayList<>();
        try {
            List<String> listOfRequestStatusDesc = (isBulkRequestRefresh == ScsbConstants.TRUE) ? bulkRequestDetailsRepository.findAllBulkRequestStatusDescExceptProcessing() : getRequestStatusDetailsRepository().findAllRequestStatusDescExceptProcessing();
            JSONObject requestJson = new JSONObject(request);
            JSONArray parameterValues = (JSONArray) requestJson.get(ScsbConstants.STATUS);
            for (int i = 0; i < parameterValues.length(); i++) {
                String parameterValue = (String) parameterValues.get(i);
                String[] split = StringUtils.split(parameterValue, "-");
                map.put(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
                requestIdList.add(Integer.valueOf(split[0]));
            }
            if(isBulkRequestRefresh) {
                List<BulkRequestItemEntity> requestItemEntityList = bulkRequestDetailsRepository.findByIdIn(requestIdList);
                for (BulkRequestItemEntity bulkRequestItemEntity : requestItemEntityList) {
                    Integer rowUpdateNum = map.get(bulkRequestItemEntity.getId());
                    for (String requestStatusDescription : listOfRequestStatusDesc) {
                        if (requestStatusDescription.equals(bulkRequestItemEntity.getBulkRequestStatus())) {
                            responseMap.put(String.valueOf(rowUpdateNum), bulkRequestItemEntity.getBulkRequestStatus());
                            responseMapForNotes.put(String.valueOf(rowUpdateNum), bulkRequestItemEntity.getNotes());
                            responseMapForFileNames.put(String.valueOf(rowUpdateNum), bulkRequestItemEntity.getBulkRequestFileName());
                        }
                    }
                }
                jsonObject.put(ScsbConstants.FILE_NAMES,responseMapForFileNames);
            } else {
                List<RequestItemEntity> requestItemEntityList = getRequestItemDetailsRepository().findByIdIn(requestIdList);
                for (RequestItemEntity requestItemEntity : requestItemEntityList) {
                    Integer rowUpdateNum = map.get(requestItemEntity.getId());
                    for (String requestStatusDescription : listOfRequestStatusDesc) {
                        if (requestStatusDescription.equals(requestItemEntity.getRequestStatusEntity().getRequestStatusDescription())) {
                            responseMap.put(String.valueOf(rowUpdateNum), requestItemEntity.getRequestStatusEntity().getRequestStatusDescription());
                            responseMapForNotes.put(String.valueOf(rowUpdateNum), requestItemEntity.getNotes());
                        }
                    }
                }
            }
            jsonObject.put(ScsbCommonConstants.STATUS, responseMap);
            jsonObject.put(ScsbCommonConstants.NOTES, responseMapForNotes);
        } catch (JSONException e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        return jsonObject.toString();
    }

    /**
     * When an item barcode is submitted to place a request in the request UI page , this method populates the information about that item in the request UI page.
     *
     * @param requestForm the request form
     * @return the string
     * @throws JSONException the json exception
     */
    public String populateItemForRequest(RequestForm requestForm, HttpServletRequest request) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        Boolean multipleItemBarcodes = false;
        Boolean isRecallAvailable = false;
        Boolean isRecallAvailableforRequestingInst = true;

        Map<String, String> deliveryLocationsMap = new LinkedHashMap<>();
        Map<String, String> frozenInstitutionPropertyMap = propertyUtil.getPropertyByKeyForAllInstitutions(PropertyKeyConstants.ILS.ILS_ENABLE_CIRCULATION_FREEZE);
        if (StringUtils.isNotBlank(requestForm.getItemBarcodeInRequest())) {
            List<String> itemBarcodes = Arrays.asList(requestForm.getItemBarcodeInRequest().split(","));
            if (itemBarcodes.size() > 1) {
                multipleItemBarcodes = true;
            }
            List<String> invalidBarcodes = new ArrayList<>();
            List<String> notAvailableBarcodes = new ArrayList<>();
            List<String> frozenBarcodes = new ArrayList<>();
            Set<String> itemTitles = new HashSet<>();
            var storageLocations = new HashSet<String>();
            Set<String> itemOwningInstitutions = new HashSet<>();
            LinkedHashSet<String> requestTypes = new LinkedHashSet<>();
            Boolean showEDD = false;
            UserDetailsForm userDetailsForm;
            for (String itemBarcode : itemBarcodes) {
                String barcode = itemBarcode.trim();
                if (StringUtils.isNotBlank(barcode)) {
                    List<ItemEntity> itemEntities = getItemDetailsRepository().findByBarcodeAndCatalogingStatusAndIsDeletedFalse(barcode, ScsbCommonConstants.COMPLETE_STATUS);
                    if (CollectionUtils.isNotEmpty(itemEntities)) {
                        for (ItemEntity itemEntity : itemEntities) {
                            OwnerCodeEntity ownerCodeEntity = getOwnerCodeDetailsRepository().findByOwnerCodeAndRecapDeliveryRestrictionLikeEDD(itemEntity.getCustomerCode(), itemEntity.getOwningInstitutionId());
                            if (ownerCodeEntity != null) {
                                showEDD = true;
                            }
                            if (null != itemEntity && CollectionUtils.isNotEmpty(itemEntity.getBibliographicEntities())) {
                                boolean isCirculationFreezeEnabled = Boolean.parseBoolean(frozenInstitutionPropertyMap.get(itemEntity.getInstitutionEntity().getInstitutionCode()));
                                if (isCirculationFreezeEnabled) {
                                    frozenBarcodes.add(barcode);
                                } else {
                                    userDetailsForm = getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.REQUEST_PRIVILEGE);
                                    if ((itemEntity.getCollectionGroupId().equals(ScsbConstants.CGD_PRIVATE)) && (!userDetailsForm.isSuperAdmin()) && (!userDetailsForm.isRepositoryUser()) && (!userDetailsForm.getLoginInstitutionId().equals(itemEntity.getOwningInstitutionId()))) {
                                        jsonObject.put(ScsbConstants.NO_PERMISSION_ERROR_MESSAGE, ScsbConstants.REQUEST_PRIVATE_ERROR_USER_NOT_PERMITTED);
                                        return jsonObject.toString();
                                    } else if (!userDetailsForm.isRecapPermissionAllowed()) {
                                        jsonObject.put(ScsbConstants.NO_PERMISSION_ERROR_MESSAGE, ScsbConstants.REQUEST_ERROR_USER_NOT_PERMITTED);
                                        return jsonObject.toString();
                                    } else {
                                        if (null != itemEntity.getItemStatusEntity() && itemEntity.getItemStatusEntity().getStatusCode().equals(ScsbCommonConstants.NOT_AVAILABLE)) {
                                            notAvailableBarcodes.add(barcode);
                                        }
                                        Integer institutionId = itemEntity.getInstitutionEntity().getId();
                                        String institutionCode = itemEntity.getInstitutionEntity().getInstitutionCode();
                                        requestForm.setItemOwningInstitution(institutionCode);
                                        for (BibliographicEntity bibliographicEntity : itemEntity.getBibliographicEntities()) {
                                            String bibContent = new String(bibliographicEntity.getContent());
                                            BibJSONUtil bibJSONUtil = new BibJSONUtil();
                                            List<Record> records = bibJSONUtil.convertMarcXmlToRecord(bibContent);
                                            Record marcRecord = records.get(0);
                                            itemTitles.add(bibJSONUtil.getTitle(marcRecord));
                                            itemOwningInstitutions.add(institutionCode);
                                            storageLocations.add(itemEntity.getImsLocationEntity().getImsLocationCode());
                                        }
                                        if (StringUtils.isNotBlank(requestForm.getRequestingInstituionHidden())) {
                                            String replaceReqInst = requestForm.getRequestingInstituionHidden();
                                            requestForm.setRequestingInstitution(replaceReqInst);
                                        }
                                        if ("true".equals(requestForm.getOnChange()) && StringUtils.isNotBlank(requestForm.getRequestingInstitution())) {
                                            InstitutionEntity requestingInstitutionEntity = institutionDetailsRepository.findByInstitutionCode(requestForm.getRequestingInstitution());
                                            Map<String, String> recallAvailablePropertyMap = propertyUtil.getPropertyByKeyForAllInstitutions(PropertyKeyConstants.ILS.ILS_RECALL_FUNCTIONALITY_AVAILABLE);
                                            isRecallAvailableforRequestingInst = Boolean.parseBoolean(recallAvailablePropertyMap.get(requestingInstitutionEntity.getInstitutionCode()));
                                            getRequestService().processCustomerAndDeliveryCodes(requestForm, deliveryLocationsMap, userDetailsForm, itemEntity, institutionId);
                                            deliveryLocationsMap = sortDeliveryLocationForRecapUser(deliveryLocationsMap, userDetailsForm);
                                        }
                                        Map<String, String> recalAvailablePropertyMap = propertyUtil.getPropertyByKeyForAllInstitutions(PropertyKeyConstants.ILS.ILS_RECALL_FUNCTIONALITY_AVAILABLE);
                                        isRecallAvailable = Boolean.parseBoolean(recalAvailablePropertyMap.get(institutionCode));

                                    }
                                }
                            }
                        }
                    } else {
                        invalidBarcodes.add(barcode);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(itemTitles)) {
                jsonObject.put(ScsbConstants.REQUESTED_ITEM_TITLE, StringUtils.join(itemTitles, " || "));
            }
            if (CollectionUtils.isNotEmpty(itemOwningInstitutions)) {
                jsonObject.put(ScsbConstants.REQUESTED_ITEM_OWNING_INSTITUTION, StringUtils.join(itemOwningInstitutions, ","));
            }
            if (CollectionUtils.isNotEmpty(storageLocations)) {
                jsonObject.put(ScsbConstants.REQUESTED_ITEM_STORAGE_LOCATION, StringUtils.join(storageLocations, ","));
            }
            if (!multipleItemBarcodes && CollectionUtils.isNotEmpty(notAvailableBarcodes) && (isRecallAvailable && isRecallAvailableforRequestingInst)) {
                requestForm.setRequestType(ScsbCommonConstants.RECALL);
                requestTypes.add(requestForm.getRequestType());
            }
            if ((!multipleItemBarcodes && showEDD) && !(ScsbCommonConstants.RECALL.equals(requestForm.getRequestType()))) {
                List<RequestTypeEntity> requestTypeEntities = getRequestTypeDetailsRepository().findAllExceptBorrowDirect();
                for (RequestTypeEntity requestTypeEntity : requestTypeEntities) {
                    requestTypes.add(requestTypeEntity.getRequestTypeCode());
                }
                if(!(isRecallAvailable && isRecallAvailableforRequestingInst) || CollectionUtils.isEmpty(notAvailableBarcodes)) {
                    requestTypes.remove(ScsbCommonConstants.RECALL);
                }

            } else if (!(ScsbCommonConstants.RECALL.equals(requestForm.getRequestType()))) {
                List<RequestTypeEntity> requestTypeEntityList = getRequestTypeDetailsRepository().findAllExceptEDDAndBorrowDirect();
                for (RequestTypeEntity requestTypeEntity : requestTypeEntityList) {
                    requestTypes.add(requestTypeEntity.getRequestTypeCode());
                }
                if(!(isRecallAvailable && isRecallAvailableforRequestingInst) || CollectionUtils.isEmpty(notAvailableBarcodes)) {
                    requestTypes.remove(ScsbCommonConstants.RECALL);
                }
            }
            if (!multipleItemBarcodes && CollectionUtils.isNotEmpty(notAvailableBarcodes) && !(isRecallAvailable && isRecallAvailableforRequestingInst)) {
                requestTypes = new LinkedHashSet<>();
            }
            jsonObject.put(ScsbConstants.REQUEST_TYPES, requestTypes);
            jsonObject.put(ScsbConstants.REQUEST_TYPE, !requestTypes.isEmpty() ? requestTypes.iterator().next() : "");
            jsonObject.put(ScsbConstants.SHOW_EDD, showEDD);
            jsonObject.put(ScsbConstants.MULTIPLE_BARCODES, multipleItemBarcodes);

            if (CollectionUtils.isNotEmpty(frozenBarcodes)) {
                jsonObject.put(ScsbConstants.NOT_AVAILABLE_FROZEN_ITEMS_ERROR_MESSAGE, ScsbConstants.BARCODES_NOT_AVAILABLE + " - " + StringUtils.join(frozenBarcodes, ",") + ". " + ScsbConstants.OWNING_INST_CIRCULATION_FREEZE_ERROR);
            }
            if (CollectionUtils.isNotEmpty(invalidBarcodes)) {
                jsonObject.put(ScsbConstants.ERROR_MESSAGE, ScsbCommonConstants.BARCODES_NOT_FOUND + " - " + StringUtils.join(invalidBarcodes, ","));
            }
            if (CollectionUtils.isNotEmpty(notAvailableBarcodes)) {
                jsonObject.put(ScsbConstants.NOT_AVAILABLE_ERROR_MESSAGE, ScsbConstants.BARCODES_NOT_AVAILABLE + " - " + StringUtils.join(notAvailableBarcodes, ","));
            }
            if (null != deliveryLocationsMap) {
                jsonObject.put(ScsbConstants.DELIVERY_LOCATION, deliveryLocationsMap);
            }
        } else {
            String replaceReqInst = requestForm.getRequestingInstitution().replace(",", "");
            if (StringUtils.isBlank(replaceReqInst)) {
                deliveryLocationsMap.put("", "");
                jsonObject.put(ScsbConstants.DELIVERY_LOCATION, deliveryLocationsMap);
            }
        }
        return jsonObject.toString();
    }

    private Map<String, String> sortDeliveryLocationForRecapUser(Map<String, String> deliveryLocationsMap, UserDetailsForm userDetailsForm) {
        if (userDetailsForm.isRepositoryUser()) {
            deliveryLocationsMap = getRequestService().sortDeliveryLocations(deliveryLocationsMap);
        }
        return deliveryLocationsMap;
    }

    /**
     * When requesting a item in search UI page, this method populates information about that item in the request UI page.
     *
     * @param model           the model
     * @param request         the request
     * @param userDetailsForm the user details form
     * @return the form details for request
     * @throws JSONException the json exception
     */
    public RequestForm setFormDetailsForRequest(Model model, HttpServletRequest request, UserDetailsForm userDetailsForm) throws JSONException {
        RequestForm requestForm = setDefaultsToCreateRequest(userDetailsForm);
        Object requestedBarcode = ((BindingAwareModelMap) model).get(ScsbConstants.REQUESTED_BARCODE);
        if (requestedBarcode != null) {
            requestForm.setOnChange("true");
            requestForm.setItemBarcodeInRequest((String) requestedBarcode);
            String stringJson = populateItemForRequest(requestForm, request);
            if (stringJson != null) {
                JSONObject jsonObject = new JSONObject(stringJson);
                Object itemTitle = jsonObject.has(ScsbConstants.REQUESTED_ITEM_TITLE) ? jsonObject.get(ScsbConstants.REQUESTED_ITEM_TITLE) : null;
                Object itemOwningInstitution = jsonObject.has(ScsbConstants.REQUESTED_ITEM_OWNING_INSTITUTION) ? jsonObject.get(ScsbConstants.REQUESTED_ITEM_OWNING_INSTITUTION) : null;
                Object deliveryLocations = jsonObject.has(ScsbConstants.DELIVERY_LOCATION) ? jsonObject.get(ScsbConstants.DELIVERY_LOCATION) : null;
                Object requestTypes = jsonObject.has(ScsbConstants.REQUEST_TYPES) ? jsonObject.get(ScsbConstants.REQUEST_TYPES) : null;
                List<OwnerCodeEntity> customerCodeEntities = new ArrayList<>();
                prepareRequestForm(requestForm, itemTitle, itemOwningInstitution, deliveryLocations, requestTypes, customerCodeEntities);
            }
        }
        return requestForm;
    }

    /**
     * When the request UI page is loaded, this method populates default values to request type drop down and
     * based on the logged in user it populates value to the requesting institution drop down.
     *
     * @param userDetailsForm the user details form
     * @return the defaults to create request
     */
    public RequestForm setDefaultsToCreateRequest(UserDetailsForm userDetailsForm) {
        RequestForm requestForm = new RequestForm();
        Boolean addOnlyRecall = false;
        Boolean addAllRequestType = false;
        Model model = new BindingAwareModelMap();
        // Object availability = true;
        Object availability = ((BindingAwareModelMap) model).get(ScsbConstants.REQUESTED_ITEM_AVAILABILITY);
        if (availability != null) {
            HashSet<String> str = (HashSet<String>) availability;
            for (String itemAvailability : str) {
                if (ScsbCommonConstants.NOT_AVAILABLE.equalsIgnoreCase(itemAvailability)) {
                    Optional<InstitutionEntity> loginInstitution = institutionDetailsRepository.findById(userDetailsForm.getLoginInstitutionId());
                    if (loginInstitution.isPresent()) {
                        Map<String, String> recalAvailablePropertyMap = propertyUtil.getPropertyByKeyForAllInstitutions(PropertyKeyConstants.ILS.ILS_RECALL_FUNCTIONALITY_AVAILABLE);
                        boolean isRecallAvailable = Boolean.parseBoolean(recalAvailablePropertyMap.get(loginInstitution.get().getInstitutionCode()));
                        if (isRecallAvailable) {
                            addOnlyRecall = true;
                        }
                    }
                }
                if (ScsbCommonConstants.AVAILABLE.equalsIgnoreCase(itemAvailability)) {
                    addAllRequestType = true;
                }
            }
        }

        List<String> requestingInstitutions = new ArrayList<>();
        List<String> requestTypes = new ArrayList<>();

        Iterable<InstitutionEntity> institutionEntities = getInstitutionDetailsRepository().findAll();
        for (Iterator iterator = institutionEntities.iterator(); iterator.hasNext(); ) {
            InstitutionEntity institutionEntity = (InstitutionEntity) iterator.next();
            if (userDetailsForm.getLoginInstitutionId().equals(institutionEntity.getId()) && (!userDetailsForm.isRepositoryUser()) && (!userDetailsForm.isSuperAdmin()) && (!supportInstitution.equalsIgnoreCase(institutionEntity.getInstitutionCode()))) {
                requestingInstitutions.add(institutionEntity.getInstitutionCode());
                requestForm.setRequestingInstitutions(requestingInstitutions);
                requestForm.setInstitutionList(requestingInstitutions);
                requestForm.setRequestingInstitution(institutionEntity.getInstitutionCode());
                requestForm.setRequestingInstituionHidden(institutionEntity.getInstitutionCode());
                requestForm.setDisableRequestingInstitution(true);
                requestForm.setOnChange("true");
            }
            if ((userDetailsForm.isRepositoryUser() || userDetailsForm.isSuperAdmin()) && (!supportInstitution.equalsIgnoreCase(institutionEntity.getInstitutionCode()))) {
                requestingInstitutions.add(institutionEntity.getInstitutionCode());
                requestForm.setRequestingInstitutions(requestingInstitutions);
                requestForm.setInstitutionList(requestingInstitutions);
                requestForm.setRequestingInstitution("");
                requestForm.setDisableRequestingInstitution(false);
            }
        }

        if (addOnlyRecall && !addAllRequestType) {
            RequestTypeEntity requestTypeEntity = getRequestTypeDetailsRepository().findByRequestTypeCode(ScsbCommonConstants.RECALL);
            requestTypes.add(requestTypeEntity.getRequestTypeCode());
            requestForm.setRequestType(requestTypeEntity.getRequestTypeCode());
            requestForm.setRequestTypes(requestTypes);
        }
        if (!addOnlyRecall || addAllRequestType) {
            Iterable<RequestTypeEntity> requestTypeEntities = getRequestTypeDetailsRepository().findAll();
            for (Iterator iterator = requestTypeEntities.iterator(); iterator.hasNext(); ) {
                RequestTypeEntity requestTypeEntity = (RequestTypeEntity) iterator.next();
                if (!ScsbCommonConstants.BORROW_DIRECT.equals(requestTypeEntity.getRequestTypeCode())) {
                    requestTypes.add(requestTypeEntity.getRequestTypeCode());
                }
            }
            requestForm.setRequestType(ScsbCommonConstants.RETRIEVAL);
            requestForm.setRequestTypes(requestTypes);
        }
        return requestForm;
    }

    /**
     * Adds all the request status description in scsb into the request statuses list.
     *
     * @param requestStatuses the request statuses
     */
    public void findAllRequestStatusExceptProcessing(List<String> requestStatuses) {
        Iterable<RequestStatusEntity> requestStatusEntities = getRequestStatusDetailsRepository().findAllExceptProcessing();
        for (Iterator iterator = requestStatusEntities.iterator(); iterator.hasNext(); ) {
            RequestStatusEntity requestStatusEntity = (RequestStatusEntity) iterator.next();
            requestStatuses.add(requestStatusEntity.getRequestStatusDescription());
        }
    }

    /**
     * Adds all the request status description in scsb into the request statuses list.
     *
     * @param requestStatuses the request statuses
     */
    public void findAllRequestStatus(List<String> requestStatuses) {
        Iterable<RequestStatusEntity> requestStatusEntities = getRequestStatusDetailsRepository().findAll();
        for (Iterator iterator = requestStatusEntities.iterator(); iterator.hasNext(); ) {
            RequestStatusEntity requestStatusEntity = (RequestStatusEntity) iterator.next();
            requestStatuses.add(requestStatusEntity.getRequestStatusDescription());
        }
    }

    /**
     * Adds the institution code into the institution list for super admin role.
     *
     * @param institutionList the institution list
     */
    public void getInstitutionForSuperAdmin(List<String> institutionList) {
        Iterable<InstitutionEntity> institutionEntities = getInstitutionDetailsRepository().getInstitutionCodeForSuperAdmin(supportInstitution);
        for (Iterator iterator = institutionEntities.iterator(); iterator.hasNext(); ) {
            InstitutionEntity institutionEntity = (InstitutionEntity) iterator.next();
            institutionList.add(institutionEntity.getInstitutionCode());
        }
    }

    public void prepareRequestForm(RequestForm requestForm, Object itemTitle, Object itemOwningInstitution, Object deliveryLocations, Object requestTypes, List<OwnerCodeEntity> customerCodeEntities) throws JSONException {
        List<String> requestTypeList = new ArrayList<>();
        if (itemTitle != null && itemOwningInstitution != null && deliveryLocations != null) {
            requestForm.setItemTitle((String) itemTitle);
            requestForm.setItemOwningInstitution((String) itemOwningInstitution);
            JSONObject deliveryLocationsJson = (JSONObject) deliveryLocations;
            Iterator iterator = deliveryLocationsJson.keys();
            while (iterator.hasNext()) {
                String customerCode = (String) iterator.next();
                String description = (String) deliveryLocationsJson.get(customerCode);
                OwnerCodeEntity customerCodeEntity = new OwnerCodeEntity();
                customerCodeEntity.setOwnerCode(customerCode);
                customerCodeEntity.setDescription(description);
                customerCodeEntities.add(customerCodeEntity);
            }
            requestForm.setDeliveryLocations(customerCodeEntities);
        }
        if (!(ScsbCommonConstants.RECALL.equals(requestForm.getRequestType())) && requestTypes != null) {
            JSONArray requestTypeArray = (JSONArray) requestTypes;
            for (int i = 0; i < requestTypeArray.length(); i++) {
                requestTypeList.add(requestTypeArray.getString(i));
            }
            requestForm.setRequestTypes(requestTypeList);
        }
    }

    public RequestLogReportRequest getRequestReports(RequestLogReportRequest requestLogReportRequest) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = HelperUtil.getSwaggerHeaders();
        HttpEntity<RequestLogReportRequest> httpEntity = new HttpEntity<>(requestLogReportRequest, headers);
        String requestLogUrl = getScsbUrl() + ScsbConstants.REQUEST_ITEM_LOG_URL;
        ResponseEntity<RequestLogReportRequest> requestLogResponse = null;
        try {
            requestLogResponse = restTemplate.exchange(requestLogUrl, HttpMethod.POST, httpEntity, RequestLogReportRequest.class);
        } catch (Exception e) {
            RequestLogReportRequest requestLogReportResponse = new RequestLogReportRequest();
            requestLogReportResponse.setStatus("No records found");
            return requestLogReportResponse;
        }
        return requestLogResponse.getBody();
    }

    public RequestLogReportRequest submitRequestReports(RequestLogReportRequest requestLogReportRequest) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = HelperUtil.getSwaggerHeaders();
        HttpEntity<RequestLogReportRequest> httpEntity = new HttpEntity<>(requestLogReportRequest, headers);
        String requestLogUrl = getScsbUrl() + ScsbConstants.REQUEST_ITEM_LOG_SUBMIT_URL;
        ResponseEntity<RequestLogReportRequest> requestLogSumitResponse = restTemplate.exchange(requestLogUrl, HttpMethod.POST, httpEntity, RequestLogReportRequest.class);
        return requestLogSumitResponse.getBody();
    }
}
