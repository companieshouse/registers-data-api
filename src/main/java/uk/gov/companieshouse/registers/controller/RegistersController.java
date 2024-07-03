package uk.gov.companieshouse.registers.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.registers.CompanyRegister;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.service.RegistersService;
import uk.gov.companieshouse.registers.model.ServiceStatus;
import uk.gov.companieshouse.logging.Logger;

import java.util.Optional;

@RestController
public class RegistersController {

    @Autowired
    private Logger logger;

    @Autowired
    private RegistersService service;

    @GetMapping("/company/{company_number}/registers")
    public ResponseEntity<CompanyRegister> companyRegistersGet(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Getting company registers for company number %s", companyNumber));

        Optional<CompanyRegistersDocument> document = service.getCompanyRegisters(companyNumber);

        return document.map(companyRegistersDocument -> ResponseEntity.ok().body(companyRegistersDocument.getData())).
                orElseGet(() -> ResponseEntity.notFound().build());

    }

    @PutMapping("/company/{company_number}/registers")
    public ResponseEntity<Void> companyRegistersUpsert(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @RequestBody InternalRegisters requestBody) {
        logger.info(String.format(
                "Processing company registers information for company number %s",
                companyNumber));

        ServiceStatus serviceStatus = service.upsertCompanyRegisters(contextId, companyNumber, requestBody);

        if (serviceStatus.equals(ServiceStatus.SERVER_ERROR)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else if (serviceStatus.equals(ServiceStatus.CLIENT_ERROR)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }else {
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/company/{company_number}/registers")
    public ResponseEntity<CompanyRegistersDocument> companyRegistersDelete(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Deleting company registers for company number %s", companyNumber));

        ServiceStatus serviceStatus = service.deleteCompanyRegisters(contextId, companyNumber);

        if (serviceStatus.equals(ServiceStatus.SERVER_ERROR)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else if (serviceStatus.equals(ServiceStatus.CLIENT_ERROR)) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok().build();
        }
    }
}
