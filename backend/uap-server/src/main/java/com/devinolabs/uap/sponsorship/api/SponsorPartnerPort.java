package com.devinolabs.uap.sponsorship.api;

import java.util.Optional;
import java.util.UUID;

public interface SponsorPartnerPort {

	record PartnerRef(UUID id, String displayName, String status, String logoUrl) {
	}

	Optional<PartnerRef> findPartner(UUID sponsorId);
}
