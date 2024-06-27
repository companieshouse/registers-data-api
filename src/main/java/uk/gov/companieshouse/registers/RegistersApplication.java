package uk.gov.companieshouse.registers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RegistersApplication {

	public static final String APPLICATION_NAME_SPACE = "registers-data-api";

	public static void main(String[] args) {
		SpringApplication.run(RegistersApplication.class, args);
	}

}
