package org.recap.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.BulkRequestInformation;
import org.recap.model.jpa.BulkCustomerCodeEntity;
import org.recap.model.jpa.BulkRequestItemEntity;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.RequestItemEntity;
import org.recap.model.jpa.UsersEntity;
import org.recap.model.search.BulkRequestForm;
import org.recap.model.search.BulkRequestResponse;
import org.recap.model.search.BulkSearchResultRow;
import org.recap.repository.jpa.BulkCustomerCodeDetailsRepository;
import org.recap.repository.jpa.BulkRequestDetailsRepository;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.UserDetailsRepository;
import org.recap.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by akulak on 22/9/17.
 */
@Slf4j
@Service
public class BulkRequestService {



    @Autowired
    private BulkRequestDetailsRepository bulkRequestDetailsRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private BulkSearchRequestService bulkSearchRequestService;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private BulkCustomerCodeDetailsRepository bulkCustomerCodeDetailsRepository;

    @Autowired
    private RestHeaderService restHeaderService;

    @Autowired
    private SecurityUtil securityUtil;

    @Value("${" + PropertyKeyConstants.SCSB_GATEWAY_URL + "}")
    private String scsbUrl;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    public BulkRequestForm processCreateBulkRequest(BulkRequestForm bulkRequestForm, HttpServletRequest request) {
        try {
            if (processPatronValidation(bulkRequestForm)){
                InstitutionEntity institutionEntity = institutionDetailsRepository.findByInstitutionCode(bulkRequestForm.getRequestingInstitution());
                BulkCustomerCodeEntity bulkCustomerCodeEntity = bulkCustomerCodeDetailsRepository.findByCustomerCode(bulkRequestForm.getDeliveryLocationInRequest());
                MultipartFile multipartFile = bulkRequestForm.getFile();
                byte[] bytes = multipartFile.getBytes();
                HttpSession session = request.getSession(false);
                Integer userId = (Integer) session.getAttribute(ScsbConstants.USER_ID);
                Optional<UsersEntity> usersEntity = userDetailsRepository.findById(userId);
                BulkRequestItemEntity bulkRequestItemEntity = new BulkRequestItemEntity();
                bulkRequestItemEntity.setCreatedBy(usersEntity.isPresent() ? usersEntity.get().getLoginId() : "");
                bulkRequestItemEntity.setCreatedDate(new Date());
                bulkRequestItemEntity.setLastUpdatedDate(new Date());
                bulkRequestItemEntity.setEmailId(getEncryptedPatronEmailId(bulkRequestForm.getPatronEmailAddress()));
                bulkRequestItemEntity.setBulkRequestName(bulkRequestForm.getBulkRequestName());
                bulkRequestItemEntity.setBulkRequestFileName(multipartFile.getOriginalFilename());
                bulkRequestItemEntity.setBulkRequestFileData(bytes);
                bulkRequestItemEntity.setPatronId(bulkRequestForm.getPatronBarcodeInRequest());
                bulkRequestItemEntity.setStopCode(bulkRequestForm.getDeliveryLocationInRequest());
                bulkRequestItemEntity.setRequestingInstitutionId(institutionEntity.getId());
                bulkRequestItemEntity.setImsLocation(bulkCustomerCodeEntity.getImsLocationId());

                bulkRequestItemEntity.setNotes(bulkRequestForm.getRequestNotes());
                bulkRequestItemEntity.setBulkRequestStatus(ScsbConstants.IN_PROCESS);
                BulkRequestItemEntity savedBulkRequestItemEntity = bulkRequestDetailsRepository.save(bulkRequestItemEntity);

                String bulkRequestItemUrl = scsbUrl + ScsbConstants.BULK_REQUEST_ITEM_URL;
                HttpEntity requestEntity = new HttpEntity<>(restHeaderService.getHttpHeaders());
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(bulkRequestItemUrl).queryParam("bulkRequestId", savedBulkRequestItemEntity.getId());
                getRestTemplate().exchange(builder.build().encode().toUri(), HttpMethod.POST, requestEntity, BulkRequestResponse.class);

                bulkRequestForm.setSubmitted(true);
                bulkRequestForm.setFileName(multipartFile.getOriginalFilename());
            } else {
                bulkRequestForm.setShowRequestErrorMsg(true);
                bulkRequestForm.setErrorMessage("Patron Barcode is incorrect");
            }
        } catch (IOException e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
        }
        return bulkRequestForm;
    }

