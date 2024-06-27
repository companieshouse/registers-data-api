package uk.gov.companieshouse.registers.util;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;

@Component
public class ResourceChangedRequestMapper {

    private final Supplier<String> timestampGenerator;

    public ResourceChangedRequestMapper(Supplier<String> timestampGenerator) {
        this.timestampGenerator = timestampGenerator;
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
            changedResource.setDeletedData(request.registersData());
        } else {
            event.setType("changed");
        }
        return changedResource;
    }
}
