package com.devinolabs.uap.billing.infrastructure.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider.VerifiedPurchase;
import com.devinolabs.uap.billing.application.InvalidApplePurchaseException;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.infrastructure.apple.AppleAppStoreServerClient.TransactionInfo;

/**
 * Fixture-only adapter tests. No live Apple App Store Server API calls.
 */
class AppleAppStoreBillingAdapterTests {

	private static final Instant NOW = Instant.parse("2026-09-28T18:00:00Z");

	private AppleBillingProperties properties;
	private AppleAppStoreServerClient serverClient;
	private AppleAppStoreBillingAdapter adapter;

	@BeforeEach
	void setUp() {
		properties = new AppleBillingProperties();
		properties.setEnabled(true);
		properties.setBundleId("com.devinolabs.athletereadiness");
		properties.setKeyId("KEYID");
		properties.setIssuerId("00000000-0000-0000-0000-000000000001");
		properties.setPrivateKeyPem("""
				-----BEGIN PRIVATE KEY-----
				TEST
				-----END PRIVATE KEY-----
				""");
		properties.setEnvironment("Sandbox");
		properties.getProducts().setIndividualPremiumMonthly("premium.monthly");
		properties.getProducts().setIndividualPremiumAnnual("premium.annual");
		serverClient = mock(AppleAppStoreServerClient.class);
		adapter = new AppleAppStoreBillingAdapter(properties, serverClient);
	}

	@Test
	void fakeTokenIsRejectedWithoutCallingApple() {
		assertThatThrownBy(() -> adapter.validateSignedTransaction("fake-token"))
				.isInstanceOf(InvalidApplePurchaseException.class);
	}

	@Test
	void fixtureSignedTransactionMapsToVerifiedPurchase() {
		String signed = signedJwt("""
				{"transactionId":"2001","originalTransactionId":"1001","productId":"premium.monthly",
				"bundleId":"com.devinolabs.athletereadiness","environment":"Sandbox",
				"purchaseDate":1727542800000,"expiresDate":1730134800000,"signedDate":1727542800000}
				""");
		when(serverClient.getTransactionInfo("2001")).thenReturn(new TransactionInfo(
				"2001",
				"1001",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				null,
				NOW.minusSeconds(60),
				NOW.plusSeconds(30 * 24 * 60 * 60),
				NOW,
				true,
				"1"));

		VerifiedPurchase purchase = adapter.validateSignedTransaction(signed);

		assertThat(purchase.planKey()).isEqualTo(CommercialPlanKey.INDIVIDUAL_PREMIUM);
		assertThat(purchase.billingCadence()).isEqualTo(BillingCadence.MONTHLY);
		assertThat(purchase.status()).isEqualTo(ProviderCommercialStatus.ACTIVE);
		assertThat(purchase.originalTransactionId()).isEqualTo("1001");
	}

	@Test
	void unknownProductFailsClosed() {
		String signed = signedJwt("""
				{"transactionId":"2002","originalTransactionId":"1002","productId":"unknown.product",
				"bundleId":"com.devinolabs.athletereadiness","environment":"Sandbox",
				"purchaseDate":1727542800000}
				""");
		when(serverClient.getTransactionInfo(anyString())).thenReturn(new TransactionInfo(
				"2002",
				"1002",
				"unknown.product",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				null,
				NOW,
				NOW.plusSeconds(60),
				NOW,
				true,
				"1"));

		assertThatThrownBy(() -> adapter.validateSignedTransaction(signed))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("allow-listed");
	}

	@Test
	void toSnapshotUsesAccountIdAsCustomerRefWhenAppAccountTokenAbsent() {
		UUID accountId = UUID.randomUUID();
		when(serverClient.getTransactionInfo("2003")).thenReturn(new TransactionInfo(
				"2003",
				"1003",
				"premium.annual",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				null,
				NOW,
				NOW.plusSeconds(365 * 24 * 60 * 60),
				NOW,
				true,
				"1"));
		VerifiedPurchase purchase = adapter.validateSignedTransaction(signedJwt("""
				{"transactionId":"2003","originalTransactionId":"1003","productId":"premium.annual",
				"bundleId":"com.devinolabs.athletereadiness","environment":"Sandbox",
				"purchaseDate":1727542800000}
				"""));

		assertThat(adapter.toSnapshot(purchase, accountId).providerCustomerRef())
				.isEqualTo(accountId.toString());
		assertThat(adapter.toSnapshot(purchase, accountId).billingCadence())
				.isEqualTo(BillingCadence.ANNUAL);
	}

