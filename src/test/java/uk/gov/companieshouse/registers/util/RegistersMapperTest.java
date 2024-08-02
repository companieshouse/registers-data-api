package uk.gov.companieshouse.registers.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.registers.*;
import uk.gov.companieshouse.registers.model.CompanyRegistersDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.companieshouse.api.registers.CompanyRegister.KindEnum.REGISTERS;
import static uk.gov.companieshouse.api.registers.RegisterListDirectors.RegisterTypeEnum.DIRECTORS;
import static uk.gov.companieshouse.api.registers.RegisterListLLPMembers.RegisterTypeEnum.LLP_MEMBERS;
import static uk.gov.companieshouse.api.registers.RegisterListLLPUsualResidentialAddress.RegisterTypeEnum.LLP_USUAL_RESIDENTIAL_ADDRESS;
import static uk.gov.companieshouse.api.registers.RegisterListMembers.RegisterTypeEnum.MEMBERS;
import static uk.gov.companieshouse.api.registers.RegisterListSecretaries.RegisterTypeEnum.SECRETARIES;
import static uk.gov.companieshouse.api.registers.RegisterListUsualResidentialAddress.RegisterTypeEnum.USUAL_RESIDENTIAL_ADDRESS;
import static uk.gov.companieshouse.api.registers.RegisteredItems.RegisterMovedToEnum.PUBLIC_REGISTER;
import static uk.gov.companieshouse.api.registers.RegisteredItems.RegisterMovedToEnum.UNSPECIFIED_LOCATION;

@ExtendWith(MockitoExtension.class)
public class RegistersMapperTest {

    private static final String COMPANY_NUMBER = "123456789";
    private static final LocalDate DATE = LocalDate.of(2022, 11, 3);

    private RegistersMapper mapper;

    @Before
    public void setup() {
        mapper = new RegistersMapper();
    }

    @Test
    @DisplayName("Test should successfully map an InternalRegisters to a CompanyRegistersDocument")
    public void mapInsert() {
        // Given
        CompanyRegister external = new CompanyRegister();
        external.setRegisters(getDeltaRegisters());

        InternalData internal = new InternalData();
        internal.setDeltaAt(OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1000, ZoneOffset.MIN));
        internal.setUpdatedBy("example@ch.gov.uk");

        InternalRegisters requestBody = new InternalRegisters();
        requestBody.setInternalData(internal);
        requestBody.setExternalData(external.getRegisters());

        CompanyRegister expectedData = new CompanyRegister();
        expectedData.setRegisters(getDeltaRegisters());
        expectedData.setKind(REGISTERS);
        expectedData.setLinks(new LinksType().self(String.format("/company/%s/registers", COMPANY_NUMBER)));

        // When
        CompanyRegistersDocument document = mapper.map(COMPANY_NUMBER, null, requestBody);

        // Then
        assertEquals(COMPANY_NUMBER, document.getId());
        assertNull(document.getCreated());
        assertNotNull(document.getData().getEtag());
        assertEquals(expectedData.getRegisters(), document.getData().getRegisters());
        assertEquals(expectedData.getKind(), document.getData().getKind());
        assertEquals(expectedData.getLinks(), document.getData().getLinks());
        assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().at().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    @DisplayName("Test should successfully map an InternalRegisters to a CompanyRegistersDocument maintaining existing document registers")
    public void mapUpdate() {
        // Given
        CompanyRegistersDocument existingDocument = new CompanyRegistersDocument()
                .setData(new CompanyRegister().registers(getExistingRegisters()));

        CompanyRegister external = new CompanyRegister();
        external.setRegisters(getDeltaRegisters());

        InternalData internal = new InternalData();
        internal.setDeltaAt(OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1000, ZoneOffset.MIN));
        internal.setUpdatedBy("example@ch.gov.uk");

        InternalRegisters requestBody = new InternalRegisters();
        requestBody.setInternalData(internal);
        requestBody.setExternalData(external.getRegisters());

        CompanyRegister expectedData = new CompanyRegister();
        expectedData.setRegisters(getDeltaRegisters());
        expectedData.setKind(REGISTERS);
        expectedData.setLinks(new LinksType().self(String.format("/company/%s/registers", COMPANY_NUMBER)));

        // When
        CompanyRegistersDocument document = mapper.map(COMPANY_NUMBER, existingDocument, requestBody);

        // Then
        assertEquals(COMPANY_NUMBER, document.getId());
        assertNull(document.getCreated());
        assertNotNull(document.getData().getEtag());
        assertEquals(expectedData.getRegisters(), document.getData().getRegisters());
        assertEquals(expectedData.getKind(), document.getData().getKind());
        assertEquals(expectedData.getLinks(), document.getData().getLinks());
        assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().at().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    private Registers getDeltaRegisters() {
        List<RegisteredItems> items = new ArrayList<RegisteredItems>();
        items.add(new RegisteredItems(DATE, UNSPECIFIED_LOCATION, null));

        RegisterListDirectors directors = new RegisterListDirectors(DIRECTORS, items);

        return new Registers().directors(directors);
    }

    private Registers getExistingRegisters() {
        List<RegisteredItems> items = new ArrayList<RegisteredItems>();
        items.add(new RegisteredItems(DATE, PUBLIC_REGISTER, null));

        RegisterListDirectors directors = new RegisterListDirectors(DIRECTORS, items);
        RegisterListMembers members = new RegisterListMembers(MEMBERS, items);
        RegisterListSecretaries secretaries = new RegisterListSecretaries(SECRETARIES, items);
        RegisterListUsualResidentialAddress ura = new RegisterListUsualResidentialAddress(USUAL_RESIDENTIAL_ADDRESS, items);
        RegisterListLLPMembers llpMembers = new RegisterListLLPMembers(LLP_MEMBERS, items);
        RegisterListLLPUsualResidentialAddress llpURA = new RegisterListLLPUsualResidentialAddress(LLP_USUAL_RESIDENTIAL_ADDRESS, items);

        return new Registers().directors(directors).members(members).secretaries(secretaries)
                .usualResidentialAddress(ura).llpMembers(llpMembers).llpUsualResidentialAddress(llpURA);
    }
}
