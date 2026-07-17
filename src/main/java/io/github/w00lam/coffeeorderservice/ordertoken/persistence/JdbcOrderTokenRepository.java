package io.github.w00lam.coffeeorderservice.ordertoken.persistence;

import io.github.w00lam.coffeeorderservice.auth.application.UserNotFoundException;
import io.github.w00lam.coffeeorderservice.ordertoken.application.OrderTokenRepository;
import io.github.w00lam.coffeeorderservice.ordertoken.domain.OrderToken;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrderTokenRepository implements OrderTokenRepository {

	private final JdbcClient jdbcClient;

	public JdbcOrderTokenRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public void save(OrderToken token) {
		int inserted = jdbcClient.sql("""
				insert into order_tokens (order_token, user_id, status, issued_at, expires_at)
				select :orderToken, user_id, :status, :issuedAt, :expiresAt
				  from point_accounts
				 where user_id = :userId
				""")
				.param("orderToken", token.value())
				.param("userId", token.userId())
				.param("status", token.status().name())
				.param("issuedAt", token.issuedAt().atOffset(ZoneOffset.UTC))
				.param("expiresAt", token.expiresAt().atOffset(ZoneOffset.UTC))
				.update();
		if (inserted == 0) {
			throw new UserNotFoundException(token.userId());
		}
	}
}
