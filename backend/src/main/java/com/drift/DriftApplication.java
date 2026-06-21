package com.drift; // Declares the Java Package

import org.springframework.boot.SpringApplication; // Import Spring Boot App launcher
import org.springframework.boot.autoconfigure.SpringBootApplication; // Import Spring Boot annotation

@SpringBootApplication // indicate this is main app config class. It will scan package and subpackages for Spring components (so com.drift.*)
public class DriftApplication {

	public static void main(String[] args) {
		SpringApplication.run(DriftApplication.class, args);
	}

}
