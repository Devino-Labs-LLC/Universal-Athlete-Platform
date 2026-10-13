package com.devinolabs.uap.sponsorship.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.sponsorship.api.SponsorPartnerPort;
import com.devinolabs.uap.sponsorship.infrastructure.persistence.SponsorRepository;

@Component
class SponsorPartnerPortAdapter implements SponsorPartnerPort {

	private final SponsorRepository sponsorRepository;

	SponsorPartnerPortAdapter(SponsorRepository sponsorRepository) {
		this.sponsorRepository = sponsorRepository;
	}

	@Override
	public Optional<PartnerRef> findPartner(UUID sponsorId) {
		if (sponsorId == null) {
			return Optional.empty();
		}
		return sponsorRepository.findById(sponsorId)
				.map(entity -> new PartnerRef(
						entity.getId(),
						entity.getDisplayName(),
						entity.getStatus(),
						entity.getLogoUrl()));
	}
}
