package com.example.codecrafthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of CodeCraftHub.
 *
 * @SpringBootApplication turns on auto-configuration and scans this package
 * (and sub-packages such as controller, service, model) for Spring components.
 * Run it, then open http://localhost:8080/api/courses
 */
@SpringBootApplication
public class CodeCraftHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeCraftHubApplication.class, args);
	}

}
