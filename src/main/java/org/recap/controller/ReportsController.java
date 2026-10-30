package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.facets.FacetsForm;
import org.recap.model.jpa.CollectionGroupEntity;
import org.recap.model.jpa.ImsLocationEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.reports.TitleMatchedReport;
import org.recap.model.request.DownloadReports;
import org.recap.model.search.DeaccessionItemResultsRow;
import org.recap.model.search.IncompleteReportResultsRow;
import org.recap.model.search.ReportsForm;
import org.recap.model.submitCollection.SubmitCollectionReport;
import org.recap.repository.jpa.CollectionGroupDetailsRepository;
import org.recap.repository.jpa.ImsLocationDetailRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.security.UserManagementService;
import org.recap.service.SCSBService;
import org.recap.util.HelperUtil;
import org.recap.util.ReportsServiceUtil;
import org.recap.util.ReportsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by rajeshbabuk on 13/10/16.
 */
@Slf4j
@RestController
@RequestMapping("/reports")
public class ReportsController extends AbstractController {



    @Autowired
    private ReportsUtil reportsUtil;

    @Autowired
    private ReportsServiceUtil reportsServiceUtil;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private  UserManagementService userManagementService;

    @Autowired
    private ImsLocationDetailRepository imsLocationDetailRepository;

    @Autowired
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;

    @Autowired
    private SCSBService scsbService;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    private static final String FROM_DATE = "fromDate";
    private static final String TO_DATE = "toDate";


    /**
     * Gets reports util.
     *
     * @return the reports util
     */
    public ReportsUtil getReportsUtil() {
        return reportsUtil;
    }

    /**
     * Get the item count for requested, accessioned and deaccessioned report.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/submit")
    public ReportsForm reportCounts(@RequestBody ReportsForm reportsForm,HttpServletRequest request) throws Exception {
        if (reportsForm.getRequestType().equalsIgnoreCase(ScsbCommonConstants.REPORTS_REQUEST)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ScsbCommonConstants.SIMPLE_DATE_FORMAT_REPORTS);
            Date requestFromDate = simpleDateFormat.parse(reportsForm.getRequestFromDate());
            Date requestToDate = simpleDateFormat.parse(reportsForm.getRequestToDate());
            Date fromDate = scsbService.getFromDate(requestFromDate);
            Date toDate = scsbService.getToDate(requestToDate);
            if (reportsForm.getShowBy().equalsIgnoreCase(ScsbCommonConstants.REPORTS_PARTNERS)) {
                reportsUtil.populatePartnersCountForRequest(reportsForm, fromDate, toDate);
            } else if (reportsForm.getShowBy().equalsIgnoreCase(ScsbCommonConstants.REPORTS_REQUEST_TYPE)) {
                reportsUtil.populateRequestTypeInformation(reportsForm, fromDate, toDate);
            }
        } else if (reportsForm.getRequestType().equalsIgnoreCase(ScsbCommonConstants.REPORTS_ACCESSION_DEACCESSION)) {
            reportsUtil.populateAccessionDeaccessionItemCounts(reportsForm);

        } else if ("CollectionGroupDesignation".equalsIgnoreCase(reportsForm.getRequestType())) {
            reportsUtil.populateCGDItemCounts(reportsForm);
        }
        return reportsForm;
    }

    /**
     * Get the item count for collection group designation report.
     *
     * @return the model and view
     * @throws Exception the exception
     */
    @GetMapping("/collectionGroupDesignation")
    public ReportsForm cgdCounts() throws Exception {
        ReportsForm reportsForm = new ReportsForm();
        return reportsUtil.populateCGDItemCounts(reportsForm);
    }

