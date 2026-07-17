package io.github.w00lam.coffeeorderservice.point.application;

import io.github.w00lam.coffeeorderservice.point.domain.PointAmount;
import org.springframework.stereotype.Service;

@Service
public class PointChargeService {

	private final PointChargeRepository pointChargeRepository;

	public PointChargeService(PointChargeRepository pointChargeRepository) {
		this.pointChargeRepository = pointChargeRepository;
	}

	public PointChargeResult charge(String userId, PointAmount amount) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("user id must not be blank");
		}
		return pointChargeRepository.charge(userId, amount.value());
	}
}
