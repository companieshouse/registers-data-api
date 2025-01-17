package uk.gov.companieshouse.registers.service;


import static uk.gov.companieshouse.registers.RegistersApplication.NAMESPACE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.registers.logging.DataMapHolder;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;
import uk.gov.companieshouse.registers.model.ServiceStatus;
import uk.gov.companieshouse.registers.util.ResourceChangedRequestMapper;

@Service
public class RegistersApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String CHANGED_RESOURCE_URI = "/private/resource-changed";

    private final String chsKafkaUrl;
    private final ApiClientService apiClientService;
    private final ResourceChangedRequestMapper mapper;

    /**
     * Invoke API.
     */
    public RegistersApiService(@Value("${chs.kafka.api.endpoint}") String chsKafkaUrl,
            ApiClientService apiClientService,
            ResourceChangedRequestMapper mapper) {
        this.chsKafkaUrl = chsKafkaUrl;
        this.apiClientService = apiClientService;
        this.mapper = mapper;
    }

    /**
     * Calls the CHS Kafka api.
     *
     * @param resourceChangedRequest encapsulates details relating to the updated or deleted company registers resource
     * @return The service status of the response from chs kafka api
     */
    public ServiceStatus invokeChsKafkaApi(ResourceChangedRequest resourceChangedRequest) {
        InternalApiClient internalApiClient = apiClientService.getInternalApiClient();
        internalApiClient.setBasePath(chsKafkaUrl);
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        CHANGED_RESOURCE_URI, mapper.mapChangedResource(resourceChangedRequest));

        return handleApiCall(changedResourcePost);
    }

    private ServiceStatus handleApiCall(PrivateChangedResourcePost changedResourcePost) {
        try {
            changedResourcePost.execute();
            return ServiceStatus.SUCCESS;
        } catch (ApiErrorResponseException ex) {
            if (!HttpStatus.valueOf(ex.getStatusCode()).is2xxSuccessful()) {
                LOGGER.error("Unsuccessful call to resource changed", ex, DataMapHolder.getLogMap());
            } else {
                LOGGER.error("Error occurred while calling resource changed", ex, DataMapHolder.getLogMap());
            }
            return ServiceStatus.SERVER_ERROR;
        }
    }
}