    private Boolean processPatronValidation(BulkRequestForm bulkRequestForm) {
        BulkRequestInformation bulkRequestInformation = new BulkRequestInformation();
        bulkRequestInformation.setRequestingInstitution(bulkRequestForm.getRequestingInstitution());
        bulkRequestInformation.setPatronBarcode(bulkRequestForm.getPatronBarcodeInRequest());
        HttpEntity httpEntity = new HttpEntity(bulkRequestInformation, restHeaderService.getHttpHeaders());
        ResponseEntity<Boolean> responseEntity = getRestTemplate().exchange(scsbUrl + "/requestItem/patronValidationBulkRequest", HttpMethod.POST, httpEntity, Boolean.class);
        return responseEntity.getBody();
    }

    public BulkRequestForm processSearchRequest(BulkRequestForm bulkRequestForm) {
        try{
            bulkRequestForm.setPageNumber(0);
            bulkRequestForm.setPageSize(10);
            if (CollectionUtils.isNotEmpty(bulkRequestForm.getBulkSearchResultRows())){
                bulkRequestForm.getBulkSearchResultRows().clear();
            }
            bulkRequestForm = getPaginatedSearchResults(bulkRequestForm);
            if(bulkRequestForm.getTotalPageCount() == 0)
                bulkRequestForm.setMessage(ScsbCommonConstants.SEARCH_RESULT_ERROR_NO_RECORDS_FOUND);
        }catch (Exception e){
            bulkRequestForm.setMessage(ScsbCommonConstants.SEARCH_RESULT_ERROR_NO_RECORDS_FOUND);
            log.error(ScsbCommonConstants.LOG_ERROR,e);
        }
        return bulkRequestForm;
    }


    public BulkRequestForm processOnPageSizeChange(BulkRequestForm bulkRequestForm) {
        try {
            bulkRequestForm.setPageNumber(getPageNumberOnPageSizeChange(bulkRequestForm));
            bulkRequestForm = getPaginatedSearchResults(bulkRequestForm);
        } catch (ParseException e) {
            log.error(ScsbCommonConstants.LOG_ERROR,e);
        }
        return bulkRequestForm;
    }


    public BulkRequestForm getPaginatedSearchResults(BulkRequestForm bulkRequestForm){
        Page<BulkRequestItemEntity> bulkRequestItemEntities = bulkSearchRequestService.processSearchRequest(bulkRequestForm);
       return buildBulkSearchResultRows(bulkRequestForm,bulkRequestItemEntities);
    }


