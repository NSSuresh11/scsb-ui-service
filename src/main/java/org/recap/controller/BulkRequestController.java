package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.BulkRequestItemEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.request.DownloadReports;
import org.recap.model.search.BulkRequestForm;
import org.recap.repository.jpa.BulkRequestDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.security.UserManagementService;
import org.recap.service.BulkRequestService;
import org.recap.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by akulak on 19/9/17.
 */
@Slf4j
@RestController
@RequestMapping("/bulkRequest")
public class BulkRequestController extends AbstractController {

    @Autowired
    InstitutionDetailsRepository institutionDetailsRepository;
    @Autowired
    private BulkRequestService bulkRequestService;
    @Autowired
    private BulkRequestDetailsRepository bulkRequestDetailsRepository;

    @Autowired
    private  UserManagementService userManagementService;

    @Autowired
    private RequestService requestService;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    @PostMapping("/loadCreateRequest")
    public BulkRequestForm loadCreateRequest(@RequestBody BulkRequestForm bulkRequestForm) {
        return loadCreateRequestPage(bulkRequestForm);
    }

    @PostMapping("/createBulkRequest")
    public JSONObject createRequest(@RequestParam("file") MultipartFile file, @RequestParam("deliveryLocation") String deliveryLocation
            , @RequestParam("requestingInstitutionId") String requestingInstitutionId
            , @RequestParam("patronBarcodeId") String patronBarcodeId
            , @RequestParam("BulkRequestName") String BulkRequestName
            , @RequestParam("choosenFile") String choosenFile
            , @RequestParam("patronEmailId") String patronEmailId
            , @RequestParam("notes") String notes, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        BulkRequestForm bulkRequestForm = new BulkRequestForm();
        bulkRequestForm.setFile(file);
        bulkRequestForm.setPatronEmailAddress(patronEmailId);
        bulkRequestForm.setFileName(choosenFile);
        bulkRequestForm.setDeliveryLocationInRequest(deliveryLocation);
        bulkRequestForm.setBulkRequestName(BulkRequestName);
        bulkRequestForm.setPatronBarcodeInRequest(patronBarcodeId);
        bulkRequestForm.setRequestingInstitution(requestingInstitutionId);
        bulkRequestForm.setRequestNotes(notes);
        log.info(ScsbConstants.CREATE_BULKREQUEST_CALLED);
        HashMap<String, String> resStatus = new HashMap<>();
        try {
            BulkRequestForm res = bulkRequestService.processCreateBulkRequest(bulkRequestForm, request);
            if (res.getErrorMessage() == null)
                resStatus.put(ScsbConstants.STATUS, ScsbConstants.CREATED);
            else
                resStatus.put(ScsbConstants.STATUS, res.getErrorMessage());
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
            resStatus.put(ScsbConstants.STATUS, e.getMessage());
        }
        JSONObject recvObj = new JSONObject(resStatus);
        return recvObj;
    }

    @PostMapping("/searchRequest")
    public BulkRequestForm searchRequest(@RequestBody BulkRequestForm bulkRequestForm,HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return bulkRequestService.processSearchRequest(bulkRequestForm);
    }


    @PostMapping("/requestPageSizeChange")
    public BulkRequestForm onPageSizeChange(@RequestBody BulkRequestForm bulkRequestForm) {
        return bulkRequestService.processOnPageSizeChange(bulkRequestForm);
    }

    @PostMapping("/first")
    public BulkRequestForm searchFirst(@RequestBody BulkRequestForm bulkRequestForm) {
        bulkRequestForm.setPageNumber(0);
        return bulkRequestService.getPaginatedSearchResults(bulkRequestForm);
    }

    @PostMapping("/previous")
    public BulkRequestForm searchPrevious(@RequestBody BulkRequestForm bulkRequestForm) {
        bulkRequestForm.setPageNumber(bulkRequestForm.getPageNumber() - 1);
        return bulkRequestService.getPaginatedSearchResults(bulkRequestForm);
    }

