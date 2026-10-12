import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { brand } from '@/src/theme/tokens';
import {
  COACH_STARTERS,
  type CoachConversation,
  type CoachSummary,
} from '@/src/features/coach/api/coachApi';
import { coachFailureMessage, useCoach } from '@/src/features/coach/hooks/useCoach';

export function CoachScreen() {
  const coach = useCoach();

  if (coach.summary.isLoading || coach.conversation.isLoading) {
    return <LoadingView message="Loading Coach…" />;
  }
  if (coach.summary.isError || !coach.summary.data) {
    return (
      <ErrorView
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
      notice={coach.send.isError ? coachFailureMessage(coach.send.error) : null}
      onDraft={coach.setDraft}
      onSend={(text) => coach.send.mutate(text)}
      onClear={() => coach.clear.mutate()}
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
  const theme = useAppTheme();
  const stale = summary.facts.some((fact) => fact.stale);
  const noSignals = summary.facts
    .filter((fact) => fact.topic !== 'Profile')
    .every((fact) => fact.availability !== 'PRESENT');

  return (
    <Screen scroll includeBottomInset testID="coach-screen">
      <Text style={[styles.intro, { color: theme.colors.textMuted }]}>
        AI Coach explains deterministic UAP state. It does not calculate your readiness score.
      </Text>
      <View style={[styles.card, { borderColor: theme.colors.border }]}>
        <Text style={[styles.heading, { color: theme.colors.text }]}>UAP facts for {summary.asOf}</Text>
        {stale ? (
          <Text style={{ color: theme.colors.textMuted }} accessibilityRole="text">
            Some UAP data is stale. Coach should treat it as stale.
          </Text>
        ) : null}
        {noSignals ? (
          <Text testID="coach-no-data" style={{ color: theme.colors.textMuted }}>
            There is no readiness, recovery, recommendation, or connected evidence for this date.
          </Text>
        ) : null}
        {summary.facts.map((fact) => (
          <Text key={`${fact.topic}-${fact.text}`} style={{ color: theme.colors.text }}>
            {fact.topic}
            {' · '}
            {fact.availability === 'PRESENT' ? 'Recorded by UAP' : 'Not available'}
            {fact.stale ? ' · Stale' : ''}
            {'\n'}
            {fact.text}
          </Text>
        ))}
      </View>
      <View style={[styles.card, { borderColor: brand.ai500 }]} testID="coach-thread">
        <Text style={[styles.heading, { color: brand.ai500 }]}>Conversation</Text>
        {conversation.messages.length === 0 ? (
          <Text style={{ color: theme.colors.textMuted }}>No Coach messages yet.</Text>
        ) : (
          conversation.messages.map((message) => (
            <View key={message.id} testID={message.forecast ? 'coach-forecast' : `coach-${message.role}`}>
              <Text style={{ color: brand.ai500, fontWeight: '700' }}>
                {message.role === 'USER'
                  ? 'You'
                  : message.forecast
                    ? 'AI forecast — not a readiness score'
                    : 'AI Coach'}
              </Text>
              <Text style={{ color: theme.colors.text }}>{message.text}</Text>
              {message.uncertainty ? (
                <Text style={{ color: theme.colors.textMuted }}>Uncertainty: {message.uncertainty}</Text>
              ) : null}
            </View>
          ))
        )}
      </View>
      {notice ? (
        <Text testID="coach-notice" accessibilityRole="alert" style={{ color: theme.colors.danger }}>
          {notice}
        </Text>
      ) : null}
      {!summary.aiEnabled ? (
        <Text testID="coach-disabled" style={{ color: theme.colors.textMuted }}>
          AI Coach is turned off. UAP facts above are unchanged.
        </Text>
      ) : null}
      <View style={styles.starters}>
        {COACH_STARTERS.map((prompt) => (
          <Pressable
            key={prompt}
            accessibilityRole="button"
            disabled={sending}
            onPress={() => onSend(prompt)}
            style={[styles.starter, { borderColor: brand.ai500 }]}>
            <Text style={{ color: brand.ai500 }}>{prompt}</Text>
          </Pressable>
        ))}
      </View>
      <TextInput
        accessibilityLabel="Message AI Coach"
        value={draft}
        maxLength={2000}
        onChangeText={onDraft}
        placeholder="Ask about today"
        placeholderTextColor={theme.colors.textMuted}
        style={[styles.input, { color: theme.colors.text, borderColor: theme.colors.border }]}
      />
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={sending ? 'Thinking' : 'Send'}
        disabled={sending || draft.trim().length === 0}
        onPress={() => onSend(draft.trim())}
        style={[styles.send, { backgroundColor: brand.ai500 }]}>
        <Text style={styles.sendLabel}>{sending ? 'Thinking…' : 'Send'}</Text>
      </Pressable>
      <Pressable accessibilityRole="button" accessibilityLabel="Clear conversation" onPress={onClear}>
        <Text style={{ color: brand.ai500 }}>Clear conversation</Text>
      </Pressable>
    </Screen>
  );
}

const styles = StyleSheet.create({
  intro: { fontSize: 15, marginBottom: 12 },
  card: { borderWidth: 1, borderRadius: 12, padding: 12, gap: 8, marginBottom: 12 },
  heading: { fontSize: 17, fontWeight: '700' },
  starters: { gap: 8, marginBottom: 12 },
  starter: { borderWidth: 1, borderRadius: 999, paddingVertical: 8, paddingHorizontal: 12 },
  input: { borderWidth: 1, borderRadius: 10, minHeight: 44, paddingHorizontal: 12, marginBottom: 12 },
  send: { borderRadius: 10, minHeight: 44, alignItems: 'center', justifyContent: 'center', marginBottom: 12 },
  sendLabel: { color: '#fff', fontWeight: '700' },
});
