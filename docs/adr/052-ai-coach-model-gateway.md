# ADR-052 — AI Coach model gateway

- **Status:** Accepted
- **Date:** 2026-10-08
- **Product:** Athlete Readiness V6 AI Coach

## Context

V6 adds an athlete-facing Coach that explains deterministic UAP state and can offer a labeled short-horizon outlook. Groq is the first inference provider. Readiness, recovery, recommendations, plans, and connected evidence already have owners. Those modules must stay authoritative, and a later provider must not require a rewrite of Coach rules.

Spring Modulith already limits cross-module access to named interfaces. Training application services are internal. A Coach module that imported them would fail the modularity check and could accidentally call mutating use cases.

## Decision

1. Add an `aicoach` module. It depends on published read ports only: athlete context (bounded profile), training `coach-context`, integration evidence, and integration connections.
2. Define a provider-neutral `ModelGateway` with its own request and response. Groq is an adapter behind that interface. The selected model id, timeout, and API key come from `uap.ai` configuration.
3. The server builds a bounded Coach context. Clients do not submit authoritative athlete facts. The prompt omits raw evidence values, body measurements, and resource identifiers.
4. Model output is untrusted. The server parses it, rejects diagnostic or mutating claims, and refuses a forecast when context is insufficient. Output cannot call a privileged use case.
5. Conversations persist in Coach-owned tables. They are not inputs to the State Engine.
6. `GET` summary does not create deterministic records and does not call the provider.
7. The provider key remains backend-only. AI defaults to disabled so missing credentials do not weaken the boundary or break boot.

## Consequences

- A second provider implements `ModelGateway` and does not change Coach intent, safety, or context code.
- Training publishes a read model instead of opening its application package.
- Live provider acceptance depends on a configured key and is outside normal CI.
- Purple UI tokens are approved on the Coach surface only.

## References

- [`docs/V6_IMPLEMENTATION_PLAN.md`](../V6_IMPLEMENTATION_PLAN.md)
- ADR-029 State Engine vs Planning ownership
- ADR-046 Integrations bounded context
- ADR-049 Connected sync is explicit
