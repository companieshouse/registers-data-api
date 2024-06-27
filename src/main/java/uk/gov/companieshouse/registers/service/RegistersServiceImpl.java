package uk.gov.companieshouse.registers.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.registers.CompanyRegister;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.registers.exception.ServiceUnavailableException;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.model.Created;
import uk.gov.companieshouse.registers.model.ResourceChangedRequest;
import uk.gov.companieshouse.registers.model.ServiceStatus;
import uk.gov.companieshouse.registers.util.RegistersMapper;
import uk.gov.companieshouse.logging.Logger;

@Service
public class RegistersServiceImpl implements RegistersService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS").withZone(ZoneId.of("Z"));

    private final Logger logger;
    private final RegistersRepository repository;
    private final RegistersMapper mapper;
    private final RegistersApiService registersApiService;

    public RegistersServiceImpl(Logger logger, RegistersRepository repository, RegistersMapper mapper, RegistersApiService registersApiService) {
        this.logger = logger;
        this.repository = repository;
        this.mapper = mapper;
        this.registersApiService = registersApiService;
    }

    @Override
    public ServiceStatus upsertCompanyRegisters(String contextId, String companyNumber, InternalRegisters requestBody) {
        try {
            Optional<CompanyRegistersDocument> existingDocument = repository.findById(companyNumber);

            // If the document does not exist OR if the delta_at in the request is after the delta_at on the document
            if (existingDocument.isEmpty() ||
                    StringUtils.isBlank(existingDocument.get().getDeltaAt()) ||
                    requestBody.getInternalData().getDeltaAt()
                            .isAfter(ZonedDateTime.parse(existingDocument.get().getDeltaAt(), FORMATTER)
                                    .toOffsetDateTime())) {
                CompanyRegistersDocument document = mapper.map(companyNumber, requestBody);

                // If a document already exists and it has a created field, then reuse it
                // otherwise, set it to the delta's updated_at field
                existingDocument.map(CompanyRegistersDocument::getCreated)
                        .ifPresentOrElse(document::setCreated,
                                () -> document.setCreated(new Created().setAt(document.getUpdated().at())));

                ServiceStatus serviceStatus = registersApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, companyNumber, null, false));
                logger.info(String.format("ChsKafka api CHANGED invoked updated successfully for context id: %s and company number: %s",
                        contextId,
                        companyNumber));

                if (ServiceStatus.SUCCESS.equals(serviceStatus)) {
                    repository.save(document);
                    logger.info(String.format("Company registers for company number: %s updated in MongoDb for context id: %s",
                            companyNumber,
                            contextId));
                }
                return serviceStatus;
            } else {
                logger.info(String.format("Record for company %s not persisted as it is not the latest record.", companyNumber));
                return ServiceStatus.CLIENT_ERROR;
            }
        } catch (IllegalArgumentException ex) {
            logger.error("Illegal argument exception caught when processing upsert", ex);
            return ServiceStatus.SERVER_ERROR;
        } catch (DataAccessException ex) {
            logger.error("Error connecting to MongoDB");
            return ServiceStatus.SERVER_ERROR;
        }
    }

    @Override
    public Optional<CompanyRegistersDocument> getCompanyRegisters(String companyNumber) {
        try {
            return repository.findById(companyNumber);
        } catch (DataAccessException ex) {
            logger.error("Failed to connect to MongoDb", ex);
            throw new ServiceUnavailableException("Data access exception thrown when calling Mongo Repository");
        }
    }

    @Override
    public ServiceStatus deleteCompanyRegisters(String contextId, String companyNumber) {
        try {
            Optional<CompanyRegistersDocument> document = repository.findById(companyNumber);
            if (document.isEmpty()) {
                logger.error(String.format("Company registers do not exist for company number %s", companyNumber));
                return ServiceStatus.CLIENT_ERROR;
            }

            CompanyRegistersDocument registersDocument = document.get();
            logger.info(String.format("BEFORE DELETE Got company registers for company number %s, registersDocument=[%s]", companyNumber,
                    registersDocument));

            CompanyRegister registerData = registersDocument.getData();
            logger.info(String.format("BEFORE DELETE Got company registers for company number %s, registerData=[%s]", companyNumber,
                    registerData));

            CompanyRegister registerApiResponse = new CompanyRegister().etag(registerData.getEtag()).
                    registers(registerData.getRegisters()).companyNumber(registerData.getCompanyNumber()).
                    links(registerData.getLinks()).kind(registerData.getKind());
            logger.info(String.format("BEFORE DELETE Got company registers for company number %s, registerApiResponse=[%s]", companyNumber,
                    registerApiResponse));


            ServiceStatus serviceStatus = registersApiService.invokeChsKafkaApi(
                    new ResourceChangedRequest(contextId, companyNumber, registerApiResponse, true));
            logger.info(String.format("ChsKafka api DELETED invoked successfully for context id: %s and company number: %s", contextId, companyNumber));

            if (ServiceStatus.SUCCESS.equals(serviceStatus)) {
                repository.deleteById(companyNumber);
                logger.info(String.format("Company registers for company number: %s deleted in MongoDb for context id: %s", companyNumber, contextId));
            }
            return serviceStatus;
        } catch (IllegalArgumentException ex) {
            logger.error("Error calling chs-kafka-api");
            return ServiceStatus.SERVER_ERROR;
        } catch (DataAccessException ex) {
            logger.error("Error connecting to MongoDB");
            return ServiceStatus.SERVER_ERROR;
        }
    }
}
