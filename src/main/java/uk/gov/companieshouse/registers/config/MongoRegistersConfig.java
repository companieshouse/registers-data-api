package uk.gov.companieshouse.registers.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ConditionalOnProperty(name = "mongodb.company_registers.company_registers", havingValue = "true")
@Configuration
@EnableTransactionManagement
public class MongoRegistersConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.name}")
    private String databaseName;

    @Value("${spring.mongodb.uri}")
    private String databaseUri;

    @Bean
    public @NonNull MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Override
    protected @NonNull String getDatabaseName() {
        return this.databaseName;
    }

    protected @NonNull String getDatabaseUri() {
        return this.databaseUri;
    }

    @Override
    public @NonNull MongoClient mongoClient() {
        final ConnectionString connectionString =
                new ConnectionString(getDatabaseUri());
        final MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(mongoClientSettings);
    }
}
