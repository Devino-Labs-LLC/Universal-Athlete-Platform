package com.devinolabs.uap.integrations.api.connections;

import java.util.List;
import java.util.UUID;

/**
 * Read-only connection status for the authenticated athlete.
 * Does not connect, sync, or ingest.
 */
public interface AthleteConnectionStatusPort {

	List<ConnectionView> listForAccount(UUID accountId);

}
