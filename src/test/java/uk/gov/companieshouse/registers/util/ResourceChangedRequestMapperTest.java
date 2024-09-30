package uk.gov.companieshouse.registers.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.registers.CompanyRegister;
import uk.gov.companieshouse.registers.logging.DataMapHolder;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;

@ExtendWith(MockitoExtension.class)
class ResourceChangedRequestMapperTest {

    private static final String EXPECTED_CONTEXT_ID = "35234234";
    private static final Instant DATE = Instant.parse("2024-09-30T12:00:00.123456Z");
    private static final String DATE_STRING = "2024-09-30T12:00:00";

    @Mock
    private Supplier<Instant> timestampGenerator;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ResourceChangedRequestMapper mapper;

    @BeforeEach
    void setUp() {
        DataMapHolder.initialise(EXPECTED_CONTEXT_ID);
    }

    @ParameterizedTest
    @MethodSource("resourceChangedScenarios")
    void testMapper(ResourceChangedTestArgument argument) throws JsonProcessingException {
        // given
        when(timestampGenerator.get()).thenReturn(DATE);
        if (argument.request().isDelete() != null && argument.request().isDelete()) {
            when(objectMapper.writeValueAsString(any())).thenReturn("{mapped_deleted_data_mock}");
            when(objectMapper.readValue("{mapped_deleted_data_mock}", Object.class)).thenReturn(argument.request().registersData());
        }

        // when
        ChangedResource actual = mapper.mapChangedResource(argument.request());

        // then
        assertEquals(argument.changedResource(), actual);
    }

    static Stream<ResourceChangedTestArgument> resourceChangedScenarios() {
        return Stream.of(
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest("12345678", null, false))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/registers")
                        .withResourceKind("registers")
                        .withEventType("changed")
                        .withEventPublishedAt(DATE_STRING)
                        .build(),
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest("12345678", null, null))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/registers")
                        .withResourceKind("registers")
                        .withEventType("changed")
                        .withEventPublishedAt(DATE_STRING)
                        .build(),
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest("12345678", new CompanyRegister(), true))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/registers")
                        .withResourceKind("registers")
                        .withEventType("deleted")
                        .withDeletedData(new CompanyRegister())
                        .withEventPublishedAt(DATE_STRING)
                        .build()
        );
    }

    record ResourceChangedTestArgument(ResourceChangedRequest request, ChangedResource changedResource) {

        public static ResourceChangedTestArgumentBuilder builder() {
                return new ResourceChangedTestArgumentBuilder();
            }

            @Override
            public String toString() {
                return this.request.toString();
            }
        }

    static class ResourceChangedTestArgumentBuilder {
        private ResourceChangedRequest request;
        private String resourceUri;
        private String resourceKind;
        private String contextId;
        private String eventType;
        private String eventPublishedAt;
        private Object deletedData;

        public ResourceChangedTestArgumentBuilder withRequest(ResourceChangedRequest request) {
            this.request = request;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withResourceUri(String resourceUri) {
            this.resourceUri = resourceUri;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withResourceKind(String resourceKind) {
            this.resourceKind = resourceKind;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withContextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withEventPublishedAt(String eventPublishedAt) {
            this.eventPublishedAt = eventPublishedAt;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withDeletedData(Object deletedData) {
            this.deletedData = deletedData;
            return this;
        }

        public ResourceChangedTestArgument build() {
            ChangedResource changedResource = new ChangedResource();
            changedResource.setResourceUri(this.resourceUri);
            changedResource.setResourceKind(this.resourceKind);
            changedResource.setContextId(this.contextId);
            ChangedResourceEvent event = new ChangedResourceEvent();
            event.setType(this.eventType);
            event.setPublishedAt(this.eventPublishedAt);
            changedResource.setEvent(event);
            changedResource.setDeletedData(deletedData);
            return new ResourceChangedTestArgument(this.request, changedResource);
        }
    }
}
