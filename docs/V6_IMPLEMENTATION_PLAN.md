# V6 Implementation Plan — AI Coach V1

Status: **Accepted product lock** (Athlete Readiness / UAP V6).

V6 introduces the first athlete-facing generative and predictive AI Coach. It sits on top of UAP's deterministic athlete intelligence. The State Engine, readiness calculations, recovery state, training recommendations, plans, and connected evidence remain authoritative.

This document is the V6 scope. V3–V5 lines that say "do not implement the AI coach" keep those versions from absorbing V6. They are not a conflicting design.

## Objective

The Coach can understand the athlete's current UAP state, explain recommendations, answer training and recovery questions, and provide bounded short-horizon coaching guidance.

```
Mobile / Web
     ↓
UAP Backend
     ↓
AI Coach Service
     ↓
Model Gateway
     ↓
Groq Provider
```

Context is assembled on the server from existing UAP reads:

- State Engine snapshot
- Readiness
- Recovery
- Training recommendation
- Today's plan counts
- Recent session RPE
- Athlete profile and active goals
- Connected evidence summaries and freshness

The model cannot write back into those systems.

## What AI may do

- Explain athlete state
- Explain deterministic recommendations
- Answer questions using the approved Coach context
- Summarize relevant athlete information
- Provide bounded coaching suggestions
- Generate explicitly labeled short-horizon forecasts

## What AI may not do

- Calculate or replace authoritative readiness
- Mutate readiness, state, recommendations, or plans
- Write HealthKit or Health Connect evidence
- Fabricate biometrics
- Diagnose medical conditions or prescribe treatment
- Bypass authorization or policy
- Turn conversation history into State Engine evidence

## Architecture

Groq is the initial inference provider. Application code talks to a provider-neutral `ModelGateway`. Another provider can be added later without changing Coach business logic.

Groq credentials stay in backend configuration. They are never sent to mobile or web.

The backend module is `aicoach`. It may depend only on published ports:

- `athlete :: context` for a bounded profile and goals
- `training :: coach-context` for a bounded training read model
- `integrations :: evidence` and `integrations :: connections` for summaries and freshness
- `identity :: auth` for the authenticated account principal

It must not call training or athlete application services, and it must not import provider payloads into the prompt.

`GET /api/v1/coach/summary` is read-only. It does not create snapshots, assessments, recommendations, or conversations.

## Phases

### V6.1 — Architecture and contracts

- This plan
- ADR-052 model gateway
- `aicoach` bounded context
- `ModelGateway`, provider-neutral request/response, Groq adapter
- Public Coach API DTOs
- Configuration, timeouts, and provider error abstraction

### V6.2 — Athlete Coach context

Server-owned context builder. The client does not construct authoritative AI context. The context distinguishes present deterministic facts, unavailable data, and stale data. Raw rows and raw integration payloads are not placed in the prompt. Body measurements are omitted on purpose so the model is not given numbers it can restate as a diagnosis.

### V6.3 — Generative Coach

Athlete questions such as:

- How am I looking today?
- Why is my readiness lower or higher today?
- Why did UAP recommend this?
- What should I focus on today?
- How has my recent training affected today?
- What should I keep in mind for today's session?

Answers must stay inside the supplied context. Missing data is stated as missing. Deterministic recommendations are attributed to UAP, not to the model.

### V6.4 — Predictive coaching

Short-horizon outlooks for today, tomorrow, and the next few days. They are labeled as AI forecasts, name contributing UAP signals, and state uncertainty. Stale or missing evidence blocks a confident forecast. The model does not emit a new readiness score, and forecasts are not stored as State Engine truth.

### V6.5 — Safety and governance

- Authenticated athlete only
- Athlete isolation
- Non-diagnostic boundary
- No fabricated measurements when the context lacks them
- No State Engine, recommendation, or plan mutation
- Request, history, and output bounds
- Provider timeout and unavailable handling
- Rate limit (`uap.ai.rate-limit`, same capacity/window shape as identity HTTP limits)
- Safe error mapping and logs that omit prompt text, model output, and credentials
- Model output is untrusted and cannot invoke privileged backend behavior

