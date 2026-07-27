package com.devinolabs.uap.training.application;

public class PersonalRecordRebuildConflictException extends RuntimeException {

	public PersonalRecordRebuildConflictException(Throwable cause) {
		super("Personal records were modified concurrently during the rebuild; retry the request", cause);
	}

}
