# V8 Implementation Plan — Athlete Marketplace V1

Status: **Accepted product lock** (Athlete Readiness / UAP V8).

Historical V3/V7 notes named later versions as “marketplace.” V7 shipped a **sponsor/partner presentation** layer, not a store. This lock is the first curated Athlete Marketplace. It is **not** open merchant self-serve, Stripe Connect, or seller payouts.

Starting baseline: `develop` at `b0783073f35b5ad23f61b8e512cbd40804803491` (V7 Sponsor & Partner).

## Objective

Athletes and teams discover UAP-approved products, services, programs, events, and offers. Commercial content stays outside readiness, recovery, the State Engine, recommendations, connected evidence, and AI Coach.

```
Approved Partner (V7 Sponsor)
      ↓
Marketplace Listing
      ↓
Category / Audience / Availability
      ↓
Athlete Marketplace
      ↓
Detail / Save / Redeem / Visit Partner
```

A V7 sponsor does **not** automatically appear in the Marketplace. Listings may reference an existing sponsor id. Campaigns and listings remain separate.

## Architecture

```
Mobile / Web
     ↓
marketplace bounded context
     ↓
sponsorship :: catalog   (partner identity + operator allowlist)
organization :: membership
identity :: auth
```

The module does not depend on training, athlete context, integrations, or aicoach.

Operators reuse `uap.sponsorship.operator-account-ids`. Admin APIs 404 for everyone else.

Server `Clock` decides whether a listing or offer is presentable.

## Domain

### Category

Controlled rows: Training, Recovery, Equipment, Apparel, Nutrition, Services, Camps & Programs.

### Listing

Types: `PRODUCT`, `SERVICE`, `PROGRAM`, `EVENT`, `OFFER`.

Stored status: `DRAFT`, `SCHEDULED`, `ACTIVE`, `PAUSED`, `ENDED`, `ARCHIVED`.

Presentable when sponsor exists, listing is `ACTIVE` or `SCHEDULED`, and `startsAt <= now < endsAt` (open end allowed).

Audience: `ALL`, `ORGANIZATION`, `TEAM` (membership only). Never readiness, evidence, or Coach data.

Optional offer fields: price display, discount text, promo code, offer window. Promo codes are returned only on a presentable listing whose offer window contains now.

External destinations: HTTPS only. Plain text only.

### Saves

Per authenticated account. IDOR: 404 for foreign listing ids on mutating save APIs when the caller cannot see the listing. Saved history may include ended/archived rows marked unavailable.

Saving never writes State Engine, evidence, or Coach rows.

## APIs

Athlete (authenticated):

| Method | Path |
| --- | --- |
| GET | `/api/v1/marketplace/categories` |
| GET | `/api/v1/marketplace/listings` query: category, type, q, featured, page, size |
| GET | `/api/v1/marketplace/listings/{id}` |
| GET | `/api/v1/marketplace/saves` |
| PUT | `/api/v1/marketplace/listings/{id}/save` |
| DELETE | `/api/v1/marketplace/listings/{id}/save` |
| POST | `/api/v1/marketplace/listings/{id}/impressions` |
| POST | `/api/v1/marketplace/listings/{id}/detail-views` |
| POST | `/api/v1/marketplace/listings/{id}/clicks` |
| POST | `/api/v1/marketplace/listings/{id}/redemptions` |

Operator (`/api/v1/platform/marketplace/**`): listing CRUD, status, reports.

## Reporting V1

Events: `IMPRESSION`, `DETAIL`, `CLICK`, `REDEEM`, `SAVE`. Hour-bucket unique per account+listing+kind. Aggregates only. No athlete health or identity lists.

## Clients

Web: `/app/marketplace` in the athlete sidebar. Operator listings under `/app/platform/marketplace`.

Mobile: Profile stack (`/profile/marketplace`), not a sixth tab.

Cards are commercial discovery chrome, not readiness/Coach styling. Partner name shown. Empty search: empty view, not a fake box.

## Non-goals

P2P sales, merchant signup/KYC, Stripe Connect, payouts, inventory, shipping, tax, auctions, ad networks, behavioral or readiness-based product recommendations, AI purchase pressure, V9+.

## Deferred carry-forward

1. V7 operator allowlist configuration.
2. V6 live Groq key.
3. V5 Apple Health real-sample signing.
