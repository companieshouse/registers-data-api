package uk.gov.companieshouse.registers.model;

public record ResourceChangedRequest(String companyNumber, Object registersData, Boolean isDelete) {

}
