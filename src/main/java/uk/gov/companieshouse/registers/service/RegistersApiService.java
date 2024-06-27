package uk.gov.companieshouse.registers.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;
import uk.gov.companieshouse.registers.util.ResourceChangedRequestMapper;
import uk.gov.companieshouse.registers.model.ServiceStatus;
import uk.gov.companieshouse.logging.Logger;

@Service
public class RegistersApiService {

    private static final String CHANGED_RESOURCE_URI = "/private/resource-changed";
    private final Logger logger;
    private final String chsKafkaUrl;
    private final ApiClientService apiClientService;
    private final ResourceChangedRequestMapper mapper;

    /**
     * Invoke API.
     */
    public RegistersApiService(@Value("${chs.kafka.api.endpoint}") String chsKafkaUrl,
                               ApiClientService apiClientService,
                               Logger logger,
                               ResourceChangedRequestMapper mapper) {
        this.chsKafkaUrl = chsKafkaUrl;
        this.apiClientService = apiClientService;
        this.logger = logger;
        this.mapper = mapper;
    }


    /**
     * Calls the CHS Kafka api.
     * @param resourceChangedRequest encapsulates details relating to the updated or deleted company registers resource
     * @return The service status of the response from chs kafka api
     */
    public ServiceStatus invokeChsKafkaApi(ResourceChangedRequest resourceChangedRequest) {
        InternalApiClient internalApiClient = apiClientService.getInternalApiClient();
        internalApiClient.setBasePath(chsKafkaUrl);

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
                logger.error("Unsuccessful call to /private/resource-changed endpoint", ex);
            } else {
                logger.error("Error occurred while calling /private/resource-changed endpoint", ex);
            }
            return ServiceStatus.SERVER_ERROR;
        }
    }
}
