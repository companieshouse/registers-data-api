package uk.gov.companieshouse.registers.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.companieshouse.api.registers.CompanyRegister;
import uk.gov.companieshouse.api.registers.InternalData;
import uk.gov.companieshouse.api.registers.InternalRegisters;
import uk.gov.companieshouse.api.registers.Registers;
import uk.gov.companieshouse.registers.config.ExceptionHandlerConfig;
import uk.gov.companieshouse.registers.config.WebSecurityConfig;
import uk.gov.companieshouse.registers.exception.ServiceUnavailableException;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;
import uk.gov.companieshouse.registers.model.ServiceStatus;
import uk.gov.companieshouse.registers.service.RegistersService;
import uk.gov.companieshouse.logging.Logger;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RegistersController.class)
@ContextConfiguration(classes = {RegistersController.class, ExceptionHandlerConfig.class})
@Import({WebSecurityConfig.class})
class RegistersControllerTest {
    private static final String URI = "/company/12345678/registers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Logger logger;

    @MockBean
    private RegistersService registersService;

    private final Gson gson = new GsonBuilder().setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    @DisplayName("Successful upsert request")
    void upsertCompanyRegisters() throws Exception {
        when(registersService.upsertCompanyRegisters(any(), any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthorised oauth2 upsert request")
    void upsertCompanyRegistersUnauthorisedOauth2() throws Exception {
        when(registersService.upsertCompanyRegisters(any(), any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "oauth2")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthorised upsert request")
    void upsertCompanyRegistersUnauthorised() throws Exception {
        when(registersService.upsertCompanyRegisters(any(), any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Server error upsert request")
    void upsertCompanyRegistersServerError() throws Exception {
        when(registersService.upsertCompanyRegisters(any(), any(), any())).thenReturn(ServiceStatus.SERVER_ERROR);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Client error upsert request")
    void upsertCompanyRegistersClientError() throws Exception {
        when(registersService.upsertCompanyRegisters(any(), any(), any())).thenReturn(ServiceStatus.CLIENT_ERROR);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Successful get company registers request")
    void getCompanyRegisters() throws Exception {
        CompanyRegistersDocument document = new CompanyRegistersDocument();
        CompanyRegister data = new CompanyRegister();
        document.setData(data);

        when(registersService.getCompanyRegisters(any())).thenReturn(Optional.of(document));

        MvcResult result = mockMvc.perform(get(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(data, objectMapper.readValue(result.getResponse().getContentAsString(), CompanyRegister.class));
    }

    @Test
    @DisplayName("Successful get company registers request with oauth2")
    void getCompanyRegistersOauth2() throws Exception {
        CompanyRegistersDocument document = new CompanyRegistersDocument();
        CompanyRegister data = new CompanyRegister();
        document.setData(data);

        when(registersService.getCompanyRegisters(any())).thenReturn(Optional.of(document));

        MvcResult result = mockMvc.perform(get(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(data, objectMapper.readValue(result.getResponse().getContentAsString(), CompanyRegister.class));
    }

//    @Test
//    @DisplayName("Document not found for get company registers request")
//    void getCompanyRegistersNotFound() throws Exception {
//        when(registersService.getCompanyRegisters(any())).thenReturn(Optional.empty());
//
//        mockMvc.perform(get(URI)
//                .contentType(APPLICATION_JSON)
//                .header("x-request-id", "5342342")
//                .header("ERIC-Identity", "Test-Identity")
//                .header("ERIC-Identity-Type", "Key")
//                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
//                .andExpect(status().isNotFound());
//    }

    @Test
    @DisplayName("MongoDB is unavailable for get company registers request")
    void getCompanyRegistersMongoUnavailable() throws Exception {
        when(registersService.getCompanyRegisters(any())).thenThrow(ServiceUnavailableException.class);

        mockMvc.perform(get(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Successful delete company registers request")
    void deleteCompanyRegisters() throws Exception {
        when(registersService.deleteCompanyRegisters(any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Server error delete request")
    void deleteCompanyRegistersServerError() throws Exception {
        when(registersService.deleteCompanyRegisters(any(), any())).thenReturn(ServiceStatus.SERVER_ERROR);

        mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Not found delete request")
    void deleteCompanyRegistersNotFound() throws Exception {
        when(registersService.deleteCompanyRegisters(any(), any())).thenReturn(ServiceStatus.CLIENT_ERROR);

        mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
    }

    private InternalRegisters getRequestBody() {
        InternalRegisters request = new InternalRegisters();
        request.setInternalData(new InternalData());
        request.setExternalData(new Registers());
        return request;
    }
}
