package com.devinolabs.uap.integrations.application;

import java.util.Optional;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.IngestEvent;
import com.devinolabs.uap.integrations.domain.IngestEventId;

public interface IngestEventRepository {

	IngestEvent save(IngestEvent event);

	Optional<IngestEvent> findById(IngestEventId id);

	Optional<IngestEvent> findByProviderAndProviderEventId(HealthProviderKey provider, String providerEventId);
}
