package io.github.w00lam.coffeeorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoffeeOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoffeeOrderServiceApplication.class, args);
	}

}
