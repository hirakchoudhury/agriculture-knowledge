package com.agriknowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgriKnowledgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgriKnowledgeApplication.class, args);
	}

}
