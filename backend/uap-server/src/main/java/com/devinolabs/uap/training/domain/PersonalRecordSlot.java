package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * The uniqueness key of a current personal record projection within one exercise performance key.
 */
public record PersonalRecordSlot(PersonalRecordType recordType, String recordQualifier) {

	public PersonalRecordSlot {
		Objects.requireNonNull(recordType, "recordType must not be null");
	}

	/**
	 * Null-safe qualifier used by the database's generated uniqueness column.
	 */
	public String qualifierKey() {
		return recordQualifier == null ? "" : recordQualifier;
	}

}
