package io.github.w00lam.coffeeorderservice.point.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.w00lam.coffeeorderservice.point.application.PointBalanceOutOfRangeException;
import io.github.w00lam.coffeeorderservice.point.application.UserNotFoundException;
import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@JdbcTest
@Import(JdbcPointChargeRepository.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JdbcPointChargeRepositoryTest extends PostgreSqlIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private JdbcPointChargeRepository repository;

	@BeforeEach
	void clearAccounts() {
		jdbcClient.sql("delete from point_accounts").update();
	}

	@Test
	void 현재_잔액에_포인트를_원자적으로_더한다() {
		insertAccount("user-1", 15_000);

		var result = repository.charge("user-1", 10_000);

		assertThat(result.chargedAmount()).isEqualTo(10_000);
		assertThat(result.balance()).isEqualTo(25_000);
		assertThat(result.chargedAt()).isNotNull();
	}

	@Test
	void 같은_금액의_성공한_POST는_각각_충전된다() {
		insertAccount("user-1", 0);

		repository.charge("user-1", 10_000);
		var second = repository.charge("user-1", 10_000);

		assertThat(second.balance()).isEqualTo(20_000);
	}

	@Test
	void 동시에_충전해도_증가분이_유실되지_않는다() throws Exception {
		insertAccount("user-1", 0);
		List<Callable<Long>> charges = new ArrayList<>();
		for (int index = 0; index < 20; index++) {
			charges.add(() -> repository.charge("user-1", 1).balance());
		}

		try (var executor = Executors.newFixedThreadPool(8)) {
			executor.invokeAll(charges).forEach(future -> assertThat(future).succeedsWithin(java.time.Duration.ofSeconds(10)));
		}

		Long balance = jdbcClient.sql("select balance from point_accounts where user_id = 'user-1'")
				.query(Long.class)
				.single();
		assertThat(balance).isEqualTo(20);
	}

	@Test
	void 존재하지_않는_사용자는_거부한다() {
		assertThatThrownBy(() -> repository.charge("missing-user", 10_000))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void 충전_후_잔액이_signed_64_bit_범위를_넘으면_잔액을_변경하지_않는다() {
		insertAccount("user-1", Long.MAX_VALUE);

		assertThatThrownBy(() -> repository.charge("user-1", 1))
				.isInstanceOf(PointBalanceOutOfRangeException.class);

		Long balance = jdbcClient.sql("select balance from point_accounts where user_id = 'user-1'")
				.query(Long.class)
				.single();
		assertThat(balance).isEqualTo(Long.MAX_VALUE);
	}

	private void insertAccount(String userId, long balance) {
		jdbcClient.sql("insert into point_accounts (user_id, balance) values (:userId, :balance)")
				.param("userId", userId)
				.param("balance", balance)
				.update();
	}
}
