package com.prenota24.backend;

import com.prenota24.backend.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class Prenota24BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(Prenota24BackendApplication.class, args);
	}

}
