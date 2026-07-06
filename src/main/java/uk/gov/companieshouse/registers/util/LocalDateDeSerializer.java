package uk.gov.companieshouse.registers.util;


import static java.time.ZoneOffset.UTC;
import static uk.gov.companieshouse.registers.RegistersApplication.NAMESPACE;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.registers.exception.BadRequestException;
import uk.gov.companieshouse.registers.logging.DataMapHolder;


/**
 * Custom Jackson deserializer for deserializing LocalDate objects from JSON.
 * <p>
 * This deserializer handles two date formats:
 * <ul>
 *     <li>ISO 8601 string format: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}</li>
 *     <li>MongoDB extended JSON format: {@code {"$numberLong": milliseconds}}, where milliseconds
 * represent the number of milliseconds since epoch (1970-01-01)</li>
 * </ul>
 * <p>
 * If the deserialization encounters any exception, a {@link BadRequestException} is thrown
 * with the exception message, and the error is logged.
 * <p>
 */
public class LocalDateDeSerializer extends ValueDeserializer<LocalDate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws JacksonException {
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            JsonNode jsonNode = jsonParser.readValueAsTree();
            JsonNode dateNode = jsonNode.get("$date");

            /* If textValue() returns a value we received a string of format yyyy-MM-dd'T'HH:mm:ss'Z
             * and use dateTimeFormatter to return LocalDate.
             *
             * However, we received a long of milliseconds away from 01/01/1970 and need to return
             * a LocalDate without dateTimeFormatter.
             */
            return dateNode.stringValue() != null ?
                    LocalDate.parse(dateNode.stringValue(), dateTimeFormatter) :
                    LocalDate.ofInstant(Instant.ofEpochMilli(dateNode.get("$numberLong").asLong()), UTC);
        } catch (Exception exception) {
            LOGGER.error("Deserialization failed", exception, DataMapHolder.getLogMap());
            throw new BadRequestException(exception.getMessage());
        }

    }
}
