package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.MarcException;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.DeliveryCodeEntity;
import org.recap.model.jpa.ItemEntity;
import org.recap.model.jpa.RequestItemEntity;
import org.recap.model.search.BibliographicMarcForm;
import org.recap.model.search.CollectionForm;
import org.recap.model.search.SearchItemResultRow;
import org.recap.model.search.SearchRecordsRequest;
import org.recap.model.search.SearchRecordsResponse;
import org.recap.model.search.SearchResultRow;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.repository.jpa.CollectionGroupDetailsRepository;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.RequestItemDetailsRepository;
import org.recap.repository.jpa.UserDetailsRepository;
import org.recap.security.UserManagementService;
import org.recap.service.SCSBService;
import org.recap.util.CollectionServiceUtil;
import org.recap.util.MarcRecordViewUtil;
import org.recap.util.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rajeshbabuk on 12/10/16.
 */
@Slf4j
@RestController
@RequestMapping("/collection")
public class CollectionController extends AbstractController {



    @Autowired
    private SearchUtil searchUtil;

    @Autowired
    private MarcRecordViewUtil marcRecordViewUtil;

    @Autowired
    private CollectionServiceUtil collectionServiceUtil;

    @Autowired
    private  UserManagementService userManagementService;

    @Autowired
    private RequestItemDetailsRepository requestItemDetailsRepository;

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;

    @Autowired
    SCSBService scsbService;

    /**
     * Gets marc record view util.
     *
     * @return the marc record view util
     */
    public MarcRecordViewUtil getMarcRecordViewUtil() {
        return marcRecordViewUtil;
    }

