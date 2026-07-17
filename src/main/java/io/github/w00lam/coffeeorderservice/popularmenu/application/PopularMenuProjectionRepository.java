package io.github.w00lam.coffeeorderservice.popularmenu.application;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

public interface PopularMenuProjectionRepository {
	boolean apply(OrderCompletedProjectionEvent event);

	List<PopularMenuProjectionRow> findTopThree(
			Instant windowStart, Instant firstFullBucket, Instant fullBucketEnd, Instant asOf);

	record PopularMenuProjectionRow(
			String menuId, String name, boolean orderable, BigInteger totalQuantity, BigInteger orderCount) {
	}
}