    private BulkRequestForm buildBulkSearchResultRows(BulkRequestForm bulkRequestForm, Page<BulkRequestItemEntity> bulkRequestItemEntities) {
        if (bulkRequestItemEntities.getTotalElements() > 0) {
            List<BulkRequestItemEntity> bulkRequestItemEntityList = bulkRequestItemEntities.getContent();
            List<BulkSearchResultRow> bulkSearchResultRows = new ArrayList<>();
            Map<Integer, String> institutionMap = institutionDetailsRepository.getInstitutionCodeForSuperAdmin(supportInstitution).stream().collect(Collectors.toMap(InstitutionEntity::getId, InstitutionEntity::getInstitutionCode));
            for (BulkRequestItemEntity bulkRequestItemEntity : bulkRequestItemEntityList) {
                try {
                    BulkSearchResultRow bulkSearchResultRow = new BulkSearchResultRow();
                    bulkSearchResultRow.setBulkRequestId(bulkRequestItemEntity.getId());
                    bulkSearchResultRow.setBulkRequestName(bulkRequestItemEntity.getBulkRequestName());
                    bulkSearchResultRow.setFileName(bulkRequestItemEntity.getBulkRequestFileName());
                    bulkSearchResultRow.setPatronBarcode(bulkRequestItemEntity.getPatronId());
                    bulkSearchResultRow.setRequestingInstitution(institutionMap.get(bulkRequestItemEntity.getRequestingInstitutionId()));
                    bulkSearchResultRow.setDeliveryLocation(bulkRequestItemEntity.getStopCode());
                    bulkSearchResultRow.setCreatedBy((userDetailsRepository.findByLoginId(bulkRequestItemEntity.getCreatedBy())).getUserDescription());
                    bulkSearchResultRow.setEmailAddress(getDecryptedPatronEmailId(bulkRequestItemEntity.getEmailId()));
                    bulkSearchResultRow.setCreatedDate(bulkRequestItemEntity.getCreatedDate());
                    bulkSearchResultRow.setStatus(bulkRequestItemEntity.getBulkRequestStatus());
                    bulkSearchResultRow.setBulkRequestNotes(bulkRequestItemEntity.getNotes());
                    bulkSearchResultRow.setImsLocation(bulkRequestItemEntity.getImsLocationEntity().getImsLocationCode());
                    bulkSearchResultRows.add(bulkSearchResultRow);
                } catch (Exception ex) {
                    log.error(ScsbCommonConstants.LOG_ERROR, ex);
                }
            }
            bulkRequestForm.setTotalRecordsCount(String.valueOf(bulkRequestItemEntities.getTotalElements()));
            bulkRequestForm.setTotalPageCount(bulkRequestItemEntities.getTotalPages());
            bulkRequestForm.setBulkSearchResultRows(bulkSearchResultRows);
        } else {
            bulkRequestForm.setMessage("No Search Results Found");
        }
        bulkRequestForm.setShowResults(true);
        return bulkRequestForm;
    }


    private Integer getPageNumberOnPageSizeChange(BulkRequestForm bulkRequestForm) throws ParseException {
        int totalRecordsCount;
        Integer pageNumber = bulkRequestForm.getPageNumber();
        totalRecordsCount = NumberFormat.getNumberInstance().parse(bulkRequestForm.getTotalRecordsCount()).intValue();
        int totalPagesCount = (int) Math.ceil((double) totalRecordsCount / (double) bulkRequestForm.getPageSize());
        if (totalPagesCount > 0 && pageNumber >= totalPagesCount) {
            pageNumber = totalPagesCount - 1;
        }
        return pageNumber;
    }

    public BulkRequestForm processDeliveryLocations(BulkRequestForm bulkRequestForm) {
        InstitutionEntity institutionEntity = institutionDetailsRepository.findByInstitutionCode(bulkRequestForm.getRequestingInstitution());
        bulkRequestForm.setDeliveryLocations(bulkCustomerCodeDetailsRepository.findByOwningInstitutionId(institutionEntity.getId()));
        return bulkRequestForm;
    }

    private String getEncryptedPatronEmailId(String patronEmailAddress) {
        return StringUtils.isNotBlank(patronEmailAddress) ? securityUtil.getEncryptedValue(patronEmailAddress) : patronEmailAddress;
    }

    private String getDecryptedPatronEmailId(String patronEmailAddress) {
        return StringUtils.isNotBlank(patronEmailAddress) ? securityUtil.getDecryptedValue(patronEmailAddress) : patronEmailAddress;
    }

