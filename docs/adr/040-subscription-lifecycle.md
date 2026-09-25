# ADR-040 — Subscription lifecycle

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #7–9).

## Context

Stripe/Apple/Google expose many native statuses. Product code should not mirror every provider enum.

## Decision

Canonical internal states (minimal set):

| State | Meaning |
| --- | --- |
| `PENDING` | Checkout/purchase started; not entitled |
| `TRIALING` | Org 14-day trial; entitled |
| `ACTIVE` | Paid entitled |
| `PAST_DUE` | Exceptional non-entitled payment attention. Not the ordinary failed-payment stop |
| `GRACE_PERIOD` | Explicit 7-calendar-day failed-payment grace; entitled; billing-owner messaging |
| `CANCEL_AT_PERIOD_END` | Cancel scheduled; entitled until paid-through |
| `EXPIRED` | Period ended / grace ended without recovery |
| `CANCELLED` | Optional terminal label after cancel-at-period-end completes (use only if ops need distinction from `EXPIRED`) |

Primary transitions:

```text
PENDING → TRIALING → ACTIVE
PENDING → ACTIVE
ACTIVE | TRIALING → CANCEL_AT_PERIOD_END → EXPIRED
ACTIVE → GRACE_PERIOD → ACTIVE | EXPIRED
TRIALING → GRACE_PERIOD → ACTIVE | EXPIRED
```

Entitlement derives from internal state + paid-through timestamps. Adapters map provider statuses (ADR-044).

Organization `planKey` and `billingCadence` are commercial attributes of the **same** Subscription aggregate (one Stripe subscription id). Slice B persistence currently treats them as immutable. Slice E management, when separately authorized, mutates them only from an authoritative allow-listed provider Price (`docs/V4_IMPLEMENTATION_PLAN.md` **§40**). That is domain/JPA mutability. It does not require a new subscription row or a Flyway migration by itself.

Organization cancel and reactivate use that same aggregate. There is no Portal lifecycle. `CANCEL_AT_PERIOD_END` is stored only when an authoritative provider snapshot says `cancel_at_period_end` for an `ACTIVE` or `TRIALING` relationship. `PAST_DUE` and `GRACE_PERIOD` do not enter that entitled state through cancel.

Ordinary recurring payment failure, and a failed Organization trial-conversion charge, enter `GRACE_PERIOD` in the same handling that records the failure. They do not settle in non-entitled `PAST_DUE` first. `graceEndsAt` is the earliest known qualifying provider failure instant in UTC plus 7 days. Entitlement is `asOf < graceEndsAt`. Later retries do not move that instant later. `PAST_DUE` remains the non-entitled exceptional attention state when a safe grace window cannot be established (`paused`, `unpaid` with no open grace, or payment attention on a relationship that is not grace-eligible). `CANCEL_AT_PERIOD_END` is not grace-eligible.

`EXPIRED` stays terminal. Under Slice F, nonpayment `EXPIRED` must be persisted only after the provider relationship is terminal and `graceEndsAt` has been reached. An earlier provider cancel does not end the 7-day entitlement. A late payment does not resurrect `EXPIRED`. Access at `graceEndsAt` ends from `isCommerciallyEntitledAt` even when the row is still `GRACE_PERIOD` because the worker has not finished provider termination. The full recovery contract is `docs/V4_IMPLEMENTATION_PLAN.md` **§44**. That section does not authorize runtime.

## Consequences

- Stable product checks across providers  
- Org trial and 7-day grace are first-class  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§15–16, 22
