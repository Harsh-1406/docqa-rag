package com.harshshah.docqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DocqaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocqaApplication.class, args);
	}

}
