package io.github.w00lam.coffeeorderservice.ordertoken.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.w00lam.coffeeorderservice.auth.application.UserNotFoundException;
import io.github.w00lam.coffeeorderservice.ordertoken.domain.OrderToken;
import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import(JdbcOrderTokenRepository.class)
@ActiveProfiles("test")
class JdbcOrderTokenRepositoryTest extends PostgreSqlIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private JdbcOrderTokenRepository repository;

	@BeforeEach
	void setUp() {
		jdbcClient.sql("delete from order_tokens").update();
		jdbcClient.sql("delete from point_accounts").update();
	}

	@Test
	void 유효한_사용자의_AVAILABLE_토큰을_저장한다() {
		insertUser("user-1");
		OrderToken token = OrderToken.issue("token-1", "user-1", Instant.parse("2026-07-17T12:00:00Z"));

		repository.save(token);

		String status = jdbcClient.sql("select status from order_tokens where order_token = 'token-1'")
				.query(String.class)
				.single();
		assertThat(status).isEqualTo("AVAILABLE");
	}

	@Test
	void 존재하지_않는_사용자에게는_토큰을_발급하지_않는다() {
		OrderToken token = OrderToken.issue("token-1", "missing-user", Instant.parse("2026-07-17T12:00:00Z"));

		assertThatThrownBy(() -> repository.save(token)).isInstanceOf(UserNotFoundException.class);
		assertThat(jdbcClient.sql("select count(*) from order_tokens").query(Long.class).single()).isZero();
	}

	private void insertUser(String userId) {
		jdbcClient.sql("insert into point_accounts (user_id, balance) values (:userId, 0)")
				.param("userId", userId)
				.update();
	}
}
