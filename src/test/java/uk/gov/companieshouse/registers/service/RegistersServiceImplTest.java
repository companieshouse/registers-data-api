package uk.gov.companieshouse.registers.service;

import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.registers.CompanyRegister;
import uk.gov.companieshouse.api.registers.InternalData;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.api.registers.Registers;
import uk.gov.companieshouse.registers.exception.ServiceUnavailableException;
import uk.gov.companieshouse.registers.model.*;
import uk.gov.companieshouse.registers.util.RegistersMapper;
import uk.gov.companieshouse.logging.Logger;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistersServiceImplTest {

    private static final String COMPANY_NUMBER = "12345678";

    @Mock
    private RegistersRepository repository;

    @Mock
    private RegistersMapper mapper;

    @Mock
    private RegistersApiService registersApiService;

    @Mock
    private Logger logger;

    @InjectMocks
    private RegistersServiceImpl service;

    private InternalRegisters requestBody;
    private CompanyRegistersDocument document;
    private CompanyRegistersDocument existingDocument;

    @BeforeEach
    public void setUp() {
        OffsetDateTime date = OffsetDateTime.now();
        requestBody = new InternalRegisters();
        InternalData internal = new InternalData();
        internal.setDeltaAt(date);
        requestBody.setInternalData(internal);
        document = new CompanyRegistersDocument();
        document.setUpdated(new Updated(LocalDateTime.now()));
        final DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String dateString = date.format(dateTimeFormatter);
        document.setDeltaAt(dateString);
        existingDocument = new CompanyRegistersDocument();
        existingDocument.setDeltaAt("20221012091025774312");
        existingDocument.setData(new CompanyRegister().registers(new Registers()));
    }

    @Test
    @DisplayName("Test successful insert and call to chs kafka api")
    void insertCompanyRegisters() {
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, null, requestBody)).thenReturn(document);
        when(registersApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);

        ServiceStatus serviceStatus = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);
        assertNotNull(document.getCreated().getAt());
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(document);
    }

    @Test
    @DisplayName("Test successful update and call to chs kafka api")
    void updateCompanyRegisters() {
        existingDocument.setCreated(new Created().setAt(LocalDateTime.of(2022, 11, 2, 15, 55)));
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(existingDocument));
        when(mapper.map(COMPANY_NUMBER, existingDocument, requestBody)).thenReturn(document);
        when(registersApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);

        ServiceStatus serviceStatus = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);
        assertEquals(LocalDateTime.of(2022, 11, 2, 15, 55), document.getCreated().getAt());
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(document);
    }

    @Test
    @DisplayName("Test should not update registers record from out of date delta")
    void outOfDateDelta() {
        requestBody.getInternalData().setDeltaAt(OffsetDateTime.of(2018,1,1,0,0,0,0,ZoneOffset.UTC));
        when(repository.findById(any())).thenReturn(Optional.of(existingDocument));
        ServiceStatus serviceStatus = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.CLIENT_ERROR, serviceStatus);
        verifyNoInteractions(registersApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test should update registers if existing document has no delta_at field")
    void updateRegistersDeltaAtAbsent() {
        existingDocument.setDeltaAt(null);
        existingDocument.setCreated(new Created().setAt(LocalDateTime.of(2022, 11, 2, 15, 55)));
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(existingDocument));
        when(mapper.map(COMPANY_NUMBER, existingDocument, requestBody)).thenReturn(document);
        when(registersApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);

        ServiceStatus serviceStatus = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);
        assertEquals(LocalDateTime.of(2022, 11, 2, 15, 55), document.getCreated().getAt());
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(document);
    }

    @Test
    @DisplayName("Test should return status server error when save to repository throws data access exception on findById")
    void saveToRepositoryFindError() {
        when(repository.findById(COMPANY_NUMBER)).thenThrow(ServiceUnavailableException.class);

        ServiceStatus serviceStatus = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SERVER_ERROR, serviceStatus);
        verifyNoInteractions(registersApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test should return status server error when save to repository throws data access exception")
    void saveToRepositoryError() {
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, null, requestBody)).thenReturn(document);
        when(repository.save(document)).thenThrow(ServiceUnavailableException.class);

        ServiceStatus serviceStatus = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SERVER_ERROR, serviceStatus);
        verify(repository).save(document);
        verifyNoInteractions(registersApiService);
    }

    @Test
    @DisplayName("Test call to upsert company registers when chs-kafka-api unavailable returns server error")
    void updateCompanyRegistersServerError() {
        // given
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, null, requestBody)).thenReturn(document);
        when(repository.save(document)).thenThrow(ServiceUnavailableException.class);

        // when
        ServiceStatus actual = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verifyNoInteractions(registersApiService);
    }

    @Test
    @DisplayName("Test call to upsert company registers when chs-kafka-api unavailable throws illegal arg exception")
    void updateCompanyRegistersIllegalArg() {
        // given
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(existingDocument));
        when(mapper.map(COMPANY_NUMBER, existingDocument, requestBody)).thenReturn(document);
        when(registersApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        ServiceStatus actual = service.upsertCompanyRegisters("", COMPANY_NUMBER, requestBody);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).save(document);
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
    }

    @Test
    @DisplayName("Test successful call to get company registers")
    void getCompanyRegisters() {
        when(repository.findById(any())).thenReturn(Optional.of(document));

        Optional<CompanyRegistersDocument> actual = service.getCompanyRegisters(COMPANY_NUMBER);

        assertTrue(actual.isPresent());
        assertEquals(document, actual.get());
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to get company registers returns not found")
    void getCompanyRegistersNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        Optional<CompanyRegistersDocument> actual = service.getCompanyRegisters(COMPANY_NUMBER);

        assertEquals(Optional.empty(), actual);
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to get company registers throws service unavailable")
    void getCompanyRegistersDataAccessException() {
        when(repository.findById(any())).thenThrow(ServiceUnavailableException.class);

        Executable executable = () -> service.getCompanyRegisters(COMPANY_NUMBER);

        Exception exception = assertThrows(ServiceUnavailableException.class, executable);
        assertEquals("Data access exception thrown when calling Mongo Repository", exception.getMessage());
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test successful call to delete company registers")
    void deleteCompanyRegisters() {
        // given
        document.setData(new CompanyRegister());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        when(registersApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);

        // when
        ServiceStatus actual = service.deleteCompanyRegisters("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SUCCESS, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document.getData(), true));
        verify(repository).deleteById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to delete company registers when document not found returns client error")
    void deleteCompanyRegistersNotFound() {
        // given
        when(repository.findById(any())).thenReturn(Optional.empty());

        // when
        ServiceStatus actual = service.deleteCompanyRegisters("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.CLIENT_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verifyNoInteractions(registersApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test call to delete company registers when chs-kafka-api unavailable returns server error")
    void deleteCompanyRegistersServerError() {
        // given
        document.setData(new CompanyRegister());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        when(registersApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SERVER_ERROR);

        // when
        ServiceStatus actual = service.deleteCompanyRegisters("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document.getData(), true));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test call to delete company registers, when chs-kafka-api unavailable and throws illegal argument exception, returns server error")
    void deleteCompanyRegistersServerErrorIllegalArg() {
        // given
        document.setData(new CompanyRegister());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        when(registersApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        ServiceStatus actual = service.deleteCompanyRegisters("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document.getData(), true));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test call to delete company registers, when MongoDB unavailable and throws data access exception at findById, returns server error")
    void deleteCompanyRegistersServerErrorDataAccessExceptionFindById() {
        // given
        when(repository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        ServiceStatus actual = service.deleteCompanyRegisters("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verifyNoInteractions(registersApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test call to delete company registers, when MongoDB unavailable and throws data access exception at deleteById, returns server error")
    void deleteCompanyRegistersServerErrorDataAccessExceptionDeleteById() {
        // given
        document.setData(new CompanyRegister());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        when(registersApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);
        doThrow(ServiceUnavailableException.class).when(repository).deleteById(any());

        // when
        ServiceStatus actual = service.deleteCompanyRegisters("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(registersApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document.getData(), true));
        verify(repository).deleteById(COMPANY_NUMBER);
    }
}