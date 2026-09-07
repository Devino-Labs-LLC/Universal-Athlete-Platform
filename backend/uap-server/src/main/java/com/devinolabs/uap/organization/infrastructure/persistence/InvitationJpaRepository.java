package com.devinolabs.uap.organization.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.organization.domain.InvitationStatus;

interface InvitationJpaRepository extends JpaRepository<InvitationJpaEntity, UUID> {

	Optional<InvitationJpaEntity> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from InvitationJpaEntity i where i.tokenHash = :tokenHash")
	Optional<InvitationJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from InvitationJpaEntity i where i.id = :id")
	Optional<InvitationJpaEntity> findByIdForUpdate(@Param("id") UUID id);

	List<InvitationJpaEntity> findAllByTeamId(UUID teamId);

	List<InvitationJpaEntity> findAllByOrganizationIdAndTeamIdIsNull(UUID organizationId);

	List<InvitationJpaEntity> findAllByInvitedEmailAndStatus(String invitedEmail, InvitationStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE InvitationJpaEntity i
			SET i.status = com.devinolabs.uap.organization.domain.InvitationStatus.ACCEPTED,
			    i.acceptedMembershipId = :acceptedMembershipId,
			    i.updatedAt = :updatedAt
			WHERE i.id = :id AND i.status = com.devinolabs.uap.organization.domain.InvitationStatus.PENDING
			""")
	int acceptIfPending(
			@Param("id") UUID id,
			@Param("acceptedMembershipId") UUID acceptedMembershipId,
			@Param("updatedAt") Instant updatedAt);

}
