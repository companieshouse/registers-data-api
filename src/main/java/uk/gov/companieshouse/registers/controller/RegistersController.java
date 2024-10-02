package uk.gov.companieshouse.registers.controller;

import static uk.gov.companieshouse.registers.RegistersApplication.NAMESPACE;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.registers.CompanyRegister;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.registers.logging.DataMapHolder;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.model.ServiceStatus;
import uk.gov.companieshouse.registers.service.RegistersService;

@RestController
public class RegistersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final RegistersService service;

    public RegistersController(RegistersService service) {
        this.service = service;
    }

    @GetMapping("/company/{company_number}/registers")
    public ResponseEntity<CompanyRegister> companyRegistersGet(@PathVariable("company_number") String companyNumber) {
        DataMapHolder.get().companyNumber(companyNumber);
        LOGGER.info("Getting company registers", DataMapHolder.getLogMap());

        Optional<CompanyRegistersDocument> document = service.getCompanyRegisters(companyNumber);

        return document.map(companyRegistersDocument -> ResponseEntity.ok().body(companyRegistersDocument.getData())).
                orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/company/{company_number}/registers")
    public ResponseEntity<Void> companyRegistersUpsert(
            @PathVariable("company_number") String companyNumber,
            @RequestBody InternalRegisters requestBody) {
        DataMapHolder.get().companyNumber(companyNumber);
        LOGGER.info("Upserting company registers", DataMapHolder.getLogMap());

        ServiceStatus serviceStatus = service.upsertCompanyRegisters(companyNumber, requestBody);

        if (serviceStatus.equals(ServiceStatus.SERVER_ERROR)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else if (serviceStatus.equals(ServiceStatus.CLIENT_ERROR)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } else {
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/company/{company_number}/registers")
    public ResponseEntity<CompanyRegistersDocument> companyRegistersDelete(
            @PathVariable("company_number") String companyNumber) {
        DataMapHolder.get().companyNumber(companyNumber);
        LOGGER.info("Deleting company registers", DataMapHolder.getLogMap());

        ServiceStatus serviceStatus = service.deleteCompanyRegisters(companyNumber);

        if (serviceStatus.equals(ServiceStatus.SERVER_ERROR)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else if (serviceStatus.equals(ServiceStatus.CLIENT_ERROR)) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok().build();
        }
    }
}
