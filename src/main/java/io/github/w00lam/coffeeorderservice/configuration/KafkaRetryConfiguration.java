package io.github.w00lam.coffeeorderservice.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;

@Configuration
@EnableKafkaRetryTopic
@ConditionalOnProperty(name = "coffee.kafka.enabled", havingValue = "true")
public class KafkaRetryConfiguration {
}
