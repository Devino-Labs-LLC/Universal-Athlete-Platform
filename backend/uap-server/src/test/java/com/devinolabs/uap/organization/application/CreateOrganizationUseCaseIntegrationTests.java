package com.devinolabs.uap.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;
import com.devinolabs.uap.organization.domain.OrganizationStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CreateOrganizationUseCaseIntegrationTests {

	@Autowired
	private CreateOrganizationUseCase createOrganizationUseCase;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Test
	void createPersistsActiveOrganizationAndOwnerMembershipAtomically() {
		AccountId creator = AccountId.generate();

		OrganizationResult created = createOrganizationUseCase.execute(creator, "Atomic Org");

		assertThat(created.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(organizationRepository.findById(created.id())).isPresent();

		var membership = membershipRepository.findActiveOwner(created.id(), creator).orElseThrow();
		assertThat(membership.role()).isEqualTo(OrganizationMembershipRole.ORG_OWNER);
		assertThat(membership.status()).isEqualTo(OrganizationMembershipStatus.ACTIVE);
		assertThat(membership.athleteId()).isNull();
		assertThat(membership.accountId()).isEqualTo(creator);
		assertThat(membership.organizationId()).isEqualTo(created.id());
	}

}
