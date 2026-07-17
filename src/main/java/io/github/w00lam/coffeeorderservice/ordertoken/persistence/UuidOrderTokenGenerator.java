package io.github.w00lam.coffeeorderservice.ordertoken.persistence;

import io.github.w00lam.coffeeorderservice.ordertoken.application.OrderTokenGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidOrderTokenGenerator implements OrderTokenGenerator {

	@Override
	public String generate() {
		return UUID.randomUUID().toString();
	}
}
