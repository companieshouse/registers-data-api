package uk.gov.companieshouse.registers.util;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.registers.CompanyRegister;

@ReadingConverter
public class RegistersReadConverter implements Converter<Document, CompanyRegister> {

    private final JsonMapper jsonMapper;

    public RegistersReadConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Read converter.
     * @param source source Document.
     * @return CompanyRegister object.
     */
    @Override
    public CompanyRegister convert(@NonNull Document source) {
        try {
            return jsonMapper.readValue(source.toJson(), CompanyRegister.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
