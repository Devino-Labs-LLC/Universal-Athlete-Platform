package com.devinolabs.uap.sponsorship.application;

import com.devinolabs.uap.sponsorship.api.PartnerPresentationRules;

public final class SponsorCopy {

	private SponsorCopy() {
	}

	public static String requirePlain(String value, String field, int maxLength) {
		try {
			return PartnerPresentationRules.requirePlain(value, field, maxLength);
		}
		catch (IllegalArgumentException ex) {
			throw new SponsorInputInvalidException(ex.getMessage());
		}
	}

	public static String optionalPlain(String value, String field, int maxLength) {
		try {
			return PartnerPresentationRules.optionalPlain(value, field, maxLength);
		}
		catch (IllegalArgumentException ex) {
			throw new SponsorInputInvalidException(ex.getMessage());
		}
	}

	static boolean containsMarkup(String value) {
		return PartnerPresentationRules.containsMarkup(value);
	}
}