### V6.6 — Conversations

Athlete-owned conversations and messages (Flyway V40). History is bounded. The server chooses the UAP context on every request. The athlete can start a new conversation and delete one conversation or clear their own history. Messages are not readiness evidence, recovery signals, training completion, or State Engine inputs.

### V6.7 — Mobile and web Coach UI

Dedicated Coach surface and an entry point on Home (mobile) and the athlete sidebar (web). First use of the reserved AI purple tokens, only on that surface. The UI shows deterministic facts separately from AI replies and from forecasts, plus loading, error, provider-unavailable, stale, and no-data states.

### V6.8 — Hardening and certification

Unit tests for the gateway, Groq adapter (mocked HTTP), context, authorization, isolation, missing and stale data, malformed provider responses, timeout, outage, unsafe output, conversation ownership, forecast labeling, and State Engine isolation. Web and mobile tests cover the major Coach states. Normal CI does not call Groq.

## API

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/v1/coach/summary` | Deterministic context summary. No model call. No writes. |
| POST | `/api/v1/coach/conversations` | Start a conversation for the authenticated athlete. |
| GET | `/api/v1/coach/conversations/current` | Latest own conversation and bounded messages. |
| DELETE | `/api/v1/coach/conversations/{id}` | Delete one owned conversation. |
| DELETE | `/api/v1/coach/conversations` | Clear the caller's conversations. |
| POST | `/api/v1/coach/conversations/{id}/messages` | Grounded reply. Requires AI to be enabled. |

Unauthenticated calls are rejected. A conversation id owned by another athlete is a not-found response.

## Configuration

| Variable | Role | Default |
| --- | --- | --- |
| `UAP_AI_ENABLED` | Feature gate | `false` |
| `UAP_AI_PROVIDER` | Gateway provider id | `groq` |
| `UAP_AI_GROQ_API_KEY` | Backend-only secret | empty; required only when enabled |
| `UAP_AI_GROQ_MODEL` | Model id | `llama-3.3-70b-versatile` |
| `UAP_AI_GROQ_BASE_URL` | HTTPS endpoint | `https://api.groq.com/openai/v1` |
| `UAP_AI_REQUEST_TIMEOUT` | Provider timeout | `20s` (allowed range 1s–60s) |
| `UAP_AI_RATE_LIMIT_CAPACITY` / `WINDOW` | Per-account message limit | 20 per 1 minute |

Startup fails when AI is enabled without a key, with a non-Groq provider, or with a non-HTTPS base URL. Business logic reads the model id from configuration.

## Acceptance

- Coach answers are grounded in server-built context.
- Unavailable and stale facts are visible to the model and the UI.
- Forecasts are labeled and are not readiness scores.
- Unsafe or unparseable model output is replaced with a safe refusal or a facts-only fallback.
- Provider timeout and outage return a graceful unavailable state and do not mutate deterministic data.
- Athletes cannot read or delete each other's conversations.
- Disabling AI still serves the deterministic summary.

## Certification

- `./gradlew test` and `./gradlew check` from `backend/uap-server`
- `pnpm web:typecheck`, `pnpm web:lint`, `pnpm web:test`, `pnpm web:build`
- `pnpm mobile:typecheck`, `pnpm mobile:lint`, `pnpm mobile:test`, `pnpm mobile:export`
- Live Groq acceptance is separate. If no API key is configured, mocked-provider tests are the CI proof and live acceptance stays deferred.

## Explicit non-goals

- Autonomous agents
- AI plan mutation
- AI readiness calculation
- Automatic State Engine writes
- Medical diagnosis or treatment
- Voice coach
- Custom or fine-tuned model training
- Sponsor functionality
- Coach or team AI workflows
- Long-term memory beyond the bounded conversation
- V7 or later features
- Changes to Apple Health or Health Connect

## V5 carry-forward (not a V6 failure)

Real HealthKit sample → signed HealthKit-capable iOS build → backend accepted evidence → non-null `lastSuccessfulSyncAt`.

Blocked by Apple signing and provisioning. V6 must not weaken, remove, or fake that path.
