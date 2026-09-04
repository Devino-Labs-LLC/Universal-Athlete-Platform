package com.devinolabs.uap.training.infrastructure.persistence;

import org.hibernate.Hibernate;

/**
 * Forces initialization of lazy JPA associations before mapping to domain objects.
 */
final class JpaAssociationInitializer {

	private JpaAssociationInitializer() {
	}

	static void initialize(Object association) {
		Hibernate.initialize(association);
	}

}
