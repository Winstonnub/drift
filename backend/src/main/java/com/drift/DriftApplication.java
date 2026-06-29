package com.drift; // Declares the Java Package

import org.springframework.boot.SpringApplication; // Import Spring Boot App launcher
import org.springframework.boot.autoconfigure.SpringBootApplication; // Import Spring Boot annotation
import org.springframework.boot.context.properties.ConfigurationPropertiesScan; // Import Spring Boot annotation for scanning configuration properties
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication // indicate this is main app config class. It will scan package and subpackages for Spring components (so com.drift.*)
@ConfigurationPropertiesScan
@EnableScheduling // Look for methods annotated with @Scheduled and run them on a schedule configured
public class DriftApplication {

	public static void main(String[] args) {
		SpringApplication.run(DriftApplication.class, args);
	}

}