	@Test
	void productionEnvironmentFailsClosed() {
		String signed = signedJwt("""
				{"transactionId":"2004","originalTransactionId":"1004","productId":"premium.monthly",
				"bundleId":"com.devinolabs.athletereadiness","environment":"Production",
				"purchaseDate":1727542800000}
				""");
		when(serverClient.getTransactionInfo("2004")).thenReturn(new TransactionInfo(
				"2004",
				"1004",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				"Production",
				null,
				NOW,
				NOW.plusSeconds(60),
				NOW,
				true,
				"1"));

		assertThatThrownBy(() -> adapter.validateSignedTransaction(signed))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("Production");
	}

	@Test
	void bundleMismatchFailsClosed() {
		String signed = signedJwt("""
				{"transactionId":"2005","originalTransactionId":"1005","productId":"premium.monthly",
				"bundleId":"com.other.app","environment":"Sandbox","purchaseDate":1727542800000}
				""");
		when(serverClient.getTransactionInfo("2005")).thenReturn(new TransactionInfo(
				"2005",
				"1005",
				"premium.monthly",
				"com.other.app",
				"Sandbox",
				null,
				NOW,
				NOW.plusSeconds(60),
				NOW,
				true,
				"1"));

		assertThatThrownBy(() -> adapter.validateSignedTransaction(signed))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("bundle");
	}

	@Test
	void billingGraceStatusMapsToPaymentAttention() {
		String signed = signedJwt("""
				{"transactionId":"2006","originalTransactionId":"1006","productId":"premium.monthly",
				"bundleId":"com.devinolabs.athletereadiness","environment":"Sandbox",
				"purchaseDate":1727542800000}
				""");
		when(serverClient.getTransactionInfo("2006")).thenReturn(new TransactionInfo(
				"2006",
				"1006",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				null,
				NOW.minusSeconds(60),
				NOW.plusSeconds(3 * 24 * 60 * 60),
				NOW,
				false,
				"4"));

		VerifiedPurchase purchase = adapter.validateSignedTransaction(signed);

		assertThat(purchase.status()).isEqualTo(ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED);
		assertThat(purchase.cancelAtPeriodEnd()).isTrue();
	}

	@Test
	void verifySignedNotificationRequiresClaimsAndLooksUpTransaction() {
		String signedTransaction = signedJwt("""
				{"transactionId":"2007","originalTransactionId":"1007","productId":"premium.monthly"}
				""");
		String notification = signedJwt("""
				{"notificationUUID":"notif-1","notificationType":"DID_FAIL_TO_RENEW","signedDate":1727542800000,
				"data":{"signedTransactionInfo":"%s"}}
				""".formatted(signedTransaction));
		when(serverClient.getTransactionInfo("2007")).thenReturn(new TransactionInfo(
				"2007",
				"1007",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				null,
				NOW.minusSeconds(60),
				NOW.plusSeconds(60),
				NOW,
				true,
				"3"));

		var verified = adapter.verifySignedNotification(notification);

		assertThat(verified.notificationUUID()).isEqualTo("notif-1");
		assertThat(verified.notificationType()).isEqualTo("DID_FAIL_TO_RENEW");
		assertThat(verified.purchase().status())
				.isEqualTo(ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED);
	}

	@Test
	void verifySignedNotificationRejectsIncompleteClaims() {
		String notification = signedJwt("""
				{"notificationUUID":"notif-2","signedDate":1727542800000}
				""");

		assertThatThrownBy(() -> adapter.verifySignedNotification(notification))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void expiredNotificationTypeMapsToEnded() {
		String signedTransaction = signedJwt("""
				{"transactionId":"2008","originalTransactionId":"1008","productId":"premium.monthly"}
				""");
		String notification = signedJwt("""
				{"notificationUUID":"notif-3","notificationType":"EXPIRED","signedDate":1727542800000,
				"data":{"signedTransactionInfo":"%s"}}
				""".formatted(signedTransaction));
		when(serverClient.getTransactionInfo("2008")).thenReturn(new TransactionInfo(
				"2008",
				"1008",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				null,
				NOW.minusSeconds(120),
				NOW.minusSeconds(30),
				NOW,
				false,
				"1"));

		assertThat(adapter.verifySignedNotification(notification).purchase().status())
				.isEqualTo(ProviderCommercialStatus.ENDED);
	}

	private static String signedJwt(String payloadJson) {
		String header = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
		String payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
		return header + "." + payload + ".sig";
	}

}