    /**
     * Get deaccessioned item results from scsb solr and display them as rows in the deaccession report UI page.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/deaccessionInformation")
    public ReportsForm deaccessionInformation(@RequestBody ReportsForm reportsForm) throws Exception {
        return daccessionItemResults(reportsForm);
    }


    /**
     * Get first page deaccessioned or incomplete item results from scsb solr and display them as rows in the deaccession report or incomplete report.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/first")
    public ReportsForm searchFirst(@RequestBody ReportsForm reportsForm) throws Exception {
        if ((ScsbConstants.REPORTS_INCOMPLETE_RECORDS).equals(reportsForm.getRequestType())) {
            reportsForm.setIncompletePageNumber(0);
            return getIncompleteRecords(reportsForm);
        } else {
            reportsForm.setPageNumber(0);
            return setReportData(reportsForm);
        }
    }

    /**
     * Get previous page deaccessioned or incomplete item results from scsb solr and display them as rows in the deaccession report or incomplete report.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/previous")
    public ReportsForm searchPrevious(@RequestBody ReportsForm reportsForm) throws Exception {
        if ((ScsbConstants.REPORTS_INCOMPLETE_RECORDS).equals(reportsForm.getRequestType())) {
            reportsForm.setIncompletePageNumber(reportsForm.getIncompletePageNumber() - 1);
            return getIncompleteRecords(reportsForm);
        } else {
            reportsForm.setPageNumber(reportsForm.getPageNumber() - 1);
            return setReportData(reportsForm);
        }
    }


    /**
     * Get next page deaccessioned or incomplete item results from scsb solr and display them as rows in the deaccession report or incomplete report.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/next")
    public ReportsForm searchNext(@RequestBody ReportsForm reportsForm) throws Exception {
        if ((ScsbConstants.REPORTS_INCOMPLETE_RECORDS).equals(reportsForm.getRequestType())) {
            reportsForm.setIncompletePageNumber(reportsForm.getIncompletePageNumber() + 1);
            return getIncompleteRecords(reportsForm);
        } else {
            reportsForm.setPageNumber(reportsForm.getPageNumber() + 1);
            return setReportData(reportsForm);
        }
    }


    /**
     * Get last page deaccessioned or incomplete item results from scsb solr and display them as rows in the deaccession report or incomplete report.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/last")
    public ReportsForm searchLast(@RequestBody ReportsForm reportsForm) throws Exception {
        if ((ScsbConstants.REPORTS_INCOMPLETE_RECORDS).equals(reportsForm.getRequestType())) {
            reportsForm.setIncompletePageNumber(reportsForm.getIncompleteTotalPageCount() - 1);
            return getIncompleteRecords(reportsForm);
        } else {
            reportsForm.setPageNumber(reportsForm.getTotalPageCount() - 1);
            return setReportData(reportsForm);
        }
    }

    /**
     * Get incomplete item results from scsb solr and display them as rows in the incomplete report UI page.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/incompleteRecords")
    public ReportsForm incompleteRecordsReport(@RequestBody ReportsForm reportsForm) throws Exception {
        reportsForm.setIncompletePageNumber(0);
        return getIncompleteRecords(reportsForm);

    }

    /**
     * To generate institution drop down values in the incomplete report UI page.
     *
     * @return the institution for incomplete report
     */
    @GetMapping("/getInstitutions")
    public FacetsForm getInstitutionForIncompleteReport() {
        FacetsForm facetsForm = new FacetsForm();
        List<String> instList = new ArrayList<>();
        List<String> storageLocationsList = new ArrayList<>();
        List<String> cgdCodesList;
        List<InstitutionEntity> institutionCodeForSuperAdmin = institutionDetailsRepository.getInstitutionCodeForSuperAdmin(supportInstitution);
        List<ImsLocationEntity> imsLocationEntities = imsLocationDetailRepository.findAll();
        List<CollectionGroupEntity> collectionGroupEntities = collectionGroupDetailsRepository.findAll();
        for(ImsLocationEntity imsLocationEntity: imsLocationEntities){
            if(!imsLocationEntity.getImsLocationCode().equalsIgnoreCase(ScsbConstants.FACETS_UN))
                storageLocationsList.add(imsLocationEntity.getImsLocationCode());
        }
        for (InstitutionEntity institutionEntity : institutionCodeForSuperAdmin) {
            instList.add(institutionEntity.getInstitutionCode());
        }
        cgdCodesList = scsbService.pullCGDCodesList(collectionGroupEntities);
        facetsForm.setInstitutionList(instList);
        facetsForm.setStorageLocationsList(storageLocationsList);
        facetsForm.setCgdCodesList(cgdCodesList);
        return facetsForm;
    }

    /**
     * To export the incomplete report results to a csv file.
     *
     * @param reportsForm the reports form
     * @return the byte [ ]
     * @throws Exception the exception
     */
    @PostMapping("/export")
    public DownloadReports exportIncompleteRecords(@RequestBody ReportsForm reportsForm) throws Exception {
        DownloadReports downloadReports = new DownloadReports();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileNameWithExtension = ScsbConstants.REPORTS_INCOMPLETE_EXPORT_FILE_NAME + reportsForm.getIncompleteRequestingInstitution() + "_" + dateFormat.format(new Date()) + ".csv";
        reportsForm.setExport(true);
        List<IncompleteReportResultsRow> incompleteReportResultsRows = reportsUtil.incompleteRecordsReportFieldsInformation(reportsForm);
        IncompleteReportResultsRow incompleteReportResultsRow = new IncompleteReportResultsRow();
        incompleteReportResultsRows.add(incompleteReportResultsRow);
        File csvFile = reportsUtil.exportIncompleteRecords(incompleteReportResultsRows, fileNameWithExtension);
        byte[] fileContent = HelperUtil.getFileContent(csvFile, fileNameWithExtension, ScsbCommonConstants.REPORTS);
        downloadReports.setContent(fileContent);
        downloadReports.setFileName(fileNameWithExtension);
        return downloadReports;
    }

    /**
     * Based on the selected page size search results will display the results in the incomplete report UI page.
     *
     * @param reportsForm the reports form
     * @return the model and view
     * @throws Exception the exception
     */
    @PostMapping("/incompleteReportPageSizeChange")
    public ReportsForm incompleteReportPageSizeChange(@RequestBody ReportsForm reportsForm) throws Exception {
        if ((ScsbConstants.REPORTS_INCOMPLETE_RECORDS).equals(reportsForm.getRequestType())) {
            reportsForm.setIncompletePageNumber(0);
            return getIncompleteRecords(reportsForm);
        } else {
            reportsForm.setPageNumber(0);
            return setReportData(reportsForm);
        }
    }

