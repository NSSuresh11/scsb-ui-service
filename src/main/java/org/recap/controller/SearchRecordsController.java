package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.search.SearchItemResultRow;
import org.recap.model.search.SearchRecordsRequest;
import org.recap.model.search.SearchRecordsResponse;
import org.recap.model.search.SearchResultRow;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.security.UserManagementService;
import org.recap.util.CsvUtil;
import org.recap.util.HelperUtil;
import org.recap.util.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by rajeshbabuk on 6/7/16.
 */

@RestController
@RequestMapping("/search")
@Slf4j
public class SearchRecordsController extends ScsbController {


    @Autowired
    private SearchUtil searchUtil;
    @Autowired
    private CsvUtil csvUtil;
    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private  UserManagementService userManagementService;

    /**
     * Gets search util.
     *
     * @return the search util
     */
    public SearchUtil getSearchUtil() {
        return searchUtil;
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
     * Performs search on solr and returns the results as rows to get displayed in the search UI page.
     *
     * @return the model and view
     */
    @PostMapping("/searchResults")
    public SearchRecordsResponse search(@RequestBody SearchRecordsRequest searchRecordsRequest, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return searchRecordsPage(searchRecordsRequest);
    }

    /**
     * Performs search on solr and returns the previous page results as rows to get displayed in the search UI page.
     *
     * @param searchRecordsRequest the search records request
     * @return the model and view
     */
    @PostMapping("/previous")
    public SearchRecordsResponse searchPrevious(@RequestBody SearchRecordsRequest searchRecordsRequest) {
        return searchRecordsPage(searchRecordsRequest);
    }

    /**
     * Performs search on solr and returns the next page results as rows to get displayed in the search UI page.
     *
     * @param searchRecordsRequest the search records request
     * @return the model and view
     */
    @PostMapping("/next")
    public SearchRecordsResponse searchNext(@RequestBody SearchRecordsRequest searchRecordsRequest) {
        return searchRecordsPage(searchRecordsRequest);
    }

    /**
     * Performs search on solr and returns the first page results as rows to get displayed in the search UI page.
     *
     * @param searchRecordsRequest the search records request
     * @return the model and view
     */
    @PostMapping("/first")
    public SearchRecordsResponse searchFirst(@RequestBody SearchRecordsRequest searchRecordsRequest) {
        searchRecordsRequest.setPageNumber(0);
        return searchUtil.searchRecord(searchRecordsRequest);
    }

    /**
     * Performs search on solr and returns the last page results as rows to get displayed in the search UI page.
     *
     * @param searchRecordsRequest the search records request
     * @return the model and view
     */
    @PostMapping("/last")
    public SearchRecordsResponse searchLast(@RequestBody SearchRecordsRequest searchRecordsRequest) {
        return searchRecordsPage(searchRecordsRequest);
    }

    /**
     * Selected items in the search UI page will be exported to a csv file.
     *
     * @param searchRecordsRequest the search records request
     * @return the byte [ ]
     * @throws Exception the exception
     */
    @PostMapping("/export")
    public byte[] exportRecords(@RequestBody SearchRecordsRequest searchRecordsRequest) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileNameWithExtension = "ExportRecords_" + dateFormat.format(new Date()) + ".csv";
        File csvFile = csvUtil.writeSearchResultsToCsv(searchRecordsRequest.getSearchResultRows(), fileNameWithExtension);
        return HelperUtil.getFileContent(csvFile, fileNameWithExtension, ScsbCommonConstants.SEARCH);
    }

    /**
     * Performs search on solr on changing the page size. The number of results returned will be equal to the selected page size.
     *
     * @param searchRecordsRequest the search records request
     */

    @PostMapping("/pageChanges")
    public SearchRecordsResponse onPageSizeChange(@RequestBody SearchRecordsRequest searchRecordsRequest) {
        Integer pageNumber = searchRecordsRequest.getPageNumber();
        searchRecordsRequest.setPageNumber(0);
        SearchRecordsResponse searchRecordsResponse = searchRecordsPage(searchRecordsRequest);
        if (searchRecordsResponse.getTotalPageCount() > 0 && pageNumber >= searchRecordsResponse.getTotalPageCount()) {
            pageNumber = searchRecordsResponse.getTotalPageCount() - 1;
        }
        searchRecordsRequest.setPageNumber(pageNumber);
        SearchRecordsResponse searchRecordsResponseNew = searchRecordsPage(searchRecordsRequest);
        searchRecordsResponseNew.setPageNumber(pageNumber);
        return searchRecordsResponseNew;
    }

