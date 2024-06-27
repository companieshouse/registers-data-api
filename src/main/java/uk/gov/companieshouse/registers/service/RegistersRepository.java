package uk.gov.companieshouse.registers.service;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;

@Repository
public interface RegistersRepository extends MongoRepository<CompanyRegistersDocument, String> {
}
