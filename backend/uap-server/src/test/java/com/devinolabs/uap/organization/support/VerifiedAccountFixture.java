package com.devinolabs.uap.organization.support;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.identity.application.RegisterAccountResult;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;

/**
 * Test helper that registers and verifies identity accounts (optionally with athlete profiles).
 */
public final class VerifiedAccountFixture {

	private static final AtomicInteger COUNTER = new AtomicInteger();
	private static final String PASSWORD = "Password123!";

	private final RegisterAccountUseCase registerAccountUseCase;
	private final VerifyEmailUseCase verifyEmailUseCase;
	private final InMemoryVerificationNotifier verificationNotifier;
	private final CreateAthleteProfileUseCase createAthleteProfileUseCase;

	public VerifiedAccountFixture(
			RegisterAccountUseCase registerAccountUseCase,
			VerifyEmailUseCase verifyEmailUseCase,
			InMemoryVerificationNotifier verificationNotifier,
			CreateAthleteProfileUseCase createAthleteProfileUseCase) {
		this.registerAccountUseCase = registerAccountUseCase;
		this.verifyEmailUseCase = verifyEmailUseCase;
		this.verificationNotifier = verificationNotifier;
		this.createAthleteProfileUseCase = createAthleteProfileUseCase;
	}

	public VerifiedAccount registerVerified(String localPart) {
		VerifiedAccount unverified = registerUnverified(localPart);
		String rawToken = verificationNotifier.lastMessage().orElseThrow().rawToken();
		verifyEmailUseCase.verify(rawToken);
		return unverified;
	}

	/** Registers an account without verifying email (for EMAIL_UNVERIFIED accept paths). */
	public VerifiedAccount registerUnverified(String localPart) {
		String email = localPart + "+" + COUNTER.incrementAndGet() + "@example.com";
		RegisterAccountResult registered = registerAccountUseCase.register(email, PASSWORD);
		return new VerifiedAccount(registered.accountId(), email.toLowerCase());
	}

	public VerifiedAccount registerVerifiedAthlete(String localPart) {
		VerifiedAccount account = registerVerified(localPart);
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(account.accountId().value()),
				"Test",
				"Athlete",
				LocalDate.of(2000, 1, 1),
				Sex.FEMALE,
				Height.ofCentimeters(170),
				Weight.ofKilograms(65),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return account;
	}

	public record VerifiedAccount(AccountId accountId, String email) {
	}

}
