package com.harshshah.docqa;

import org.springframework.boot.SpringApplication;

public class TestDocqaApplication {

	public static void main(String[] args) {
		SpringApplication.from(DocqaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
