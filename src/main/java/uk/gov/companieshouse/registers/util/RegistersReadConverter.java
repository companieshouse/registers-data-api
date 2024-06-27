package uk.gov.companieshouse.registers.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;
import uk.gov.companieshouse.api.registers.CompanyRegister;

@ReadingConverter
public class RegistersReadConverter implements Converter<Document, CompanyRegister> {

    private final ObjectMapper objectMapper;

    public RegistersReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Write convertor.
     * @param source source Document.
     * @return charge BSON object.
     */
    @Override
    public CompanyRegister convert(@NonNull Document source) {
        try {
            return objectMapper.readValue(source.toJson(), CompanyRegister.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
