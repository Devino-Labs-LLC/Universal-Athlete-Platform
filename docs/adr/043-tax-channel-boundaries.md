# ADR-043 — Tax channel boundaries

## Status

**Accepted** — V4 Product Owner decision lock (§22.1 #13–14).

## Context

Tax is channel-specific. Double-taxing Apple/Google renewals through Stripe Tax would be incorrect. Domain booleans like `athlete.salesTax` are the wrong model.

## Decision

- **Stripe/Web:** Stripe Tax is the intended tax-calculation integration. Advertised pricing is **tax-exclusive** where applicable (`price + applicable taxes`). Enable for **live** charging only after Devino Labs’ relevant tax registrations and product tax classification are reviewed/configured. `automatic_tax.enabled` is **not** registration.  
- **Apple / Google:** Use storefront-required localized/tax presentation; **do not** run those renewals through Stripe Tax or force a single display convention.  
- No Athlete/Organization tax boolean fields for “whether sales tax applies.”  
- This ADR does not give legal advice; counsel remains a live-launch dependency.

## Consequences

- Channel-specific checkout/portal configuration  
- Minimizes duplicated financial data in UAP  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §§10, 22