    /**
     * Gets collection service util.
     *
     * @return the collection service util
     */
    public CollectionServiceUtil getCollectionServiceUtil() {
        return collectionServiceUtil;
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
     * Perform search on solr based on the item barcodes and returns the results as rows to get displayed in the collection UI page.
     *
     * @param collectionForm the collection form
     * @return CollectionForm
     * @throws Exception the exception
     */
    @PostMapping("/displayRecords")
    public CollectionForm displayRecords(@RequestBody CollectionForm collectionForm, HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(false);
        return searchAndSetResults(collectionForm);
    }

    /**
     * Upon click on the title in the search result row in collection UI page, a popup box is opened with marc information as well as to perform collection updates.
     *
     * @param collectionForm the collection form
     * @return CollectionForm
     * @throws Exception the exception
     */
    @PostMapping("/openMarcView")
    public CollectionForm openMarcView(@RequestBody CollectionForm collectionForm, HttpServletRequest request) throws MarcException {
        log.info(ScsbConstants.COLLECTION_OMV_CALLED);
        UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(request.getSession(false), ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE);
        BibliographicMarcForm bibliographicMarcForm = getMarcRecordViewUtil().buildBibliographicMarcForm(collectionForm.getBibId(), collectionForm.getItemId(), userDetailsForm);
        CollectionForm collectionFormUpdated = populateCollectionForm(collectionForm, bibliographicMarcForm);
        collectionFormUpdated.setAllowCGDandDeaccession(checkIsCGDandDeaccessionRestricted(collectionForm.getBarcode(),request));
        return collectionFormUpdated;
    }

    /**
     * To perform operations update cgd or deaccession for the selected item in the collection UI page.
     *
     * @param collectionForm the collection form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/collectionUpdate")
    public CollectionForm collectionUpdate(@RequestBody CollectionForm collectionForm, HttpServletRequest request) throws Exception {
        log.info(ScsbConstants.COLLECTION_UPDATE_CALLED);
        HttpSession session = request.getSession(false);
        String username = (String) session.getAttribute(ScsbConstants.USER_NAME);
        collectionForm.setUsername(username);
        if (ScsbCommonConstants.UPDATE_CGD.equalsIgnoreCase(collectionForm.getCollectionAction())) {
            getCollectionServiceUtil().updateCGDForItem(collectionForm);
        } else if (ScsbCommonConstants.DEACCESSION.equalsIgnoreCase(collectionForm.getCollectionAction())) {
            getCollectionServiceUtil().deAccessionItem(collectionForm);
        }
        collectionForm.setAllowEdit(true);
        return collectionForm;
    }

    /**
     * This method is to check whether the item is cross instituion borrowed while performing collection update.
     *
     * @param collectionForm the collection form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/checkCrossInstitutionBorrowed")
    public CollectionForm checkCrossInstitutionBorrowed(@RequestBody CollectionForm collectionForm) {
        log.info(ScsbConstants.COLLECTION_CCIB_CALLED);
        String itemBarcode = collectionForm.getBarcode();
        String warningMessage = null;
        List<ItemEntity> itemEntities = itemDetailsRepository.findByBarcode(itemBarcode);
        int owningInstitutionId = !itemEntities.isEmpty() ? itemEntities.get(0).getOwningInstitutionId() : 0;
        List<DeliveryCodeEntity> deliveryLocations = marcRecordViewUtil.getDeliveryLocationsList(collectionForm.getCustomerCode(), owningInstitutionId);
        collectionForm.setDeliveryLocations(deliveryLocations);
        RequestItemEntity activeRetrievalRequest = getRequestItemDetailsRepository().findByItemBarcodeAndRequestStaCode(itemBarcode, ScsbCommonConstants.REQUEST_STATUS_RETRIEVAL_ORDER_PLACED);
        RequestItemEntity activeRecallRequest = getRequestItemDetailsRepository().findByItemBarcodeAndRequestStaCode(itemBarcode, ScsbCommonConstants.REQUEST_STATUS_RECALLED);
        if (null != activeRetrievalRequest && null != activeRecallRequest) {
            warningMessage = ScsbConstants.WARNING_MESSAGE_REQUEST_BORROWED_ITEM;
        } else if (null != activeRetrievalRequest && null == activeRecallRequest) {
            String itemOwningInstitution = activeRetrievalRequest.getItemEntity().getInstitutionEntity().getInstitutionCode();
            String retrievalRequestingInstitution = activeRetrievalRequest.getInstitutionEntity().getInstitutionCode();
            if (!itemOwningInstitution.equalsIgnoreCase(retrievalRequestingInstitution)) {
                warningMessage = ScsbConstants.WARNING_MESSAGE_RETRIEVAL_CROSS_BORROWED_ITEM;
            } else {
                warningMessage = ScsbConstants.WARNING_MESSAGE_RETRIEVAL_BORROWED_ITEM;
            }
        } else if (null == activeRetrievalRequest && null != activeRecallRequest) {
            String itemOwningInstitution = activeRecallRequest.getItemEntity().getInstitutionEntity().getInstitutionCode();
            String recallRequestingInstitution = activeRecallRequest.getInstitutionEntity().getInstitutionCode();
            if (!itemOwningInstitution.equalsIgnoreCase(recallRequestingInstitution)) {
                warningMessage = ScsbConstants.WARNING_MESSAGE_RECALL_CROSS_BORROWED_ITEM;
            } else {
                warningMessage = ScsbConstants.WARNING_MESSAGE_RECALL_BORROWED_ITEM;
            }
        }

        if (StringUtils.isNotBlank(warningMessage)) {
            if (ScsbCommonConstants.UPDATE_CGD.equalsIgnoreCase(collectionForm.getCollectionAction())) {
                collectionForm.setWarningMessage(warningMessage);
            } else if (ScsbCommonConstants.DEACCESSION.equalsIgnoreCase(collectionForm.getCollectionAction())) {
                collectionForm.setWarningMessage(warningMessage + " " + ScsbConstants.WARNING_MESSAGE_DEACCESSION_REQUEST_BORROWED_ITEM);
            }
        }
        return collectionForm;
    }
    private boolean checkIsCGDandDeaccessionRestricted (String barcode,HttpServletRequest request){
        HttpSession session = request.getSession(false);
        String username = (String) session.getAttribute(ScsbConstants.USER_NAME);
        String userCode = userDetailsRepository.findInstitutionCodeByUserName(username);
        String itemCode = itemDetailsRepository.findInstitutionCodeByBarcode(barcode);
        List<String> userRoles = userDetailsRepository.getUserRoles(username);
        return scsbService.validateUserRoles(userRoles,userCode,itemCode);
    }
    private CollectionForm searchAndSetResults(CollectionForm collectionForm) throws Exception {
        return buildMissingBarcodes(buildResultRows(trimBarcodes(collectionForm)));
    }

    private String limitedBarcodes(CollectionForm collectionForm) {
        String[] barcodeArray = collectionForm.getItemBarcodes().split(",");
        if (barcodeArray.length > ScsbCommonConstants.BARCODE_LIMIT) {
            String[] limitBarcodeArray = Arrays.copyOfRange(barcodeArray, 0, ScsbCommonConstants.BARCODE_LIMIT);
            collectionForm.setIgnoredBarcodesErrorMessage(ScsbConstants.BARCODE_LIMIT_ERROR + " - " + StringUtils.join(Arrays.copyOfRange(barcodeArray, ScsbCommonConstants.BARCODE_LIMIT, barcodeArray.length), ","));
            return StringUtils.join(limitBarcodeArray, ",");
        }
        return StringUtils.join(barcodeArray, ",");
    }

    private CollectionForm trimBarcodes(CollectionForm collectionForm) {
        List<String> barcodeList = new ArrayList<>();
        String[] barcodeArray = collectionForm.getItemBarcodes().split(",");
        for (String barcode : barcodeArray) {
            if (StringUtils.isNotBlank(barcode)) {
                String itemBarcode = barcode.trim();
                barcodeList.add(itemBarcode);
            }
        }
        collectionForm.setItemBarcodes(StringUtils.join(barcodeList, ","));
        return collectionForm;
    }

    private CollectionForm buildResultRows(CollectionForm collectionForm) {
        if (StringUtils.isNotBlank(collectionForm.getItemBarcodes())) {
            SearchRecordsRequest searchRecordsRequest = new SearchRecordsRequest();
            searchRecordsRequest.setFieldName(ScsbCommonConstants.BARCODE);
            searchRecordsRequest.setFieldValue(limitedBarcodes(collectionForm));
            List<CollectionGroupEntity> collectionGroupEntities = collectionGroupDetailsRepository.findAll();
            searchRecordsRequest.setCollectionGroupDesignations(scsbService.pullCGDCodesList(collectionGroupEntities));
            searchRecordsRequest.setBarcodeSearch(false);
            SearchRecordsResponse searchRecordsResponse = searchUtil.requestSearchResults(searchRecordsRequest);
            List<SearchResultRow> searchResultRows = searchRecordsResponse.getSearchResultRows();
            collectionForm.setSearchResultRows(Collections.emptyList());
            if (CollectionUtils.isNotEmpty(searchResultRows)) {
                collectionForm.setSearchResultRows(searchResultRows);
                collectionForm.setSelectAll(false);
            }
        } else {
            collectionForm.setErrorMessage(ScsbCommonConstants.NO_RESULTS_FOUND);
        }
        collectionForm.setShowResults(true);
        return collectionForm;
    }

    private CollectionForm buildMissingBarcodes(CollectionForm collectionForm) {
        Set<String> missingBarcodes = getMissingBarcodes(collectionForm);
        if (CollectionUtils.isNotEmpty(missingBarcodes)) {
            collectionForm.setBarcodesNotFoundErrorMessage(ScsbCommonConstants.BARCODES_NOT_FOUND + " - " + StringUtils.join(missingBarcodes, ","));
        }
        return collectionForm;
    }

    private Set<String> getMissingBarcodes(CollectionForm collectionForm) {
        if (StringUtils.isNotBlank(collectionForm.getItemBarcodes())) {
            String[] barcodeArray = collectionForm.getItemBarcodes().split(",");
            if (barcodeArray.length > ScsbCommonConstants.BARCODE_LIMIT) {
                barcodeArray = Arrays.copyOfRange(barcodeArray, 0, ScsbCommonConstants.BARCODE_LIMIT);
            }
            Set<String> missingBarcodes = new HashSet<>(Arrays.asList(barcodeArray));
            for (SearchResultRow searchResultRow : collectionForm.getSearchResultRows()) {
                String barcode = searchResultRow.getBarcode();
                if (StringUtils.isBlank(barcode) && CollectionUtils.isNotEmpty(searchResultRow.getSearchItemResultRows())) {
                    SearchItemResultRow searchItemResultRow = searchResultRow.getSearchItemResultRows().get(0);
                    barcode = searchItemResultRow.getBarcode();
                    searchResultRow.setBarcode(barcode);
                    searchResultRow.setItemId(searchItemResultRow.getItemId());
                    searchResultRow.setCollectionGroupDesignation(searchItemResultRow.getCollectionGroupDesignation());
                }
                missingBarcodes.remove(barcode);
            }
            return missingBarcodes;
        }
        return Collections.emptySet();
    }

    private CollectionForm populateCollectionForm(CollectionForm collectionForm, BibliographicMarcForm bibliographicMarcForm) {
        collectionForm.setTitle(bibliographicMarcForm.getTitle());
        collectionForm.setAuthor(bibliographicMarcForm.getAuthor());
        collectionForm.setPublisher(bibliographicMarcForm.getPublisher());
        collectionForm.setPublishedDate(bibliographicMarcForm.getPublishedDate());
        collectionForm.setOwningInstitution(bibliographicMarcForm.getOwningInstitution());
        collectionForm.setCallNumber(bibliographicMarcForm.getCallNumber());
        collectionForm.setMonographCollectionGroupDesignation(bibliographicMarcForm.getMonographCollectionGroupDesignation());
        collectionForm.setTag000(bibliographicMarcForm.getTag000());
        collectionForm.setControlNumber001(bibliographicMarcForm.getControlNumber001());
        collectionForm.setControlNumber005(bibliographicMarcForm.getControlNumber005());
        collectionForm.setControlNumber008(bibliographicMarcForm.getControlNumber008());
        collectionForm.setBibDataFields(bibliographicMarcForm.getBibDataFields());
        collectionForm.setAvailability(bibliographicMarcForm.getAvailability());
        collectionForm.setBarcode(bibliographicMarcForm.getBarcode());
        collectionForm.setLocationCode(bibliographicMarcForm.getLocationCode());
        collectionForm.setUseRestriction(bibliographicMarcForm.getUseRestriction());
        collectionForm.setCollectionGroupDesignation(bibliographicMarcForm.getCollectionGroupDesignation());
        collectionForm.setNewCollectionGroupDesignation(bibliographicMarcForm.getNewCollectionGroupDesignation());
        collectionForm.setCgdChangeNotes(bibliographicMarcForm.getCgdChangeNotes());
        collectionForm.setCustomerCode(bibliographicMarcForm.getCustomerCode());
        collectionForm.setDeaccessionType(bibliographicMarcForm.getDeaccessionType());
        collectionForm.setDeaccessionNotes(bibliographicMarcForm.getDeaccessionNotes());
        collectionForm.setDeliveryLocation(bibliographicMarcForm.getDeliveryLocation());
        collectionForm.setShared(bibliographicMarcForm.isShared());
        collectionForm.setSubmitted(bibliographicMarcForm.isSubmitted());
        collectionForm.setMessage(bibliographicMarcForm.getMessage());
        collectionForm.setErrorMessage(bibliographicMarcForm.getErrorMessage());
        collectionForm.setWarningMessage(bibliographicMarcForm.getWarningMessage());
        collectionForm.setCollectionAction(bibliographicMarcForm.getCollectionAction());
        collectionForm.setLeaderMaterialType(bibliographicMarcForm.getLeaderMaterialType());
        collectionForm.setLocationCode(bibliographicMarcForm.getLocationCode());
        collectionForm.setShowModal(true);
        collectionForm.setShowResults(true);
        collectionForm.setAllowEdit(bibliographicMarcForm.isAllowEdit());
        return collectionForm;
    }

}
