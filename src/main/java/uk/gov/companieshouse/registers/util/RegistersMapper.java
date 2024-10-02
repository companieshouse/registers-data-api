package uk.gov.companieshouse.registers.util;

import static uk.gov.companieshouse.api.registers.CompanyRegister.KindEnum.REGISTERS;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.registers.CompanyRegister;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.api.registers.LinksType;
import uk.gov.companieshouse.api.registers.Registers;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.model.Updated;

@Component
public class RegistersMapper {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");

    public CompanyRegistersDocument map(String companyNumber, CompanyRegistersDocument existingDocument,
            InternalRegisters requestBody) {
        return new CompanyRegistersDocument()
                .setId(companyNumber)
                .setData(new CompanyRegister()
                        .registers(mapRegisters(existingDocument, requestBody.getExternalData()))
                        .kind(REGISTERS)
                        .links(new LinksType().self(String.format("/company/%s/registers", companyNumber)))
                        .etag(GenerateEtagUtil.generateEtag()))
                .setUpdated(new Updated(LocalDateTime.now()))
                .setDeltaAt(dateTimeFormatter.format(requestBody.getInternalData().getDeltaAt()));
    }

    private Registers mapRegisters(CompanyRegistersDocument existingDocument, Registers externalData) {
        // no existing registers to add to or update so just return new registers from delta
        if (existingDocument == null) {
            return externalData;
        }

        // existing registers need to be added to or updated with registers from delta
        Registers existingRegisters = existingDocument.getData().getRegisters();
        if (externalData.getDirectors() != null) {
            existingRegisters.setDirectors(externalData.getDirectors());
        }
        if (externalData.getSecretaries() != null) {
            existingRegisters.setSecretaries(externalData.getSecretaries());
        }
        if (externalData.getPersonsWithSignificantControl() != null) {
            existingRegisters.setPersonsWithSignificantControl(externalData.getPersonsWithSignificantControl());
        }
        if (externalData.getMembers() != null) {
            existingRegisters.setMembers(externalData.getMembers());
        }
        if (externalData.getUsualResidentialAddress() != null) {
            existingRegisters.setUsualResidentialAddress(externalData.getUsualResidentialAddress());
        }
        if (externalData.getLlpMembers() != null) {
            existingRegisters.setLlpMembers(externalData.getLlpMembers());
        }
        if (externalData.getLlpUsualResidentialAddress() != null) {
            existingRegisters.setLlpUsualResidentialAddress(externalData.getLlpUsualResidentialAddress());
        }
        return existingRegisters;
    }
}
