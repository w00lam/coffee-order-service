package io.github.w00lam.coffeeorderservice.popularmenu.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PopularMenuQueryService {
	private static final long WINDOW_HOURS = 168;
	private final PopularMenuProjectionRepository repository;

	public PopularMenuQueryService(PopularMenuProjectionRepository repository) {
		this.repository = repository;
	}

	public PopularMenuResult query(Instant requestedAsOf) {
		Instant asOf = requestedAsOf.truncatedTo(ChronoUnit.MICROS);
		Instant windowStart = asOf.minus(WINDOW_HOURS, ChronoUnit.HOURS);
		Instant firstFullBucket = ceilingHour(windowStart);
		Instant fullBucketEnd = asOf.truncatedTo(ChronoUnit.HOURS);
		var rows = repository.findTopThree(windowStart, firstFullBucket, fullBucketEnd, asOf);
		List<PopularMenuItemResult> menus = new ArrayList<>(rows.size());
		for (int index = 0; index < rows.size(); index++) {
			var row = rows.get(index);
			menus.add(new PopularMenuItemResult(index + 1, row.menuId(), row.name(), row.orderable(),
					row.totalQuantity(), row.orderCount()));
		}
		return new PopularMenuResult(asOf, windowStart, menus);
	}

	private Instant ceilingHour(Instant instant) {
		Instant floor = instant.truncatedTo(ChronoUnit.HOURS);
		return floor.equals(instant) ? floor : floor.plus(1, ChronoUnit.HOURS);
	}
}
