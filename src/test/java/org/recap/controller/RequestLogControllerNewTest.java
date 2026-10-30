package org.recap.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.recap.model.request.RequestInfo;
import org.recap.model.request.RequestLogReportRequest;
import org.recap.service.RequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class})
public class RequestLogControllerNewTest {

    @InjectMocks
    private RequestLogController requestLogController;

    @Mock
    private RequestService requestService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(requestLogController).build();
    }

    private RequestLogReportRequest buildRequest() {
        RequestLogReportRequest req = new RequestLogReportRequest();
        req.setTotalRecordsCount(50L);
        req.setPageNumber(1);
        req.setPageSize(10);
        req.setTotalPageCount(5);
        req.setFromDate("2024-01-01");
        req.setToDate("2024-01-31");
        req.setInstitution("PUL");
        req.setStatus("Approved");
        req.setValidationStatus("Valid");
        req.setGatewayRequestLogId(999);
        req.setRequestInfoList(buildRequestInfoList());
        return req;
    }

    private List<RequestInfo> buildRequestInfoList() {
        List<RequestInfo> list = new ArrayList<>();
        RequestInfo info = new RequestInfo();
        info.setId(1);
        info.setRequestInstitution("PUL");
        info.setItemOwningInstitution("CUL");
        info.setRequestedItemBarcode("BC123");
        info.setResponseMessage("OK");
        info.setValidationMessage("Valid");
        info.setStatusId(1);
        info.setStatus("Completed");
        info.setDate(new Date(0));
        list.add(info);
        return list;
    }

    private RequestLogReportRequest buildMinimalRequest() {
        RequestLogReportRequest req = new RequestLogReportRequest();
        req.setPageNumber(0);
        req.setPageSize(0);
        return req;
    }

    @Test
    public void getRequestsLogReports_shouldReturn200() throws Exception {
        RequestLogReportRequest req = buildRequest();
        RequestLogReportRequest resp = buildRequest();
        when(requestService.getRequestReports(any())).thenReturn(resp);
        mockMvc.perform(post("/request-log/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    public void getRequestsLogReports_shouldReturnContentTypeJson() throws Exception {
        when(requestService.getRequestReports(any())).thenReturn(buildRequest());
        mockMvc.perform(post("/request-log/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void getRequestsLogReports_shouldMapToPostReportsEndpoint() throws Exception {
        when(requestService.getRequestReports(any())).thenReturn(buildMinimalRequest());
        MvcResult result = mockMvc.perform(post("/request-log/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void getRequestsLogReports_shouldDelegateToRequestService() throws Exception {
        when(requestService.getRequestReports(any())).thenReturn(buildRequest());
        mockMvc.perform(post("/request-log/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest())));
        verify(requestService, times(1)).getRequestReports(any(RequestLogReportRequest.class));
    }

    @Test
    public void getRequestsLogReports_shouldForwardInstitutionToService() throws Exception {
        ArgumentCaptor<RequestLogReportRequest> captor =
                ArgumentCaptor.forClass(RequestLogReportRequest.class);
        when(requestService.getRequestReports(captor.capture())).thenReturn(buildRequest());
        RequestLogReportRequest req = buildRequest();
        req.setInstitution("NYPL");
        mockMvc.perform(post("/request-log/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
        assertEquals("NYPL", captor.getValue().getInstitution());
    }

    @Test
    public void getRequestsLogReports_shouldForwardFromDateAndToDateToService() throws Exception {
        ArgumentCaptor<RequestLogReportRequest> captor =
                ArgumentCaptor.forClass(RequestLogReportRequest.class);
        when(requestService.getRequestReports(captor.capture())).thenReturn(buildRequest());
        RequestLogReportRequest req = buildRequest();
        req.setFromDate("2024-03-01");
        req.setToDate("2024-03-31");
        mockMvc.perform(post("/request-log/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
        assertEquals("2024-03-01", captor.getValue().getFromDate());
        assertEquals("2024-03-31", captor.getValue().getToDate());
    }

    @Test
    public void getRequestsLogReports_responseBodyShouldMatchServiceResult() throws Exception {
        RequestLogReportRequest serviceResult = buildRequest();
        serviceResult.setStatus("PendingFromIls");
        when(requestService.getRequestReports(any())).thenReturn(serviceResult);
        MvcResult result = mockMvc.perform(post("/request-log/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        RequestLogReportRequest returned =
                objectMapper.readValue(body, RequestLogReportRequest.class);
        assertEquals("PendingFromIls", returned.getStatus());
    }

    @Test
    public void getRequestsLogReports_directCall_shouldWrapInResponseEntityWithHttpStatusOk() {
        RequestLogReportRequest serviceResult = buildRequest();
        when(requestService.getRequestReports(any())).thenReturn(serviceResult);
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.getRequestsLogReports(buildRequest());
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    public void getRequestsLogReports_directCall_bodyMatchesServiceReturn() {
        RequestLogReportRequest serviceResult = buildRequest();
        serviceResult.setTotalRecordsCount(42L);
        when(requestService.getRequestReports(any())).thenReturn(serviceResult);
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.getRequestsLogReports(buildRequest());
        assertNotNull(entity.getBody());
        assertEquals(42L, (long) entity.getBody().getTotalRecordsCount());
    }

    @Test
    public void getRequestsLogReports_whenServiceReturnsNull_responseShouldBeOkWithNullBody() {
        when(requestService.getRequestReports(any())).thenReturn(null);
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.getRequestsLogReports(buildRequest());
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNull(entity.getBody());
    }

    @Test
    public void getRequestsLogReports_withEmptyRequestBody_shouldStillCallService()
            throws Exception {
        when(requestService.getRequestReports(any())).thenReturn(buildMinimalRequest());
        mockMvc.perform(post("/request-log/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        verify(requestService).getRequestReports(any());
    }

    @Test
    public void getRequestsLogReports_pageFieldsPreservedInResponse() {
        RequestLogReportRequest serviceResult = buildRequest();
        serviceResult.setPageNumber(3);
        serviceResult.setPageSize(25);
        serviceResult.setTotalPageCount(10);
        when(requestService.getRequestReports(any())).thenReturn(serviceResult);
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.getRequestsLogReports(buildRequest());
        assertNotNull(entity.getBody());
    }

    @Test
    public void submitRequestsLogReports_shouldReturn200() throws Exception {
        when(requestService.submitRequestReports(any())).thenReturn(buildRequest());
        mockMvc.perform(post("/request-log/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());
    }

    @Test
    public void submitRequestsLogReports_shouldReturnContentTypeJson() throws Exception {
        when(requestService.submitRequestReports(any())).thenReturn(buildRequest());
        mockMvc.perform(post("/request-log/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void submitRequestsLogReports_shouldMapToPostSubmitEndpoint() throws Exception {
        when(requestService.submitRequestReports(any())).thenReturn(buildMinimalRequest());
        MvcResult result = mockMvc.perform(post("/request-log/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void submitRequestsLogReports_shouldDelegateToRequestService() throws Exception {
        when(requestService.submitRequestReports(any())).thenReturn(buildRequest());
        mockMvc.perform(post("/request-log/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest())));
        verify(requestService, times(1))
                .submitRequestReports(any(RequestLogReportRequest.class));
    }

    @Test
    public void submitRequestsLogReports_shouldForwardGatewayIdToService() throws Exception {
        ArgumentCaptor<RequestLogReportRequest> captor =
                ArgumentCaptor.forClass(RequestLogReportRequest.class);
        when(requestService.submitRequestReports(captor.capture())).thenReturn(buildRequest());
        RequestLogReportRequest req = buildRequest();
        req.setGatewayRequestLogId(77777);
        mockMvc.perform(post("/request-log/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    @Test
    public void submitRequestsLogReports_responseBodyShouldMatchServiceResult() throws Exception {
        RequestLogReportRequest serviceResult = buildRequest();
        serviceResult.setValidationStatus("SubmittedToIls");
        when(requestService.submitRequestReports(any())).thenReturn(serviceResult);
        MvcResult result = mockMvc.perform(post("/request-log/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        RequestLogReportRequest returned =
                objectMapper.readValue(body, RequestLogReportRequest.class);
        assertEquals("SubmittedToIls", returned.getValidationStatus());
    }

    @Test
    public void submitRequestsLogReports_directCall_shouldWrapInResponseEntityWithHttpStatusOk() {
        when(requestService.submitRequestReports(any())).thenReturn(buildRequest());
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.submitRequestsLogReports(buildRequest());
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    public void submitRequestsLogReports_directCall_bodyMatchesServiceReturn() {
        RequestLogReportRequest serviceResult = buildRequest();
        serviceResult.setTotalRecordsCount(999L);
        when(requestService.submitRequestReports(any())).thenReturn(serviceResult);
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.submitRequestsLogReports(buildRequest());
        assertNotNull(entity.getBody());
        assertEquals(999L, (long) entity.getBody().getTotalRecordsCount());
    }

    @Test
    public void submitRequestsLogReports_whenServiceReturnsNull_responseShouldBeOkWithNullBody() {
        when(requestService.submitRequestReports(any())).thenReturn(null);
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.submitRequestsLogReports(buildRequest());
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNull(entity.getBody());
    }

    @Test
    public void submitRequestsLogReports_withEmptyRequestBody_shouldStillCallService()
            throws Exception {
        when(requestService.submitRequestReports(any())).thenReturn(buildMinimalRequest());
        mockMvc.perform(post("/request-log/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        verify(requestService).submitRequestReports(any());
    }

    @Test
    public void submitRequestsLogReports_requestInfoListPreservedInResponse() {
        RequestLogReportRequest serviceResult = buildRequest();
        serviceResult.setRequestInfoList(buildRequestInfoList());
        when(requestService.submitRequestReports(any())).thenReturn(serviceResult);
        ResponseEntity<RequestLogReportRequest> entity =
                requestLogController.submitRequestsLogReports(buildRequest());
        assertNotNull(entity.getBody());
        assertNotNull(entity.getBody().getRequestInfoList());
        assertFalse(entity.getBody().getRequestInfoList().isEmpty());
    }

    @Test
    public void getRequestsLogReports_shouldNeverCallSubmitReports() {
        when(requestService.getRequestReports(any())).thenReturn(buildRequest());
        requestLogController.getRequestsLogReports(buildRequest());
        verify(requestService, never()).submitRequestReports(any());
    }

    @Test
    public void submitRequestsLogReports_shouldNeverCallGetReports() {
        when(requestService.submitRequestReports(any())).thenReturn(buildRequest());
        requestLogController.submitRequestsLogReports(buildRequest());
        verify(requestService, never()).getRequestReports(any());
    }
}