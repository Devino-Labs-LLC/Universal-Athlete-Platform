# V7 Implementation Plan — Sponsor & Partner Ecosystem V1

Status: **Accepted product lock** (Athlete Readiness / UAP V7).

V3–V6 documents named V7 as “marketplace / social / brand ecosystem” and forbade implementing it then. This lock replaces that placeholder. V7 is **not** an open advertising marketplace, social graph, or auction.

Starting baseline: `develop` at `4d31c7626354d206784293a33ba968d686f68936` (V6 AI Coach).

## Objective

Approved organizations and brands can support athletes and teams through UAP-controlled sponsorship relationships and placements. Sponsorship is a business and presentation layer. It must not become an input into readiness, recovery, the State Engine, training recommendations, AI Coach context, or connected evidence.

Sponsor content is visibly labeled and visually distinct from UAP athlete intelligence and from the AI Coach purple language.

```
Sponsor / Partner
      ↓
Sponsorship Campaign
      ↓
Placement / Audience Scope
      ↓
Athlete / Team-facing presentation
```

Administratively:

```
UAP operator (config allowlist)
   ↓
Sponsors → Campaigns → Assets (HTTPS URLs) → Placements → Eligibility → Status → Reporting
```

## Architecture

```
Mobile / Web
     ↓
UAP Backend
     ↓
sponsorship bounded context
     ↓
organization :: membership (eligibility only)
```

The module does **not** depend on training, athlete context, integrations evidence, or aicoach.

There is no platform-admin Spring role today (`ROLE_USER` only; org roles are tenant-scoped). V7 operators are account IDs in `uap.sponsorship.operator-account-ids`. Non-operators receive **404** on admin APIs (ADR-032).

## Domain

### Sponsor

`DRAFT` → `ACTIVE` → `PAUSED` → `ARCHIVED`

Fields: id, name, display name, status, logo URL, website URL, optional description, created/updated.

### Campaign

Belongs to a sponsor.

Stored operator status: `DRAFT`, `SCHEDULED`, `ACTIVE`, `PAUSED`, `ENDED`, `ARCHIVED`.

Server `Clock` decides whether a stored `ACTIVE`/`SCHEDULED` campaign is presentable: `startsAt <= now` and (`endsAt` null or `now < endsAt`). Clients never decide activation.

Copy: headline, body, CTA label. Destination and optional creative are HTTPS URLs. No HTML, scripts, or tracking pixels.

### Placement

Surfaces that already exist:

| Surface | Where |
| --- | --- |
| `HOME` | Athlete home / today dashboard |
| `TRAINING` | Athlete training landing |
| `TEAM` | Existing team coach surfaces |

There is no Event/challenge product surface. Do not invent one for inventory.

Audience: `ALL` (any authenticated account), `ORGANIZATION` (active org membership), `TEAM` (active team membership). No health, readiness, sleep, body, injury, or Coach-conversation targeting.

One eligible card per surface. If none, render nothing.

## APIs

Athlete / coach (authenticated):

| Method | Path |
| --- | --- |
| GET | `/api/v1/sponsorships/placements?surface=HOME\|TRAINING\|TEAM` |
| POST | `/api/v1/sponsorships/placements/{placementId}/impressions` |
| POST | `/api/v1/sponsorships/placements/{placementId}/clicks` |

Operator (`/api/v1/platform/sponsorships/**`):

- `GET /operators/me`
- Sponsor list/create/get/update and status transitions
- Campaign list/create/get/update and status transitions
- Placement attach
- `GET /reports` aggregates only

GET placements does not write State Engine, evidence, recommendations, or Coach rows. Impression/click writes only `sponsorship_events`.

## Reporting V1

First-party events: `IMPRESSION`, `CLICK`.

Dedupe: unique `(account_id, campaign_id, surface, kind, hour-bucket)`.

Aggregates: sponsor, campaign, placement/surface, time bucket, impression count, click count, CTR. No athlete identity, health, readiness, RPE, or Coach messages in reports.

## Clients

Web and mobile fetch placements independently of today/bootstrap. Cards use “Sponsored” / “Partner” labels, existing card chrome (not AI purple, not readiness styling). Missing asset: omit image. Broken link: still labeled, destination omitted if invalid.

Web operator directory under `/app/platform/sponsors` is **not** in the athlete sidebar. A Platform link appears only after `operators/me` succeeds.

## Non-goals

Auctions, RTB, sponsor self-signup, impression billing, health data for advertisers, behavioral profiles, readiness/injury/Coach targeting, third-party pixels, sponsor HTML/JS, sponsor influence on recommendations or Coach, sponsor AI agents, V8+.

## Certification

Backend: lifecycle, scheduling, eligibility, unauthorized 404, invalid URLs, expired/paused/archived, missing assets, impression/click dedupe, aggregate reporting, isolation from readiness/evidence/Coach.

Web/mobile: card, label, no-placement, loading/error, link, operator 404.

## Deferred carry-forward (not V7 failures)

1. Live Groq acceptance (V6) — backend key.
2. V5 Apple Health real-sample acceptance — Apple Development signing.