    @PostMapping("/submitCollcetionReport")
    public ResponseEntity<SubmitCollectionReport> submitCollectionReport(@RequestBody SubmitCollectionReport submitCollectionReprot, @RequestParam(FROM_DATE) String fromDate, @RequestParam(TO_DATE) String toDate) throws Exception {
        Map<String, Date> dateMap = scsbService.dateFormatter(fromDate, toDate);
        submitCollectionReprot.setFrom(dateMap.get(FROM_DATE));
        submitCollectionReprot.setTo(dateMap.get(TO_DATE));
        return reportsUtil.submitCollectionReport(submitCollectionReprot);
    }

    @PostMapping("/accessionReport")
    public ResponseEntity<SubmitCollectionReport> accessionReport(@RequestBody SubmitCollectionReport submitCollectionReprot, @RequestParam(FROM_DATE) String fromDate, @RequestParam(TO_DATE) String toDate) throws Exception {
        Map<String, Date> dateMap = scsbService.dateFormatter(fromDate, toDate);
        submitCollectionReprot.setFrom(dateMap.get(FROM_DATE));
        submitCollectionReprot.setTo(dateMap.get(TO_DATE));
        return reportsUtil.accessionReport(submitCollectionReprot);
    }

    /**
     *
     * @param titleMatchedReport
     * @param fromDate
     * @param toDate
     * @return List of TitleMatchReports
     * @throws Exception
     */
    @PostMapping("/titleMatchCount")
    public TitleMatchedReport titleMatchCount(@RequestBody TitleMatchedReport titleMatchedReport, @RequestParam(FROM_DATE) String fromDate, @RequestParam(TO_DATE) String toDate) throws Exception {
        Map<String, Date> dateMap = scsbService.dateFormatter(fromDate, toDate);
        titleMatchedReport.setFromDate(dateMap.get(FROM_DATE));
        titleMatchedReport.setToDate(dateMap.get(TO_DATE));
        return reportsServiceUtil.getTitleMatchReport(titleMatchedReport,ScsbConstants.TRUE,ScsbConstants.FALSE);
    }

    /**
     *
     * @param titleMatchedReport
     * @param fromDate
     * @param toDate
     * @return List of TitleMatchReports
     * @throws Exception
     */
    @PostMapping("/titleMatchReports")
    public TitleMatchedReport titleMatchReports(@RequestBody TitleMatchedReport titleMatchedReport, @RequestParam(FROM_DATE) String fromDate, @RequestParam(TO_DATE) String toDate) throws Exception {
        Map<String, Date> dateMap = scsbService.dateFormatter(fromDate, toDate);
        titleMatchedReport.setFromDate(dateMap.get(FROM_DATE));
        titleMatchedReport.setToDate(dateMap.get(TO_DATE));
        return reportsServiceUtil.getTitleMatchReport(titleMatchedReport,ScsbConstants.FALSE,ScsbConstants.FALSE);
    }

    /**
     *
     * @param titleMatchedReport
     * @param fromDate
     * @param toDate
     * @return List of TitleMatchReports
     * @throws Exception
     */
    @PostMapping("/titleMatchReportExport")
    public TitleMatchedReport titleMatchReportsExport(@RequestBody TitleMatchedReport titleMatchedReport, @RequestParam(FROM_DATE) String fromDate, @RequestParam(TO_DATE) String toDate) throws Exception {
        Map<String, Date> dateMap = scsbService.dateFormatter(fromDate, toDate);
        titleMatchedReport.setFromDate(dateMap.get(FROM_DATE));
        titleMatchedReport.setToDate(dateMap.get(TO_DATE));
        return reportsServiceUtil.getTitleMatchReport(titleMatchedReport,ScsbConstants.FALSE,ScsbConstants.TRUE);
    }

    private ReportsForm getIncompleteRecords(ReportsForm reportsForm) throws Exception {
        List<IncompleteReportResultsRow> incompleteReportResultsRows = getReportsUtil().incompleteRecordsReportFieldsInformation(reportsForm);
        if (incompleteReportResultsRows.isEmpty()) {
            reportsForm.setShowIncompleteResults(false);
            reportsForm.setErrorMessage(ScsbConstants.REPORTS_INCOMPLETE_RECORDS_NOT_FOUND);
        } else {
            reportsForm.setShowIncompleteResults(true);
            reportsForm.setShowIncompletePagination(true);
            reportsForm.setIncompleteReportResultsRows(incompleteReportResultsRows);
        }
        return reportsForm;
    }



    private ReportsForm setReportData(ReportsForm reportsForm) throws Exception {
        return daccessionItemResults(reportsForm);
    }

    private ReportsForm daccessionItemResults(ReportsForm reportsForm) throws Exception {
        List<DeaccessionItemResultsRow> deaccessionItemResultsRowList = getReportsUtil().deaccessionReportFieldsInformation(reportsForm);
        reportsForm.setDeaccessionItemResultsRows(deaccessionItemResultsRowList);
        return reportsForm;
    }
}
