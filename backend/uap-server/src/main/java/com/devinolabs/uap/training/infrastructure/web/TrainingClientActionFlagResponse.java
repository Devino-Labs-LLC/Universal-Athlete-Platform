package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.application.TrainingClientActionFlag;

record TrainingClientActionFlagResponse(
		boolean allowed,
		String reasonCode) {

	static TrainingClientActionFlagResponse from(TrainingClientActionFlag flag) {
		if (flag == null) {
			return new TrainingClientActionFlagResponse(false, null);
		}
		return new TrainingClientActionFlagResponse(flag.allowed(), flag.reasonCode());
	}

}
