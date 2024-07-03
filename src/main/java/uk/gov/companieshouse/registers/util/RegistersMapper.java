package uk.gov.companieshouse.registers.util;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.registers.*;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.model.Updated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.companieshouse.api.registers.CompanyRegister.KindEnum.REGISTERS;

@Component
public class RegistersMapper {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");

    public CompanyRegistersDocument map(String companyNumber, InternalRegisters requestBody) {
        return new CompanyRegistersDocument()
                .setId(companyNumber)
                .setData(new CompanyRegister()
                        .registers(requestBody.getExternalData())
                        .kind(REGISTERS)
                        .links(new LinksType().self(String.format("/company/%s/registers", companyNumber)))
                        .etag(GenerateEtagUtil.generateEtag()))
                .setUpdated(new Updated(LocalDateTime.now()))
                .setDeltaAt(dateTimeFormatter.format(requestBody.getInternalData().getDeltaAt()));
    }
}
