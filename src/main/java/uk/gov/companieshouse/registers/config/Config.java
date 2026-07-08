package uk.gov.companieshouse.registers.config;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.companieshouse.registers.util.EmptyFieldDeserializer;
import uk.gov.companieshouse.registers.util.LocalDateDeSerializer;
import uk.gov.companieshouse.registers.util.LocalDateSerializer;
import uk.gov.companieshouse.registers.util.RegistersReadConverter;
import uk.gov.companieshouse.registers.util.RegistersWriteConverter;

@Configuration
public class Config {

    @Bean
    public Supplier<Instant> instantSupplier() {
        return Instant::now;
    }

    /**
     * mongoCustomConversions.
     *
     * @return MongoCustomConversions.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        JsonMapper objectMapper = mongoDbObjectMapper();
        return new MongoCustomConversions(
                List.of(new RegistersWriteConverter(objectMapper), new RegistersReadConverter(objectMapper)));
    }

    /**
     * Mongo DB Object Mapper.
     *
     * @return JsonMapper.
     */
    private JsonMapper mongoDbObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        module.addSerializer(LocalDate.class, new LocalDateSerializer());

        return JsonMapper.builder()
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
                .addModule(new SimpleModule().addDeserializer(String.class, new EmptyFieldDeserializer()))
                .addModule(module)
                .build();
    }

}

