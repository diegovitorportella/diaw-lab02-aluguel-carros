package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DemoApplication {

	/** Fuso do sistema: datas e horários (histórico, "hoje") seguem o horário de Brasília, e não o do servidor. */
	public static final String FUSO = "America/Sao_Paulo";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(FUSO));
		SpringApplication.run(DemoApplication.class, args);
	}

}
