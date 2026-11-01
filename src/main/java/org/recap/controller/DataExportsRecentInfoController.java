package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.dataexportinfo.DataExportResponse;
import org.recap.model.dataexportinfo.S3RecentDataExportInfoList;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.security.UserManagementService;
import org.recap.util.HelperUtil;
import org.recap.util.UserAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dataExport")
public class DataExportsRecentInfoController {

    @Autowired
    HomeController homeController;
    @Value("${" + PropertyKeyConstants.SCSB_ETL_URL + "}")
    private String scsbEtlUrl;
    @Value("${" + PropertyKeyConstants.SCSB_GATEWAY_URL + "}")
    private String scsbUrl;
    @Autowired
    private UserAuthUtil userAuthUtil;

    @Autowired
    private  UserManagementService userManagementService;

    @GetMapping("/getRecentDataExportsInfo")
    public S3RecentDataExportInfoList getRecentDataExportsInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        S3RecentDataExportInfoList s3RecentDataExportInfoList = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = HelperUtil.getSwaggerHeaders();
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<S3RecentDataExportInfoList> responseEntity = restTemplate.exchange(scsbEtlUrl + ScsbConstants.SCSB_DATA_EXPORT_RECENT_INFO_URL, HttpMethod.GET, httpEntity, S3RecentDataExportInfoList.class);
            if (responseEntity.getBody() != null && responseEntity.getStatusCode().is2xxSuccessful()) {
                s3RecentDataExportInfoList = responseEntity.getBody();
            }
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e.getMessage());
        }
        return s3RecentDataExportInfoList;
    }

    @GetMapping("/exportDataDump")
    public DataExportResponse exportDataDump(@RequestParam(value = "institutionCodes", required = true) String institutionCodes,
                                             @RequestParam(value = "requestingInstitutionCode", required = true) String requestingInstitutionCode,
                                             @RequestParam(value = "imsDepositoryCodes", required = false) String imsDepositoryCodes,
                                             @RequestParam(value = "fetchType", required = true) String fetchType,
                                             @RequestParam(value = "outputFormat", required = true) String outputFormat,
                                             @RequestParam(value = "date", required = false) String date,
                                             @RequestParam(value = "collectionGroupIds", required = false) String collectionGroupIds,
                                             @RequestParam(value = "transmissionType", required = false) String transmissionType,
                                             @RequestParam(value = "emailToAddress", required = false) String emailToAddress,
                                             @RequestParam(value = "userName", required = false) String userName,HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        DataExportResponse dataExportResponse = new DataExportResponse();
        Map<String, String> inputMap = new HashMap<>();
        setInputMapValues(inputMap, institutionCodes, requestingInstitutionCode, fetchType, outputFormat, date, collectionGroupIds, transmissionType, emailToAddress, imsDepositoryCodes, userName);
        try {
            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(HelperUtil.getSwaggerHeaders());
            ResponseEntity<String> responseEntity = new RestTemplate().exchange(scsbUrl + ScsbConstants.SCSB_DATA_DUMP_URL + "?institutionCodes={institutionCodes}&requestingInstitutionCode={requestingInstitutionCode}&imsDepositoryCodes={imsDepositoryCodes}&fetchType={fetchType}&outputFormat={outputFormat}&date={date}&collectionGroupIds={collectionGroupIds}&transmissionType={transmissionType}&emailToAddress={emailToAddress}&userName={userName}", HttpMethod.GET, httpEntity, String.class, inputMap);
            if (responseEntity.getBody() != null && responseEntity.getStatusCode().is2xxSuccessful()) {
                dataExportResponse.setMessage(responseEntity.getBody());
            } else {
                dataExportResponse.setErrorMessage(responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e.getMessage());
            dataExportResponse.setErrorMessage(e.getMessage());
        }
        return dataExportResponse;
    }

    private void setInputMapValues(Map<String, String> inputMap, String institutionCodes, String requestingInstitutionCode, String fetchType,
                                   String outputFormat, String date, String collectionGroupIds, String transmissionType, String emailToAddress, String imsDepositoryCodes, String userName) {
        inputMap.put("institutionCodes", institutionCodes);
        inputMap.put("requestingInstitutionCode", requestingInstitutionCode);
        inputMap.put("fetchType", fetchType);
        inputMap.put("outputFormat", outputFormat);
        inputMap.put("date", date);
        inputMap.put("collectionGroupIds", collectionGroupIds);
        inputMap.put("transmissionType", transmissionType);
        inputMap.put("emailToAddress", emailToAddress);
        inputMap.put("imsDepositoryCodes", imsDepositoryCodes);
        inputMap.put("userName", userName);
    }

    /**
     * @return data dump export fields descriptions
     */

    @GetMapping("/getDescriptions")
    public Map<String, String> dataExportDescriptionFields() {
        Map<String, String> instDescriptions = new LinkedHashMap<>();
        List<InstitutionEntity> InstitutionCodes = homeController.fecthingInstituionsFromDB();
        String institutionDescription = "";
        for (InstitutionEntity institutionEntity : InstitutionCodes) {
            institutionDescription += (institutionEntity.getInstitutionCode() + " = " + institutionEntity.getInstitutionName());
            institutionDescription += (InstitutionCodes.indexOf(institutionEntity) != InstitutionCodes.size() - 1) ? ", " : "";
        }
        instDescriptions.put("desc", institutionDescription);
        return instDescriptions;
    }
}
