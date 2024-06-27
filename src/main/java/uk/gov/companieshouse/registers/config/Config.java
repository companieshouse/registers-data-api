package uk.gov.companieshouse.registers.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import uk.gov.companieshouse.api.model.registers.RegistersLinks;
import uk.gov.companieshouse.api.registers.*;
import uk.gov.companieshouse.registers.util.*;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.registers.RegistersApplication.APPLICATION_NAME_SPACE;

@Configuration
public class Config {

    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger(APPLICATION_NAME_SPACE);
    }

    @Bean
    public Supplier<String> offsetDateTimeGenerator() {
        return () -> String.valueOf(OffsetDateTime.now());
    }

    /**
     * mongoCustomConversions.
     *
     * @return MongoCustomConversions.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        ObjectMapper objectMapper = mongoDbObjectMapper();
        return new MongoCustomConversions(List.of(new RegistersWriteConverter(objectMapper), new RegistersReadConverter(objectMapper)));
    }

    /**
     * Mongo DB Object Mapper.
     *
     * @return ObjectMapper.
     */
    private ObjectMapper mongoDbObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
//        module.addSerializer(String.class, new NonBlankStringSerializer());
//        module.addSerializer(RegistersLinks.class, new NotNullFieldObjectSerializer());
//        module.addSerializer(RegisterListDirectors.class, new NotNullFieldObjectSerializer());
//        module.addSerializer(RegisterListSecretaries.class, new NotNullFieldObjectSerializer());
//        module.addSerializer(RegisterListPersonsWithSignificantControl.class, new NotNullFieldObjectSerializer());
//        module.addSerializer(RegisterListMembers.class, new NotNullFieldObjectSerializer());
//        module.addSerializer(RegisterListUsualResidentialAddress.class, new NotNullFieldObjectSerializer());
//        module.addSerializer(RegisterListLLPMembers.class, new NotNullFieldObjectSerializer());
//        module.addSerializer(RegisterListLLPUsualResidentialAddress.class, new NotNullFieldObjectSerializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
}

