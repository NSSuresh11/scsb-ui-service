package org.recap.helper;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.recap.service.RestHeaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
Helper class which helps to build the Rest Template and consume the HTTP Rest request.

 @author  Ramanujam (CEI)
 */

@Component
@Slf4j
public class RestTemplateHelper {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    @Autowired
    private RestHeaderService restHeaderService;

    public RestHeaderService getRestHeaderService(){
        return restHeaderService;
    }

    @Autowired
    public RestTemplateHelper(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        //TODO: Need to disable the interceptor on PROD
        restTemplate.getInterceptors().add(new HttpInterceptor());
        this.objectMapper = objectMapper;
    }

    /**
     * Helper method to consume HTTP POST Rest endpoint.
     * @param clazz Expected type of business entity  as response.
     * @param url String Url for the HTTP POST request.
     * @param body Body to be posted at the request. It is of an type.
     * @param uriVariables Object of uri Variables if any needs to be passed.
     * @param <T> Type of business entity to be returned as response..
     * @param <R> Type of business entity passed as body.
     * @return Generic business entity.
     */
    public <T, R> T postForEntity(Class<T> clazz, String url, R body, Object... uriVariables) {
       // HttpHeaders headers = HelperUtil.getSwaggerHeaders();
        HttpHeaders headers = getRestHeaderService().getHttpHeaders();
        HttpEntity<R> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class, uriVariables);
        JavaType javaType = objectMapper.getTypeFactory().constructType(clazz);
        return readValue(response, javaType);
    }

    /**
     * Function to transform the String response to specific JavaType
     * @param response String response which needs to be read and transformed.
     * @param javaType The Business Entity type, the string response needs to be converted.
     * @param <T> Generic Type of Business Entity.
     * @return Business Entity Type.
     */
    private <T> T readValue(ResponseEntity<String> response, JavaType javaType) {
        T result = null;
        if (response.getStatusCode() == HttpStatus.OK ||
                response.getStatusCode() == HttpStatus.CREATED) {
            try {
                result = objectMapper.readValue(response.getBody(), javaType);
            } catch (JacksonException e) {
                log.info(e.getMessage());
            }
        } else {
            log.info("No data found {}", response.getStatusCode());
        }
        return result;
    }
}
