package com.luccavergara.solaris;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SolarisApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolarisApiApplication.class, args);
	}

}
