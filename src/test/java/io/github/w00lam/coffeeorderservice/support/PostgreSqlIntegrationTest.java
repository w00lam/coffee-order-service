package io.github.w00lam.coffeeorderservice.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
public abstract class PostgreSqlIntegrationTest {

	@Container
	@ServiceConnection
	protected static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.10");
}
