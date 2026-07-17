package io.github.w00lam.coffeeorderservice;

import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CoffeeOrderServiceApplicationTests extends PostgreSqlIntegrationTest {

	@Test
	void contextLoads() {
	}

}
