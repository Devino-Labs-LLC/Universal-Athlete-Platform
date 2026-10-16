package com.devinolabs.uap.integrations.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.ProviderCertificationState;

@Service
public class WearableCertificationService {

	private final ProviderCertificationRepository repository;

	WearableCertificationService(ProviderCertificationRepository repository) {
		this.repository = Objects.requireNonNull(repository);
	}

	@Transactional(readOnly = true)
	public ProviderCertificationState stateOf(HealthProviderKey provider) {
		return repository.findState(provider).orElse(ProviderCertificationState.DISABLED);
	}

	public boolean allowsConnect(HealthProviderKey provider) {
		ProviderCertificationState state = stateOf(provider);
		return state == ProviderCertificationState.DEVELOPMENT || state == ProviderCertificationState.CERTIFIED;
	}

	public boolean allowsLivePull(HealthProviderKey provider) {
		return stateOf(provider) == ProviderCertificationState.CERTIFIED;
	}
}
