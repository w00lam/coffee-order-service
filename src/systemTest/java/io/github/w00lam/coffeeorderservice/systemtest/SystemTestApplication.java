package io.github.w00lam.coffeeorderservice.systemtest;

import io.github.w00lam.coffeeorderservice.CoffeeOrderServiceApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

public final class SystemTestApplication {

	private SystemTestApplication() {
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(CoffeeOrderServiceApplication.class)
				.sources(SystemTestAuthenticationConfiguration.class)
				.profiles("system-test")
				.run(args);
	}
}
