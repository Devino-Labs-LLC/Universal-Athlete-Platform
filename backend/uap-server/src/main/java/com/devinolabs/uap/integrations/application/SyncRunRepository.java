package com.devinolabs.uap.integrations.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;

public interface SyncRunRepository {

	SyncRun save(SyncRun syncRun);

	Optional<SyncRun> findById(SyncRunId id);

	List<SyncRun> findByConnectionIdOrderByRequestedAtDesc(ConnectionId connectionId);

}
