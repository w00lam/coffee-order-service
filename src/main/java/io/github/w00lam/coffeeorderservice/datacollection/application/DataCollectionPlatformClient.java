package io.github.w00lam.coffeeorderservice.datacollection.application;

public interface DataCollectionPlatformClient {
	void send(DataCollectionOrder order);
}