    @PostMapping("/next")
    public BulkRequestForm searchNext(@RequestBody BulkRequestForm bulkRequestForm) {
        bulkRequestForm.setPageNumber(bulkRequestForm.getPageNumber() + 1);
        return bulkRequestService.getPaginatedSearchResults(bulkRequestForm);
    }

    @PostMapping("/last")
    public BulkRequestForm searchLast(@RequestBody BulkRequestForm bulkRequestForm) {
        bulkRequestForm.setPageNumber(bulkRequestForm.getTotalPageCount() - 1);
        return bulkRequestService.getPaginatedSearchResults(bulkRequestForm);
    }

    @GetMapping("/bulkRequest/goToSearchRequest")
    public ModelAndView searchRequestByPatronBarcode(String patronBarcodeInRequest, Model model) {
        BulkRequestForm bulkRequestForm = new BulkRequestForm();
        bulkRequestForm.setPatronBarcodeSearch(patronBarcodeInRequest);
        bulkRequestService.processSearchRequest(bulkRequestForm);
        loadSearchRequestPage(bulkRequestForm);
        model.addAttribute(ScsbCommonConstants.TEMPLATE, ScsbConstants.BULK_REQUEST);
        return new ModelAndView(ScsbConstants.BULK_REQUEST, ScsbConstants.BULK_REQUEST_FORM, bulkRequestForm);
    }

    @PostMapping("/bulkRequest/loadCreateRequestForSamePatron")
    public ModelAndView loadCreateRequestForSamePatron(@Valid @ModelAttribute("bulkRequestForm") BulkRequestForm bulkRequestForm, Model model) {
        loadCreateRequestPage(bulkRequestForm);
        bulkRequestForm.setSubmitted(false);
        bulkRequestForm.setRequestingInstitution(bulkRequestForm.getRequestingInstituionHidden());
        return processBulkRequest(bulkRequestForm, model);
    }

    @PostMapping("/populateDeliveryLocations")
    public BulkRequestForm populateDeliveryLocations(@RequestBody BulkRequestForm bulkRequestForm) {
        return bulkRequestService.processDeliveryLocations(bulkRequestForm);
    }

    @GetMapping("/{bulkRequestId}")
    public DownloadReports downloadReports(@PathVariable String bulkRequestId) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        BulkRequestItemEntity bulkRequestItemEntity = bulkRequestService.saveUpadatedRequestStatus(Integer.valueOf(bulkRequestId));
        String fileNameWithExtension = "Results_" + StringUtils.substringBefore(bulkRequestItemEntity.getBulkRequestFileName(), ".csv") + dateFormat.format(new Date()) + ".csv";
        DownloadReports downloadReports = new DownloadReports();
        downloadReports.setContent(bulkRequestItemEntity.getBulkRequestFileData());
        downloadReports.setFileName(fileNameWithExtension);
        return downloadReports;
    }

    @PostMapping("/refreshStatus")
    public String refreshStatus(@RequestBody String request) {
        return requestService.getRefreshedStatus(request, ScsbConstants.TRUE);
    }
    private BulkRequestForm loadCreateRequestPage(BulkRequestForm bulkRequestForm) {
        bulkRequestForm.setRequestingInstitutions(getInstitutions());
        return bulkRequestForm;
    }

    private void loadSearchRequestPage(BulkRequestForm bulkRequestForm) {
        bulkRequestForm.setInstitutionList(getInstitutions());
    }

    private List<String> getInstitutions() {
        return institutionDetailsRepository.getInstitutionCodeForSuperAdmin(supportInstitution).stream().map(InstitutionEntity::getInstitutionCode).collect(Collectors.toList());
    }

    private ModelAndView processBulkRequest(BulkRequestForm bulkRequestForm, Model model) {
        bulkRequestService.processDeliveryLocations(bulkRequestForm);
        model.addAttribute(ScsbCommonConstants.TEMPLATE, ScsbConstants.BULK_REQUEST);
        return new ModelAndView(ScsbConstants.BULK_REQUEST, ScsbConstants.BULK_REQUEST_FORM, bulkRequestForm);
    }


}
