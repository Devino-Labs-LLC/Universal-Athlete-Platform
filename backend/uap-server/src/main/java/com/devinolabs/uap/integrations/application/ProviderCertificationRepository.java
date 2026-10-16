package com.devinolabs.uap.integrations.application;

import java.util.Optional;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.ProviderCertificationState;

public interface ProviderCertificationRepository {

	Optional<ProviderCertificationState> findState(HealthProviderKey provider);
}
