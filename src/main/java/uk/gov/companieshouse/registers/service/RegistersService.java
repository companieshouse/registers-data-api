package uk.gov.companieshouse.registers.service;

import java.util.Optional;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.model.ServiceStatus;

public interface RegistersService {
    ServiceStatus upsertCompanyRegisters(String contextId, String companyNumber, InternalRegisters requestBody);
    Optional<CompanyRegistersDocument> getCompanyRegisters(String companyNumber);
    ServiceStatus deleteCompanyRegisters(String contextId, String companyNumber);
}
