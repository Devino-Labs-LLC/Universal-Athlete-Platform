package com.devinolabs.uap.sponsorship.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorRepository extends JpaRepository<SponsorEntity, UUID> {

	List<SponsorEntity> findAllByOrderByUpdatedAtDesc();
}
