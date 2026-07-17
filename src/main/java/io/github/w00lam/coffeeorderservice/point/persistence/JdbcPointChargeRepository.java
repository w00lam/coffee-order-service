package io.github.w00lam.coffeeorderservice.point.persistence;

import io.github.w00lam.coffeeorderservice.point.application.PointBalanceOutOfRangeException;
import io.github.w00lam.coffeeorderservice.point.application.PointChargeRepository;
import io.github.w00lam.coffeeorderservice.point.application.PointChargeResult;
import io.github.w00lam.coffeeorderservice.auth.application.UserNotFoundException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPointChargeRepository implements PointChargeRepository {

	private final JdbcClient jdbcClient;

	public JdbcPointChargeRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public PointChargeResult charge(String userId, long amount) {
		ChargeRow row = jdbcClient.sql("""
				with updated as (
				    update point_accounts
				       set balance = balance + :amount
				     where user_id = :userId
				       and balance <= 9223372036854775807 - :amount
				 returning balance, clock_timestamp() as charged_at
				)
				select 'CHARGED' as status, balance, charged_at
				  from updated
				union all
				select case
				           when exists (select 1 from point_accounts where user_id = :userId)
				           then 'OUT_OF_RANGE'
				           else 'NOT_FOUND'
				       end as status,
				       null::bigint as balance,
				       null::timestamptz as charged_at
				 where not exists (select 1 from updated)
				""")
				.param("userId", userId)
				.param("amount", amount)
				.query((resultSet, rowNumber) -> new ChargeRow(
						resultSet.getString("status"),
						(Long) resultSet.getObject("balance"),
						resultSet.getTimestamp("charged_at")))
				.single();

		return switch (row.status()) {
			case "CHARGED" -> new PointChargeResult(amount, row.balance(), row.chargedAt().toInstant());
			case "OUT_OF_RANGE" -> throw new PointBalanceOutOfRangeException();
			case "NOT_FOUND" -> throw new UserNotFoundException(userId);
			default -> throw new IllegalStateException("Unknown point charge result: " + row.status());
		};
	}

	private record ChargeRow(String status, Long balance, Timestamp chargedAt) {
	}
}
