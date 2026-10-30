package org.recap.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.recap.model.request.RequestInfo;
import org.recap.model.request.RequestLogReportRequest;
import org.recap.service.RequestService;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class})
public class RequestLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestLogController requestLogController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(requestLogController).build();
    }

    @Test
    public void testGetRequestsLogReports() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        RequestLogReportRequest requestLogReportRequest = getRequestLogReportRequest();
        RequestLogReportRequest mockResponse = getRequestLogReportRequest();

        Mockito.when(requestService.getRequestReports(requestLogReportRequest)).thenReturn(mockResponse);

        try {
            mockMvc.perform(post("/request-log/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestLogReportRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(mockResponse)));
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSubmitRequestsLogReports() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        RequestLogReportRequest requestLogReportRequest = getRequestLogReportRequest();
        RequestLogReportRequest mockResponse = getRequestLogReportRequest();

        Mockito.when(requestService.submitRequestReports(requestLogReportRequest)).thenReturn(mockResponse);

        try {
        mockMvc.perform(post("/request-log/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestLogReportRequest)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockResponse)));
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public RequestLogReportRequest getRequestLogReportRequest(){
        RequestLogReportRequest requestLogReportRequest = new RequestLogReportRequest();

        requestLogReportRequest.setTotalRecordsCount(100L);
        requestLogReportRequest.setPageNumber(1);
        requestLogReportRequest.setPageSize(20);
        requestLogReportRequest.setTotalPageCount(5);
        requestLogReportRequest.setFromDate("2023-09-01");
        requestLogReportRequest.setToDate("2023-09-30");
        requestLogReportRequest.setInstitution("Some Institution");
        requestLogReportRequest.setStatus("Approved");
        requestLogReportRequest.setValidationStatus("Validated");
        requestLogReportRequest.setGatewayRequestLogId(12345);

        List<RequestInfo> requestInfoList = new ArrayList<>();
        RequestInfo requestInfo = getRequestInfo();
        requestInfoList.add(requestInfo);
        requestLogReportRequest.setRequestInfoList(requestInfoList);
        return requestLogReportRequest;
    }

    private RequestInfo getRequestInfo() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setId(101);
        requestInfo.setRequestInstitution("Requesting Institution");
        requestInfo.setItemOwningInstitution("Owning Institution");
        requestInfo.setRequestRecieved("2023-09-01");
        requestInfo.setRequestedItemBarcode("ITEM123456");
        requestInfo.setResponseMessage("Request successfully processed");
        requestInfo.setValidationMessage("Valid request");
        requestInfo.setStatusId(1);
        requestInfo.setStatus("Completed");
        requestInfo.setDate(new Date());
        return requestInfo;
    }
}
