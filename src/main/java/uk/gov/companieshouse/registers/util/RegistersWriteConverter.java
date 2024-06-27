package uk.gov.companieshouse.registers.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;
import uk.gov.companieshouse.api.registers.CompanyRegister;

@WritingConverter
public class RegistersWriteConverter implements Converter<CompanyRegister, BasicDBObject> {

    private final ObjectMapper objectMapper;

    public RegistersWriteConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Write convertor.
     * @param source source Document.
     * @return charge BSON object.
     */
    @Override
    public BasicDBObject convert(@NonNull CompanyRegister source) {
        try {
            return BasicDBObject.parse(objectMapper.writeValueAsString(source));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
