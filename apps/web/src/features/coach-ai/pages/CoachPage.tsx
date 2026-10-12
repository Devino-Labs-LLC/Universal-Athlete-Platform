import { useState } from 'react';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { COACH_STARTERS, type CoachConversation, type CoachSummary } from '@/features/coach-ai/api/coachApi';
import { coachFailureMessage, useCoach } from '@/features/coach-ai/hooks/useCoach';
import styles from '@/features/coach-ai/pages/CoachPage.module.scss';

export function CoachPage() {
  const coach = useCoach();
  const [notice, setNotice] = useState<string | null>(null);

  if (coach.summary.isLoading || coach.conversation.isLoading) {
    return <LoadingView message="Loading Coach…" />;
  }

  if (coach.summary.isError || !coach.summary.data) {
    return (
      <ErrorView
        title="Coach is unavailable"
        message={coachFailureMessage(coach.summary.error)}
        onRetry={() => void coach.summary.refetch()}
      />
    );
  }

  return (
    <CoachPanel
      summary={coach.summary.data}
      conversation={coach.conversation.data ?? { id: null, updatedAt: null, messages: [] }}
      draft={coach.draft}
      sending={coach.send.isPending}
      notice={notice ?? (coach.send.isError ? coachFailureMessage(coach.send.error) : null)}
      onDraft={coach.setDraft}
      onSend={(text) => {
        setNotice(null);
        coach.send.mutate(text, {
          onError: (error) => setNotice(coachFailureMessage(error)),
        });
      }}
      onClear={() => {
        setNotice(null);
        coach.clear.mutate();
      }}
    />
  );
}

export function CoachPanel({
  summary,
  conversation,
  draft,
  sending,
  notice,
  onDraft,
  onSend,
  onClear,
}: {
  summary: CoachSummary;
  conversation: CoachConversation;
  draft: string;
  sending: boolean;
  notice: string | null;
  onDraft: (value: string) => void;
  onSend: (text: string) => void;
  onClear: () => void;
}) {
  const stale = summary.facts.some((fact) => fact.stale);
  const noSignals = summary.facts
    .filter((fact) => fact.topic !== 'Profile')
    .every((fact) => fact.availability !== 'PRESENT');

  return (
    <section className={styles.page} aria-label="AI Coach">
      <p className={styles.intro}>
        AI Coach explains deterministic UAP state. It does not calculate your readiness score.
      </p>
      <div className={styles.facts}>
        <h2 className={styles.sectionTitle}>UAP facts for {summary.asOf}</h2>
        {stale ? (
          <p className={styles.stale} role="status">
            Some UAP data is stale. Coach should treat it as stale.
          </p>
        ) : null}
        {noSignals ? (
          <p className={styles.unavailable} role="status">
            There is no readiness, recovery, recommendation, or connected evidence for this date.
          </p>
        ) : null}
        <ul>
          {summary.facts.map((fact) => (
            <li key={`${fact.topic}-${fact.text}`} className={styles.fact}>
              <span className={styles.topic}>{fact.topic}</span>
              <span>
                {' '}
                · {fact.availability === 'PRESENT' ? 'Recorded by UAP' : 'Not available'}
                {fact.stale ? ' · Stale' : ''}
              </span>
              <p>{fact.text}</p>
            </li>
          ))}
        </ul>
      </div>

      <div className={styles.thread} aria-live="polite">
        <h2 className={styles.sectionTitle}>Conversation</h2>
        {conversation.messages.length === 0 ? (
          <p className={styles.unavailable}>No Coach messages yet.</p>
        ) : (
          conversation.messages.map((message) => (
            <p
              key={message.id}
              className={
                message.role === 'USER' ? styles.user : message.forecast ? styles.forecast : styles.assistant
              }
            >
              <span className={styles.kind}>
                {message.role === 'USER'
                  ? 'You'
                  : message.forecast
                    ? 'AI forecast — not a readiness score'
                    : 'AI Coach'}
              </span>
              {message.text}
              {message.uncertainty ? ` Uncertainty: ${message.uncertainty}` : ''}
            </p>
          ))
        )}
      </div>

      {notice ? (
        <p className={styles.notice} role="alert">
          {notice}
        </p>
      ) : null}
      {!summary.aiEnabled ? (
        <p className={styles.notice} role="status">
          AI Coach is turned off. UAP facts above are unchanged.
        </p>
      ) : null}

      <form
        className={styles.composer}
        onSubmit={(event) => {
          event.preventDefault();
          const text = draft.trim();
          if (text && !sending) {
            onSend(text);
          }
        }}
      >
        <div className={styles.starters}>
          {COACH_STARTERS.map((prompt) => (
            <button
              key={prompt}
              type="button"
              className={styles.starter}
              disabled={sending}
              onClick={() => onSend(prompt)}
            >
              {prompt}
            </button>
          ))}
        </div>
        <div className={styles.row}>
          <label className={styles.visuallyHidden} htmlFor="coach-input">
            Message AI Coach
          </label>
          <input
            id="coach-input"
            className={styles.input}
            value={draft}
            maxLength={2000}
            onChange={(event) => onDraft(event.target.value)}
            placeholder="Ask about today"
          />
          <button className={styles.send} type="submit" disabled={sending || draft.trim().length === 0}>
            {sending ? 'Thinking…' : 'Send'}
          </button>
        </div>
        <button type="button" className={styles.starter} onClick={onClear} disabled={sending}>
          Clear conversation
        </button>
      </form>
    </section>
  );
}
