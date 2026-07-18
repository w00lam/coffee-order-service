plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

val systemTest by sourceSets.creating {
	java.srcDir("src/systemTest/java")
	resources.srcDir("src/systemTest/resources")
	compileClasspath += sourceSets.main.get().output
	runtimeClasspath += output + compileClasspath
}

configurations[systemTest.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[systemTest.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

group = "io.github.w00lam"
version = "0.0.1-SNAPSHOT"
description = "Coffee order service"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-kafka")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-kafka")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<org.springframework.boot.gradle.tasks.bundling.BootJar>("systemTestBootJar") {
	group = "verification"
	description = "Builds the local multi-instance verification application."
	archiveClassifier = "system-test"
	mainClass = "io.github.w00lam.coffeeorderservice.systemtest.SystemTestApplication"
	targetJavaVersion.set(JavaVersion.VERSION_21)
	classpath(systemTest.runtimeClasspath)
}
