package com.devinolabs.uap.integrations.application;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@Configuration
class WearableAdapterConfiguration {

	@Bean
	List<WearableProviderAdapter> wearableProviderAdapters(IntegrationsProperties properties) {
		return List.of(
				new DeferredLiveWearableAdapter(HealthProviderKey.GARMIN, properties.getGarmin().isMock()),
				new DeferredLiveWearableAdapter(HealthProviderKey.FITBIT, properties.getFitbit().isMock()),
				new DeferredLiveWearableAdapter(HealthProviderKey.WHOOP, properties.getWhoop().isMock()),
				new DeferredLiveWearableAdapter(HealthProviderKey.OURA, properties.getOura().isMock()),
				new DeferredLiveWearableAdapter(HealthProviderKey.POLAR, properties.getPolar().isMock()));
	}
}
