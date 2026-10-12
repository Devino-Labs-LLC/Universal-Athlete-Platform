-- V6 AI Coach conversations. Owned by the athlete. Not State Engine evidence.

CREATE TABLE ai_coach_conversations (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_ai_coach_conversations_athlete FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	INDEX idx_ai_coach_conversations_athlete_updated (athlete_id, updated_at)
);

CREATE TABLE ai_coach_messages (
	id BINARY(16) NOT NULL,
	conversation_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	role VARCHAR(16) NOT NULL,
	body VARCHAR(4000) NOT NULL,
	content_kind VARCHAR(32) NOT NULL,
	forecast BOOLEAN NOT NULL,
	uncertainty VARCHAR(500) NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_ai_coach_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai_coach_conversations (id),
	CONSTRAINT fk_ai_coach_messages_athlete FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT ck_ai_coach_messages_role CHECK (role IN ('USER', 'ASSISTANT')),
	CONSTRAINT ck_ai_coach_messages_kind CHECK (
		content_kind IN ('EXPLANATION', 'FORECAST', 'REFUSAL', 'FALLBACK')
	),
	INDEX idx_ai_coach_messages_conversation_created (conversation_id, created_at)
);
