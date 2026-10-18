package com.tracepilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.config.additional-location=optional:file:./config/"
})
@ActiveProfiles("simulator")
class TracepilotBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
