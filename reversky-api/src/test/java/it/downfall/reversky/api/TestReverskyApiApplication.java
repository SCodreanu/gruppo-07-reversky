package it.downfall.reversky.api;

import org.springframework.boot.SpringApplication;

public class TestReverskyApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(ReverskyApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
