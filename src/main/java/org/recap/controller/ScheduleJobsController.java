package org.recap.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.jpa.InstitutionEntity;
import org.recap.model.jpa.JobEntity;
import org.recap.model.jpa.JobParamEntity;
import org.recap.model.schedule.ScheduleJobRequest;
import org.recap.model.schedule.ScheduleJobResponse;
import org.recap.model.search.ScheduleJobsForm;
import org.recap.model.usermanagement.UserDetailsForm;
import org.recap.repository.jpa.InstitutionDetailsRepository;
import org.recap.repository.jpa.JobDetailsRepository;
import org.recap.repository.jpa.JobParamDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rajeshbabuk on 4/4/17.
 */
@Slf4j
@RestController
@RequestMapping("/jobs")
public class ScheduleJobsController extends AbstractController {


    @Autowired
    private JobDetailsRepository jobDetailsRepository;

    @Autowired
    private JobParamDetailRepository jobParamDetailRepository;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Value("${" + PropertyKeyConstants.SCSB_SUPPORT_INSTITUTION + "}")
    private String supportInstitution;

    /**
     * Gets rest template.
     *
     * @return the rest template
     */
    @Override
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    public JobDetailsRepository getJobDetailsRepository() {
        return jobDetailsRepository;
    }


    /**
     * Gets all the jobs information from scsb database and display them as rows in the jobs UI page.
     *
     * @return the string
     */
    @GetMapping("/jobs")
    public ScheduleJobsForm displayJobs(HttpServletRequest request) {
        String errorMessage = null;
        HttpSession session = request.getSession(false);
        ScheduleJobsForm scheduleJobsForm = new ScheduleJobsForm();
        UserDetailsForm userDetailsForm = getUserAuthUtil().getUserDetails(session, ScsbConstants.BARCODE_RESTRICTED_PRIVILEGE);
        if (userDetailsForm.isSuperAdmin()) {
            List<JobEntity> jobEntities = getJobDetailsRepository().findAll();
            scheduleJobsForm.setJobEntities(jobEntities);
        } else {
            scheduleJobsForm.setErrorMessage(errorMessage);
        }
        return scheduleJobsForm;
    }

    /**
     * Passes information to the scsb-batch-scheduler microservice about the job whether to be schedule or unschedule.
     *
     * @param scheduleJobsForm the schedule jobs form
     * @return the model and view
     */
    @PostMapping("/jobs")
    public ScheduleJobsForm scheduleJob(@RequestBody ScheduleJobsForm scheduleJobsForm, HttpServletRequest request) {
        ScheduleJobResponse scheduleJobResponse = null;
        try {
            ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest();
            scheduleJobRequest.setJobId(scheduleJobsForm.getJobId());
            scheduleJobRequest.setJobName(scheduleJobsForm.getJobName());
            scheduleJobRequest.setCronExpression(scheduleJobsForm.getCronExpression());
            scheduleJobRequest.setScheduleType(scheduleJobsForm.getScheduleType());
            HttpEntity<ScheduleJobRequest> httpEntity = new HttpEntity<>(scheduleJobRequest, getRestHeaderService().getHttpHeaders());

            ResponseEntity<ScheduleJobResponse> responseEntity = getRestTemplate().exchange(getScsbUrl() + ScsbCommonConstants.URL_SCHEDULE_JOBS, HttpMethod.POST, httpEntity, ScheduleJobResponse.class);
            scheduleJobResponse = responseEntity.getBody() != null ? responseEntity.getBody() : new ScheduleJobResponse();
            String message = null;
            if (scheduleJobResponse != null) {
                message = scheduleJobResponse.getMessage();
            }
            if (StringUtils.containsIgnoreCase(message, ScsbCommonConstants.SUCCESS)) {
                saveJob(scheduleJobsForm, scheduleJobResponse.getNextRunTime());
                scheduleJobsForm.setMessage(scheduleJobResponse.getMessage());
            } else {
                scheduleJobsForm.setErrorMessage(scheduleJobResponse.getMessage());
            }
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
            scheduleJobsForm.setErrorMessage(e.getMessage());
        }
        return scheduleJobsForm;
    }

    /**
     * Get job parameters by job name
     *
     * @param scheduleJobsForm the schedule jobs form
     * @return the model
     */
    @PostMapping("/job-parameters")
    public ScheduleJobsForm getJobParameters(@RequestBody ScheduleJobsForm scheduleJobsForm, HttpServletRequest request) {
        try {
            JobEntity jobEntity = jobDetailsRepository.findByJobName(scheduleJobsForm.getJobName());
            if (null != jobEntity) {
                JobParamEntity jobParamEntity = jobParamDetailRepository.findByJobName(jobEntity.getJobName());
                if (null != jobParamEntity
                        && CollectionUtils.isNotEmpty(jobParamEntity.getJobParamDataEntities())
                        && ScsbCommonConstants.INSTITUTION.equalsIgnoreCase(jobParamEntity.getJobParamDataEntities().get(0).getParamName())) {
                    scheduleJobsForm.setJobParameter(jobParamEntity.getJobParamDataEntities().get(0).getParamValue());
                    scheduleJobsForm.setMessage(ScsbCommonConstants.SUCCESS);
                }
            }
        } catch (Exception e) {
            log.error(ScsbCommonConstants.LOG_ERROR, e);
            scheduleJobsForm.setErrorMessage(e.getMessage());
        }
        return scheduleJobsForm;
    }

    /**
     * Get All Institutions except support institution
     * @return instituions
     */
    @GetMapping("/get-institutions")
    public List<String> getInstitutions() {
       return institutionDetailsRepository.getInstitutionCodeForSuperAdmin(supportInstitution).stream().map(InstitutionEntity::getInstitutionCode).collect(Collectors.toList());
    }

    /**
     * Save Job Entity
     * @param scheduleJobsForm Schedule Jobs Form
     * @param nextRunTime Next Run Time
     */
    private void saveJob(ScheduleJobsForm scheduleJobsForm, Date nextRunTime) {
        JobEntity jobEntity = jobDetailsRepository.findByJobName(scheduleJobsForm.getJobName());
        if (null != jobEntity) {
            if (ScsbConstants.UNSCHEDULE.equals(scheduleJobsForm.getScheduleType())) {
                jobEntity.setStatus(ScsbConstants.UNSCHEDULED);
                jobEntity.setNextRunTime(null);
            } else {
                jobEntity.setStatus(ScsbConstants.SCHEDULED);
                jobEntity.setCronExpression(scheduleJobsForm.getCronExpression());
                jobEntity.setNextRunTime(nextRunTime);
            }
            jobDetailsRepository.save(jobEntity);
            if (StringUtils.isNotBlank(scheduleJobsForm.getJobParameter())) {
                JobParamEntity jobParamEntity = jobParamDetailRepository.findByJobName(jobEntity.getJobName());
                if (CollectionUtils.isNotEmpty(jobParamEntity.getJobParamDataEntities())) {
                    jobParamEntity.getJobParamDataEntities().get(0).setParamValue(scheduleJobsForm.getJobParameter());
                    jobParamDetailRepository.save(jobParamEntity);
                }
            }
        }
    }
}
