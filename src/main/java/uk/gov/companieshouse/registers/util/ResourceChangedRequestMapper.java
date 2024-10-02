package uk.gov.companieshouse.registers.util;

import static java.time.ZoneOffset.UTC;
import static uk.gov.companieshouse.registers.RegistersApplication.NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.registers.exception.InternalServerErrorException;
import uk.gov.companieshouse.registers.logging.DataMapHolder;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;

@Component
public class ResourceChangedRequestMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String SERDES_ERROR_MSG = "Serialisation/deserialisation failed when mapping deleted data";
    private static final DateTimeFormatter PUBLISHED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss").withZone(UTC);

    private final Supplier<Instant> timestampGenerator;
    private final ObjectMapper objectMapper;

    public ResourceChangedRequestMapper(Supplier<Instant> timestampGenerator, ObjectMapper objectMapper) {
        this.timestampGenerator = timestampGenerator;
        this.objectMapper = objectMapper;
    }

    public ChangedResource mapChangedResource(ResourceChangedRequest request) {
        ChangedResourceEvent event = new ChangedResourceEvent()
                .publishedAt(PUBLISHED_AT_FORMATTER.format(timestampGenerator.get()));
        ChangedResource changedResource = new ChangedResource()
                .resourceUri(String.format("company/%s/registers", request.companyNumber()))
                .resourceKind("registers")
                .event(event)
                .contextId(DataMapHolder.getRequestId());

        if (request.isDelete() != null && Boolean.TRUE.equals(request.isDelete())) {
            event.setType("deleted");
            try {
                final String serialisedDeletedData =
                        objectMapper.writeValueAsString(request.registersData());
                changedResource.setDeletedData(objectMapper.readValue(serialisedDeletedData, Object.class));
            } catch (JsonProcessingException ex) {
                LOGGER.error(SERDES_ERROR_MSG, ex, DataMapHolder.getLogMap());
                throw new InternalServerErrorException(SERDES_ERROR_MSG);
            }
        } else {
            event.setType("changed");
        }
        return changedResource;
    }
}
