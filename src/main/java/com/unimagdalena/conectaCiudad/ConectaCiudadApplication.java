package com.unimagdalena.conectaCiudad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConectaCiudadApplication {
	public static void main(String[] args) {
		SpringApplication.run(ConectaCiudadApplication.class, args);
	}

}
