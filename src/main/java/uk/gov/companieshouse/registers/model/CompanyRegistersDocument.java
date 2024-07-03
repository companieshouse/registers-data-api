package uk.gov.companieshouse.registers.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.registers.CompanyRegister;

import javax.persistence.Id;
import java.util.Objects;

@Document(collection = "company_registers")
public class CompanyRegistersDocument {

    @Id
    private String id;

    private Created created;

    @Field("data")
    private CompanyRegister data;

    @Field("delta_at")
    private String deltaAt;

    private Updated updated;

    public String getId() {
        return id;
    }

    public CompanyRegistersDocument setId(String id) {
        this.id = id;
        return this;
    }

    public Created getCreated() {
        return created;
    }

    public CompanyRegistersDocument setCreated(Created created) {
        this.created = created;
        return this;
    }

    public CompanyRegister getData() {
        return data;
    }

    public CompanyRegistersDocument setData(CompanyRegister data) {
        this.data = data;
        return this;
    }

    public String getDeltaAt() {
        return deltaAt;
    }

    public CompanyRegistersDocument setDeltaAt(String deltaAt) {
        this.deltaAt = deltaAt;
        return this;
    }

    public Updated getUpdated() {
        return updated;
    }

    public CompanyRegistersDocument setUpdated(Updated updated) {
        this.updated = updated;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompanyRegistersDocument document = (CompanyRegistersDocument) o;
        return Objects.equals(id, document.id) && Objects.equals(created, document.created) && Objects.equals(data, document.data) && Objects.equals(deltaAt, document.deltaAt) && Objects.equals(updated, document.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, created, data, deltaAt, updated);
    }
}
