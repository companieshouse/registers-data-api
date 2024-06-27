package uk.gov.companieshouse.registers.model;

import java.util.Objects;

public record ResourceChangedRequest(String contextId, String companyNumber, Object registersData, Boolean isDelete) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceChangedRequest that = (ResourceChangedRequest) o;
        return Objects.equals(contextId, that.contextId) &&
                Objects.equals(companyNumber, that.companyNumber) &&
                Objects.equals(registersData, that.registersData) &&
                Objects.equals(isDelete, that.isDelete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, companyNumber, registersData, isDelete);
    }
}
