package uk.gov.companieshouse.registers.service;


import static uk.gov.companieshouse.registers.RegistersApplication.NAMESPACE;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.registers.exception.ServiceUnavailableException;
import uk.gov.companieshouse.registers.logging.DataMapHolder;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.model.Created;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;
import uk.gov.companieshouse.registers.model.ServiceStatus;
import uk.gov.companieshouse.registers.util.RegistersMapper;

@Service
public class RegistersServiceImpl implements RegistersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
            .withZone(ZoneId.of("Z"));

    private final RegistersRepository repository;
    private final RegistersMapper mapper;
    private final RegistersApiService registersApiService;

    public RegistersServiceImpl(RegistersRepository repository, RegistersMapper mapper,
            RegistersApiService registersApiService) {
        this.repository = repository;
        this.mapper = mapper;
        this.registersApiService = registersApiService;
    }

    @Override
    public ServiceStatus upsertCompanyRegisters(String companyNumber, InternalRegisters requestBody) {
        try {
            Optional<CompanyRegistersDocument> existingDocument = repository.findById(companyNumber);

            // If the document does not exist OR if the delta_at in the request is after the delta_at on the document
            if (existingDocument.isEmpty() ||
                    StringUtils.isBlank(existingDocument.get().getDeltaAt()) ||
                    // use compareTo >= 0 instead of isAfter to allow the same delta to be re-run to ensure the stream
                    // always gets updated by a retry if any call to /resource-changed fails the delta
                    !requestBody.getInternalData().getDeltaAt()
                            .isBefore(ZonedDateTime.parse(existingDocument.get().getDeltaAt(), FORMATTER)
                                    .toOffsetDateTime())) {
                CompanyRegistersDocument document = mapper.map(companyNumber, existingDocument.orElse(null),
                        requestBody);

                // If a document already exists and it has a created field, then reuse it
                // otherwise, set it to the delta's updated_at field
                existingDocument.map(CompanyRegistersDocument::getCreated)
                        .ifPresentOrElse(document::setCreated,
                                () -> document.setCreated(new Created().setAt(document.getUpdated().at())));

                // save the document before calling resource-changed
                repository.save(document);
                LOGGER.info("Company registers upserted in MongoDb", DataMapHolder.getLogMap());

                // call resource-changed after saving the document
                ServiceStatus serviceStatus = registersApiService.invokeChsKafkaApi(
                        new ResourceChangedRequest(companyNumber, null, false));
                LOGGER.info("ChsKafka api CHANGED invoked successfully", DataMapHolder.getLogMap());
                return serviceStatus;
            } else {
                LOGGER.error("Record not persisted as it is not the latest record", DataMapHolder.getLogMap());
                return ServiceStatus.CLIENT_ERROR;
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Illegal argument exception caught when processing upsert", ex, DataMapHolder.getLogMap());
            return ServiceStatus.SERVER_ERROR;
        } catch (DataAccessException ex) {
            LOGGER.error("Error connecting to MongoDB", ex, DataMapHolder.getLogMap());
            return ServiceStatus.SERVER_ERROR;
        }
    }

    @Override
    public Optional<CompanyRegistersDocument> getCompanyRegisters(String companyNumber) {
        try {
            return repository.findById(companyNumber);
        } catch (DataAccessException ex) {
            LOGGER.error("Failed to connect to MongoDb", ex, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException("Data access exception thrown when calling Mongo Repository");
        }
    }

    @Override
    public ServiceStatus deleteCompanyRegisters(String companyNumber) {
        try {
            Optional<CompanyRegistersDocument> document = getCompanyRegisters(companyNumber);
            if (document.isEmpty()) {
                LOGGER.info("Company registers do not exist", DataMapHolder.getLogMap());
                return ServiceStatus.CLIENT_ERROR;
            }

            ServiceStatus serviceStatus = registersApiService.invokeChsKafkaApi(
                    new ResourceChangedRequest(companyNumber, document.get().getData(), true));
            LOGGER.info("ChsKafka api DELETED invoked successfully", DataMapHolder.getLogMap());

            if (ServiceStatus.SUCCESS.equals(serviceStatus)) {
                repository.deleteById(companyNumber);
                LOGGER.info("Company registers deleted in MongoDb", DataMapHolder.getLogMap());
            }
            return serviceStatus;
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Error calling chs-kafka-api", ex, DataMapHolder.getLogMap());
            return ServiceStatus.SERVER_ERROR;
        } catch (DataAccessException ex) {
            LOGGER.error("Error connecting to MongoDB", ex, DataMapHolder.getLogMap());
            return ServiceStatus.SERVER_ERROR;
        }
    }
}
