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
| `PAST_DUE` | Payment failed; grace accounting |
| `GRACE_PERIOD` | Explicit 7-calendar-day failed-payment grace; entitled; billing-owner messaging |
| `CANCEL_AT_PERIOD_END` | Cancel scheduled; entitled until paid-through |
| `EXPIRED` | Period ended / grace ended without recovery |
| `CANCELLED` | Optional terminal label after cancel-at-period-end completes (use only if ops need distinction from `EXPIRED`) |

Primary transitions:

```text
PENDING → TRIALING → ACTIVE
PENDING → ACTIVE
ACTIVE | TRIALING → CANCEL_AT_PERIOD_END → EXPIRED
ACTIVE → PAST_DUE / GRACE_PERIOD → ACTIVE | EXPIRED
```

Entitlement derives from internal state + paid-through timestamps. Adapters map provider statuses (ADR-044).

Organization `planKey` and `billingCadence` are commercial attributes of the **same** Subscription aggregate (one Stripe subscription id). Slice B persistence currently treats them as immutable. Slice E management, when separately authorized, mutates them only from an authoritative allow-listed provider Price (`docs/V4_IMPLEMENTATION_PLAN.md` **§40**). That is domain/JPA mutability. It does not require a new subscription row or a Flyway migration by itself.

## Consequences

- Stable product checks across providers  
- Org trial and 7-day grace are first-class  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§15–16, 22