    private void processRequest(SearchRecordsRequest searchRecordsRequest, UserDetailsForm userDetailsForm, RedirectAttributes redirectAttributes) {
        String userInstitution = null;
        Optional<InstitutionEntity> institutionEntity = getInstitutionDetailsRepository().findById(userDetailsForm.getLoginInstitutionId());


        if (institutionEntity.isPresent()) {
            userInstitution = institutionEntity.get().getInstitutionCode();
        }
        List<SearchResultRow> searchResultRows = searchRecordsRequest.getSearchResultRows();
        Set<String> barcodes = new HashSet<>();
        Set<String> itemTitles = new HashSet<>();
        Set<String> itemOwningInstitutions = new HashSet<>();
        Set<String> itemAvailability = new HashSet<>();
        for (SearchResultRow searchResultRow : searchResultRows) {
            if (searchResultRow.isSelected()) {
                if (ScsbCommonConstants.PRIVATE.equals(searchResultRow.getCollectionGroupDesignation()) && !userDetailsForm.isSuperAdmin() && !userDetailsForm.isRepositoryUser() && StringUtils.isNotBlank(userInstitution) && !userInstitution.equals(searchResultRow.getOwningInstitution())) {
                    searchRecordsRequest.setErrorMessage(ScsbConstants.REQUEST_PRIVATE_ERROR_USER_NOT_PERMITTED);
                } else if (!userDetailsForm.isRecapPermissionAllowed()) {
                    searchRecordsRequest.setErrorMessage(ScsbConstants.REQUEST_ERROR_USER_NOT_PERMITTED);
                } else {
                    processBarcodesForSearchResultRow(barcodes, itemTitles, itemOwningInstitutions, searchResultRow, itemAvailability);
                }
            } else if (!CollectionUtils.isEmpty(searchResultRow.getSearchItemResultRows())) {
                for (SearchItemResultRow searchItemResultRow : searchResultRow.getSearchItemResultRows()) {
                    if (searchItemResultRow.isSelectedItem()) {
                        if (ScsbCommonConstants.PRIVATE.equals(searchItemResultRow.getCollectionGroupDesignation()) && !userDetailsForm.isSuperAdmin() && !userDetailsForm.isRepositoryUser() && StringUtils.isNotBlank(userInstitution) && !userInstitution.equals(searchResultRow.getOwningInstitution())) {
                            searchRecordsRequest.setErrorMessage(ScsbConstants.REQUEST_PRIVATE_ERROR_USER_NOT_PERMITTED);
                        } else if (!userDetailsForm.isRecapPermissionAllowed()) {
                            searchRecordsRequest.setErrorMessage(ScsbConstants.REQUEST_ERROR_USER_NOT_PERMITTED);
                        } else {
                            processBarcodeForSearchItemResultRow(barcodes, itemTitles, itemOwningInstitutions, searchItemResultRow, searchResultRow, itemAvailability);
                        }
                    }
                }
            }
        }
        redirectAttributes.addFlashAttribute(ScsbConstants.REQUESTED_BARCODE, StringUtils.join(barcodes, ","));
        redirectAttributes.addFlashAttribute(ScsbConstants.REQUESTED_ITEM_TITLE, StringUtils.join(itemTitles, " || "));
        redirectAttributes.addFlashAttribute(ScsbConstants.REQUESTED_ITEM_OWNING_INSTITUTION, StringUtils.join(itemOwningInstitutions, ","));
        redirectAttributes.addFlashAttribute(ScsbConstants.REQUESTED_ITEM_AVAILABILITY, itemAvailability);
    }

    private void processBarcodeForSearchItemResultRow(Set<String> barcodes, Set<String> titles, Set<String> itemInstitutions, SearchItemResultRow searchItemResultRow, SearchResultRow searchResultRow, Set<String> itemAvailabilty) {
        String barcode = searchItemResultRow.getBarcode();
        processTitleAndItemInstitution(barcodes, titles, itemInstitutions, searchResultRow, barcode, itemAvailabilty);
    }

    private void processBarcodesForSearchResultRow(Set<String> barcodes, Set<String> titles, Set<String> itemInstitutions, SearchResultRow searchResultRow, Set<String> itemAvailabilty) {
        String barcode = searchResultRow.getBarcode();
        processTitleAndItemInstitution(barcodes, titles, itemInstitutions, searchResultRow, barcode, itemAvailabilty);
    }

    private void processTitleAndItemInstitution(Set<String> barcodes, Set<String> titles, Set<String> itemInstitutions, SearchResultRow searchResultRow, String barcode, Set<String> itemAvailabilty) {
        String title = searchResultRow.getTitle();
        String owningInstitution = searchResultRow.getOwningInstitution();
        itemAvailabilty.add(searchResultRow.getAvailability());
        if (StringUtils.isNotBlank(barcode)) {
            barcodes.add(barcode);
        }
        if (StringUtils.isNotBlank(title)) {
            titles.add(title);
        }
        if (StringUtils.isNotBlank(owningInstitution)) {
            itemInstitutions.add(owningInstitution);
        }
    }

    /**
     * Add up for srring thymeleaf and spring binding.
     *
     * @param binder the binder
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(1048576);
    }

    private SearchRecordsResponse searchRecordsPage(SearchRecordsRequest searchRecordsRequest) {
        return searchUtil.searchAndSetResults(searchRecordsRequest);
    }
}
