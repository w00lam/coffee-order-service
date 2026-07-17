package io.github.w00lam.coffeeorderservice.popularmenu.persistence;

import io.github.w00lam.coffeeorderservice.popularmenu.application.OrderCompletedProjectionEvent;
import io.github.w00lam.coffeeorderservice.popularmenu.application.OrderCompletedProjectionItem;
import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuProjectionRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPopularMenuProjectionRepository implements PopularMenuProjectionRepository {
	private final JdbcClient jdbcClient;

	public JdbcPopularMenuProjectionRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean apply(OrderCompletedProjectionEvent event) {
		int accepted = jdbcClient.sql("""
				insert into popular_menu_processed_events (event_id, order_id, completed_at, processed_at)
				values (:eventId, :orderId, :completedAt, clock_timestamp())
				on conflict (event_id) do nothing
				""").param("eventId", event.eventId()).param("orderId", event.orderId())
				.param("completedAt", Timestamp.from(event.completedAt())).update();
		if (accepted == 0) {
			return false;
		}

		event.items().stream().sorted(Comparator.comparing(OrderCompletedProjectionItem::menuId))
				.forEach(item -> applyItem(event, item));
		return true;
	}

	private void applyItem(OrderCompletedProjectionEvent event, OrderCompletedProjectionItem item) {
		jdbcClient.sql("""
				insert into popular_menu_contributions (event_id, order_id, menu_id, quantity, completed_at)
				values (:eventId, :orderId, :menuId, :quantity, :completedAt)
				""").param("eventId", event.eventId()).param("orderId", event.orderId())
				.param("menuId", item.menuId()).param("quantity", item.quantity())
				.param("completedAt", Timestamp.from(event.completedAt())).update();

		jdbcClient.sql("""
				insert into popular_menu_hourly_stats
				       (bucket_start, menu_id, total_quantity, order_count)
				values (date_trunc('hour', cast(:completedAt as timestamptz) at time zone 'UTC') at time zone 'UTC',
				        :menuId, :quantity, 1)
				on conflict (bucket_start, menu_id) do update
				set total_quantity = popular_menu_hourly_stats.total_quantity + excluded.total_quantity,
				    order_count = popular_menu_hourly_stats.order_count + 1
				""").param("completedAt", Timestamp.from(event.completedAt()))
				.param("menuId", item.menuId()).param("quantity", item.quantity()).update();
	}

	@Override
	public List<PopularMenuProjectionRow> findTopThree(
			Instant windowStart, Instant firstFullBucket, Instant fullBucketEnd, Instant asOf) {
		return jdbcClient.sql("""
				with full_bucket_values as (
				    select menu_id, total_quantity, order_count
				      from popular_menu_hourly_stats
				     where bucket_start >= :firstFullBucket
				       and bucket_start < :fullBucketEnd
				), edge_values as (
				    select menu_id, sum(quantity)::numeric as total_quantity, count(*)::numeric as order_count
				      from popular_menu_contributions
				     where completed_at > :windowStart
				       and completed_at <= :asOf
				       and (completed_at < :firstFullBucket or completed_at >= :fullBucketEnd)
				     group by menu_id
				), totals as (
				    select menu_id, sum(total_quantity) as total_quantity, sum(order_count) as order_count
				      from (
				          select * from full_bucket_values
				          union all
				          select * from edge_values
				      ) values_by_range
				     group by menu_id
				)
				select totals.menu_id, menus.name, menus.availability,
				       totals.total_quantity, totals.order_count
				  from totals
				  join menus on menus.menu_id = totals.menu_id
				 order by totals.total_quantity desc, totals.order_count desc, totals.menu_id asc
				 limit 3
				""").param("windowStart", Timestamp.from(windowStart))
				.param("firstFullBucket", Timestamp.from(firstFullBucket))
				.param("fullBucketEnd", Timestamp.from(fullBucketEnd))
				.param("asOf", Timestamp.from(asOf))
				.query((rs, rowNum) -> new PopularMenuProjectionRow(
						rs.getString("menu_id"), rs.getString("name"),
						"ORDERABLE".equals(rs.getString("availability")),
						rs.getBigDecimal("total_quantity").toBigIntegerExact(),
						rs.getBigDecimal("order_count").toBigIntegerExact()))
				.list();
	}
}
