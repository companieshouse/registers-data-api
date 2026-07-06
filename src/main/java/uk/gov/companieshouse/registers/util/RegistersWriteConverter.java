package uk.gov.companieshouse.registers.util;

import com.mongodb.BasicDBObject;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.registers.CompanyRegister;

@WritingConverter
public class RegistersWriteConverter implements Converter<CompanyRegister, BasicDBObject> {

    private final JsonMapper objectMapper;

    public RegistersWriteConverter(JsonMapper objectMapper) {
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
