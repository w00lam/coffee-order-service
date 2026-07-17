package io.github.w00lam.coffeeorderservice.popularmenu.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PopularMenuProjectionUpdater {
	private final PopularMenuProjectionRepository repository;

	public PopularMenuProjectionUpdater(PopularMenuProjectionRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public boolean apply(OrderCompletedProjectionEvent event) {
		return repository.apply(event);
	}
}
