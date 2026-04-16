package com.prenota24.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.prenota24.backend.config")
public class Prenota24BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(Prenota24BackendApplication.class, args);
	}

}
