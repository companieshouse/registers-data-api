package uk.gov.companieshouse.registers.util;

import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.registers.exception.InternalServerErrorException;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;

@Component
public class ResourceChangedRequestMapper {

    private static final String SERDES_ERROR_MSG = "Serialisation/deserialisation failed when mapping deleted data";
    private final Supplier<String> timestampGenerator;
    private final ObjectMapper objectMapper;
    private final Logger logger;

    public ResourceChangedRequestMapper(Supplier<String> timestampGenerator, ObjectMapper objectMapper, Logger logger) {
        this.timestampGenerator = timestampGenerator;
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    public ChangedResource mapChangedResource(ResourceChangedRequest request) {
        ChangedResourceEvent event = new ChangedResourceEvent().publishedAt(this.timestampGenerator.get());
        ChangedResource changedResource = new ChangedResource()
                .resourceUri(String.format("company/%s/registers", request.companyNumber()))
                .resourceKind("registers")
                .event(event)
                .contextId(request.contextId());

        if (request.isDelete() != null && Boolean.TRUE.equals(request.isDelete())) {
            event.setType("deleted");
            try {
                final String serialisedDeletedData =
                        objectMapper.writeValueAsString(request.registersData());
                changedResource.setDeletedData(objectMapper.readValue(serialisedDeletedData, Object.class));
            } catch (JsonProcessingException ex) {
                logger.error(SERDES_ERROR_MSG, ex);
                throw new InternalServerErrorException(SERDES_ERROR_MSG);
            }
        } else {
            event.setType("changed");
        }
        return changedResource;
    }
}
