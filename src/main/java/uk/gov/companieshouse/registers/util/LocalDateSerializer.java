package uk.gov.companieshouse.registers.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateSerializer extends ValueSerializer<LocalDate> {

    @Override
    public void serialize(LocalDate localDate, JsonGenerator jsonGenerator, SerializationContext ctxt) throws JacksonException {
        if (localDate == null) {
            jsonGenerator.writeNull();
        } else {
            DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String format = localDate.atStartOfDay().format(dateTimeFormatter);
            jsonGenerator.writeRawValue("ISODate(\"" + format + "\")");
        }
    }
}
