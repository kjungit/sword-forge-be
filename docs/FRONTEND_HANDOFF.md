# Frontend Handoff

This file is the backend contract for the Godot/mobile frontend.

Base URL for local development:

```text
http://127.0.0.1:8080/api/v1
```

## Security

Runtime APIs use HTTP Basic authentication when `APP_SECURITY_ENABLED=true`.

Frontend env values:

```text
SWORD_FORGE_API_BASE_URL=http://127.0.0.1:8080/api/v1
SWORD_FORGE_API_USER=local_user
SWORD_FORGE_API_PASSWORD=local_password
```

Backend local env values live in `.env.example`. Keep the frontend user/password aligned with `SPRING_SECURITY_USER_NAME` and `SPRING_SECURITY_USER_PASSWORD`.

Backend local server and smoke-test instructions live in `docs/LOCAL_TESTING.md`.

Before any `POST`, `PUT`, `PATCH`, or `DELETE`, call:

```http
GET /api/v1/security/csrf
```

Response data:

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "csrf-token-value",
  "cookieName": "XSRF-TOKEN"
}
```

Mutation requests must send:

```http
Authorization: Basic <base64-user-password>
Content-Type: application/json
X-XSRF-TOKEN: csrf-token-value
Cookie: XSRF-TOKEN=csrf-token-value
```

If a mutation returns `403`, refresh the CSRF token and retry only when the user action is still current.

## CORS

Godot native/mobile does not need browser CORS, but Web export and browser-based test harnesses do.

Default local origins:

```text
http://localhost:3000
http://127.0.0.1:3000
http://localhost:5173
http://127.0.0.1:5173
http://localhost:8080
http://127.0.0.1:8080
```

Override in backend deployment:

```text
APP_CORS_ALLOWED_ORIGINS=https://game.example.com,https://preview.example.com
```

## Save State

Load or create the player save:

```http
GET /api/v1/saves/{userId}
```

Important fields:

- `currentWeaponId`
- `materials`
- `specialItems`
- `weaponInventory`
- `lockedWeaponIds`
- `unlockedWeaponShop`
- `discoveredWeaponIds`
- `highestReachedWeaponId`
- `pityStacks`

Gold is `materials.gold`.

Default save currently starts with:

```json
{
  "currentWeaponId": "normal_01",
  "materials": { "gold": 100000 },
  "weaponInventory": { "normal_01": 1 }
}
```

## Enhance

Preview:

```http
POST /api/v1/enhance/preview
```

Request:

```json
{
  "userId": "local_user",
  "weaponId": "normal_01",
  "useProtection": false,
  "rateBoostItemId": "enhance_rate_boost_5"
}
```

`rateBoostItemId` is optional.

Use these preview fields for UI state:

- `weaponId`
- `nextWeaponId`
- `successRate`
- `baseSuccessRate`
- `adjustedSuccessRate`
- `failRate`
- `goldCost`
- `requiredItems`
- `canBreak`
- `useProtectionAvailable`
- `canAfford`
- `missingResources`
- `failureResult`
- `enhancementAvailable`
- `pityKey`
- `pityStack`
- `pityBonus`

Normal-grade enhancement is gold-only:

- `requiredItems` is `[]`.
- Do not block normal enhancement because of missing materials.

Attempt:

```http
POST /api/v1/enhance/attempt
```

Use the attempt response immediately for the "one more" loop:

- `outcome`
- `goldCost`
- `remainingGold`
- `currentWeaponId`
- `equippedWeaponId`
- `remainingMaterials`
- `nextPreview`
- `canRetry`
- `missingResources`

Frontend should prefer `remainingMaterials` and `nextPreview` after an attempt, then refresh save in the background if needed. Avoid forcing a full save fetch before enabling the next retry button.

## Sale

Sale preview should include user context:

```http
GET /api/v1/shop/sell-preview/{weaponId}?userId=local_user&amount=1
```

Use these fields:

- `weaponId`
- `investedGold`
- `sellGold`
- `profitMultiplier`
- `willFallbackToStarter`
- `unitGoldPrice`
- `totalGold`

Backend rule:

```text
sellGold >= investedGold * 2
```

Sale execution:

```http
POST /api/v1/shop/sell
```

Request:

```json
{
  "userId": "local_user",
  "weaponId": "normal_02",
  "amount": 1
}
```

Response fields to apply:

- `remainingGold`
- `equippedWeaponId`
- `weaponInventory`
- `investedGold`
- `profitMultiplier`
- `willFallbackToStarter`
- `unitGoldPrice`
- `totalGold`

Rules:

- Selling the only starter `normal_01` is rejected.
- Selling the last non-starter sword restores and equips `normal_01`.
- Locked weapons cannot be sold.

## Idle Income

The home screen may animate local hits, but gold must be settled by the server.

Claim:

```http
POST /api/v1/idle/claim
```

Request:

```json
{
  "userId": "local_user"
}
```

Response:

```json
{
  "damage": 120,
  "isCritical": false,
  "goldGained": 12,
  "totalGold": 100012,
  "lastClaimedAt": "2026-06-14T00:00:00Z"
}
```

Frontend guidance:

- Do not add permanent gold locally.
- Use local hit animation only as visual feedback.
- Call claim on app foreground, periodic intervals, and before enhance/sale screens if the displayed gold may be stale.
- Update `materials.gold` from `totalGold`.

## Weapon Storage

Current short-term backend inventory shape:

```json
{
  "weaponInventory": {
    "normal_01": 1,
    "normal_02": 2
  }
}
```

This is count-based by `weaponId`. The future long-term model will use sword instances:

```json
{
  "weaponInstanceId": "uuid",
  "weaponId": "normal_02",
  "investedGold": 14,
  "locked": false,
  "createdAt": "2026-06-14T00:00:00Z"
}
```

Do not build frontend UI that assumes two copies of the same weapon can already have separate names, locks, or investment history. That needs the instance migration first.

## Frontend Test Checklist

Minimum smoke flow against a local backend:

1. `GET /health` returns `up`.
2. `GET /security/csrf` returns `headerName`, `token`, and `cookieName`.
3. `GET /saves/local_user` returns a save with `currentWeaponId`, `materials`, and `weaponInventory`.
4. `POST /enhance/preview` for `normal_01` returns `requiredItems=[]`.
5. `POST /enhance/attempt` returns retry context.
6. `GET /shop/sell-preview/normal_02?userId=local_user&amount=1` returns `sellGold` and `willFallbackToStarter`.
7. `POST /idle/claim` returns `goldGained` and `totalGold`.
8. Repeated mutation after a stale CSRF token refreshes the token before retry.

Recommended frontend unit coverage:

- API client attaches Basic Auth, CSRF header, and CSRF cookie.
- Sale adapter reads `sellGold`, not legacy `goldPrice`.
- Enhance adapter applies `remainingMaterials` and `nextPreview`.
- Idle adapter updates gold from `totalGold`.
- Last-sword sale modal uses `willFallbackToStarter`.
