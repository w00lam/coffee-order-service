package io.github.w00lam.coffeeorderservice.loadtest;

import io.github.w00lam.coffeeorderservice.CoffeeOrderServiceApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

public final class LoadTestApplication {

	private LoadTestApplication() {
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(CoffeeOrderServiceApplication.class)
				.sources(LoadTestAuthenticationConfiguration.class)
				.profiles("load-test")
				.run(args);
	}
}
