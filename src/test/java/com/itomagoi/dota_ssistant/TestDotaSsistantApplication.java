package com.itomagoi.dota_ssistant;

import com.itomagoi.dotaassistant.DotaSsistantApplication;
import org.springframework.boot.SpringApplication;

public class TestDotaSsistantApplication {

	public static void main(String[] args) {
		SpringApplication.from(DotaSsistantApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
