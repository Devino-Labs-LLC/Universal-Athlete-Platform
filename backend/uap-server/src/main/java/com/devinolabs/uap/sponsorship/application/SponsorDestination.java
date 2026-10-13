package com.devinolabs.uap.sponsorship.application;

import com.devinolabs.uap.sponsorship.api.PartnerPresentationRules;

public final class SponsorDestination {

	private SponsorDestination() {
	}

	public static String requireHttps(String value, String field) {
		try {
			return PartnerPresentationRules.requireHttps(value, field);
		}
		catch (IllegalArgumentException ex) {
			throw new SponsorInputInvalidException(ex.getMessage());
		}
	}

	public static String optionalHttps(String value, String field) {
		try {
			return PartnerPresentationRules.optionalHttps(value, field);
		}
		catch (IllegalArgumentException ex) {
			throw new SponsorInputInvalidException(ex.getMessage());
		}
	}
}
