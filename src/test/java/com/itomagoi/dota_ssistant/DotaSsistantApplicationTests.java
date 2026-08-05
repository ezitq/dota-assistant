package com.itomagoi.dota_ssistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DotaSsistantApplicationTests {

	@Test
	void contextLoads() {
	}

}
