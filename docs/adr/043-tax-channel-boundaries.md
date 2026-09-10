# ADR-043 — Tax channel boundaries

## Status

**Proposed** — awaiting tax display and Stripe Tax launch posture decisions; legal counsel remains a live-launch dependency.

## Context

Tax is channel-specific. Double-taxing Apple/Google renewals through Stripe Tax would be incorrect. Domain booleans like `athlete.salesTax` are the wrong model.

## Decision (proposed)

- **Stripe/Web:** evaluate Stripe Tax + Product Tax Codes + registrations; treat `automatic_tax.enabled` as calculation config, **not** proof of registration in every jurisdiction.  
- **Apple:** App Store tax category; Apple owns storefront tax handling per its rules — do not re-run through Stripe Tax.  
- **Google:** Play product/tax classification; do not double-tax via Stripe.  
- Advertise tax-inclusive vs exclusive pricing per PO decision.  
- No Athlete/Organization tax boolean fields for “whether sales tax applies.”

This ADR does not give legal advice; Devino Labs must confirm registrations with appropriate counsel before live collection.

## Consequences

- Channel-specific checkout/portal configuration  
- Minimizes duplicated financial data in UAP  

## References

`docs/V4_IMPLEMENTATION_PLAN.md` §10