    public BulkRequestItemEntity saveUpadatedRequestStatus(Integer bulkRequestId) throws Exception {
        Optional<BulkRequestItemEntity> bulkRequestItemEntity = bulkRequestDetailsRepository.findById(bulkRequestId);
        if(bulkRequestItemEntity.isPresent()){
            if("PROCESSED".equalsIgnoreCase(bulkRequestItemEntity.get().getBulkRequestStatus())){
                StringBuilder csvRowBuilder = new StringBuilder();
                Map<Integer, String> currentStatus = new HashMap<>();
                Map<Integer, String> exceptionNote = new HashMap<>();
                getCurrentRequestStatus(bulkRequestItemEntity.get(),currentStatus,exceptionNote);
                String bulkRequestFileData = new String(bulkRequestItemEntity.get().getBulkRequestFileData());
                String[] bulkRequestFileDataSplit = bulkRequestFileData.split("\n");
                int count = 0;
                for (String bulkRequestDataRows : bulkRequestFileDataSplit) {
                    String[] bulkRequestData = bulkRequestDataRows.split(",");
                    buildCsvRows(csvRowBuilder, currentStatus, exceptionNote,bulkRequestData,count);
                    count++;
                }
                if (bulkRequestFileDataSplit.length == count){
                    bulkRequestItemEntity.get().setBulkRequestFileData(csvRowBuilder.toString().getBytes());
                    bulkRequestDetailsRepository.save(bulkRequestItemEntity.get());
                }else {
                    bulkRequestItemEntity.get().setBulkRequestFileData("Error occurred while processing bulk request report".getBytes());
                }
            }else {
                bulkRequestItemEntity.get().setBulkRequestFileData("Bulk Request is in process state, so bulk request report could not be generated.".getBytes());
            }
        }else{
            BulkRequestItemEntity notFoundEntity = new BulkRequestItemEntity();
            log.info("bulk request id requested {}",bulkRequestId);
            notFoundEntity.setBulkRequestFileData("Unable to generate bulk request report".getBytes());
            return  notFoundEntity;
        }
        return bulkRequestItemEntity.get();
    }

    private void getCurrentRequestStatus(BulkRequestItemEntity bulkRequestItemEntity,Map<Integer, String> currentStatus,Map<Integer, String> exceptionNote) {
        for(RequestItemEntity requestItemEntity : bulkRequestItemEntity.getRequestItemEntities()){
            currentStatus.put(requestItemEntity.getId(),requestItemEntity.getRequestStatusEntity().getRequestStatusCode());
            if("EXCEPTION".equalsIgnoreCase(requestItemEntity.getRequestStatusEntity().getRequestStatusCode())){
                exceptionNote.put(requestItemEntity.getId(),StringUtils.substringAfter(requestItemEntity.getNotes(), "Exception :"));
            }
        }
    }

    private void buildCsvRows(StringBuilder csvRowBuilder, Map<Integer, String> currentStatus, Map<Integer, String> exceptionNote, String[] bulkRequestData,int count) {
        boolean exceptionStatus = false;
        csvRowBuilder.append(bulkRequestData[0]).append(",");
        csvRowBuilder.append(bulkRequestData[1]).append(",");
        csvRowBuilder.append(bulkRequestData[2]).append(",");
        if(count!=0){
            if (!"null".equalsIgnoreCase(bulkRequestData[2]) && StringUtils.isNotBlank(bulkRequestData[2])){
                String currentRequestStatus = currentStatus.get(Integer.valueOf(bulkRequestData[2]));
                csvRowBuilder.append(currentRequestStatus).append(",");
                if("EXCEPTION".equalsIgnoreCase(currentRequestStatus)){
                    exceptionStatus = true;
                }
            }else {
                csvRowBuilder.append(bulkRequestData[3]).append(",");
            }
        } else{
            csvRowBuilder.append(bulkRequestData[3]).append(",");
        }
        if(exceptionStatus){
            csvRowBuilder.append(exceptionNote.get(Integer.valueOf(bulkRequestData[2]))).append("\n");
        }else {
            if (bulkRequestData.length == 5){
                csvRowBuilder.append(bulkRequestData[4]).append("\n");
            }else {
                csvRowBuilder.append("").append("\n");
            }
        }
    }
}
