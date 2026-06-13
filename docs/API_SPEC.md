# API Spec

Base path: `/api/v1`

Swagger UI: `/swagger-ui.html`

OpenAPI JSON: `/v3/api-docs`

Runtime API requests require HTTP Basic authentication unless `app.security.enabled=false` is set for a local or test profile. Health and Swagger/OpenAPI endpoints are public.

When runtime security is enabled, regular players can only access requests whose `userId` matches the authenticated username. Admin users are allowed to access other player ids.

Runtime mutation requests also require a CSRF token. Call `GET /security/csrf`, then send the returned `headerName` with the returned `token`. Native clients that do not keep cookies automatically must also send `Cookie: {cookieName}={token}` on mutation requests.

Local browser or Web export clients can use the default CORS origins configured by `APP_CORS_ALLOWED_ORIGINS`. The default local origins include `localhost` and `127.0.0.1` on ports `3000`, `5173`, and `8080`.

## Security

### `GET /security/csrf`

Returns the CSRF header name, parameter name, token, and cookie name for mutation requests.

Example response data:

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "csrf-token-value",
  "cookieName": "XSRF-TOKEN"
}
```

For `POST`, `PUT`, `PATCH`, and `DELETE`, send:

```http
X-XSRF-TOKEN: csrf-token-value
Cookie: XSRF-TOKEN=csrf-token-value
```

## Health

### `GET /health`

Returns service status.

## Weapons

### `GET /weapons`

Returns the full weapon catalog.

### `GET /weapons/{weaponId}`

Returns one weapon by id.

### `POST /weapons/equip`

Equips a stored weapon copy owned by the player.

Body:

```json
{
  "userId": "local_user",
  "weaponId": "normal_02"
}
```

The server rejects weapons that are not present in `weaponInventory`.

### `POST /weapons/lock`

Locks an owned weapon id to prevent accidental sale.

Body:

```json
{
  "userId": "local_user",
  "weaponId": "normal_02"
}
```

### `POST /weapons/unlock`

Removes the sale lock from a weapon id.

Body:

```json
{
  "userId": "local_user",
  "weaponId": "normal_02"
}
```

## Save Data

### `GET /saves/{userId}`

Loads or creates a player save.

### `PUT /saves/{userId}`

Replaces the player save payload. This endpoint is admin-only when runtime security is enabled.

Body:

```json
{
  "currentWeaponId": "normal_01",
  "materials": { "gold": 100000 },
  "specialItems": {},
  "weaponInventory": { "normal_01": 1 },
  "lockedWeaponIds": [],
  "ownedWeaponIds": ["normal_01"],
  "unlockedWeaponShop": ["normal_01"],
  "discoveredWeaponIds": ["normal_01"],
  "highestReachedWeaponId": "normal_01",
  "pityStacks": {}
}
```

`gold` is stored as `materials.gold` and acts as the soft currency for enhancement fees, weapon sale proceeds, and item purchases.

## Enhance

### `POST /enhance/preview`

Previews a single enhancement attempt.

Body:

```json
{
  "userId": "local_user",
  "weaponId": "normal_01",
  "useProtection": false
}
```

Response includes UI decision fields:
- `nextWeaponId`
- `successRate`, `baseSuccessRate`, `adjustedSuccessRate`, and `failRate`
- `goldCost`
- `requiredItems`
- `canBreak`
- `useProtectionAvailable`
- `canAfford`
- `missingResources`
- `failureResult`

Normal-grade enhancement is gold-only. For normal swords, `requiredItems` is always empty and item shortage does not block `/enhance/attempt`.

### `POST /enhance/attempt`

Runs an enhancement attempt, writes the save data, and logs the attempt.

Enhancement only advances within the same grade. A grade-cap weapon, such as `normal_10` or `rare_08`, must use the evolution API instead of enhancement.

Response includes:
- outcome
- roll value
- success threshold
- `goldCost` and `remainingGold`
- `currentWeaponId` and `equippedWeaponId`
- `remainingMaterials`
- `nextPreview`
- `canRetry`
- `missingResources`
- failure rewards
- rate boost item id, when one was consumed
- `pityKey`, `pityStackBefore`, `pityStackAfter`, and `pityBonus`

Enhancement attempts consume the configured gold fee whether the attempt succeeds, fails with protection, or fails with destruction. Destroyed weapons do not grant sale gold.

Pity stacks increase on enhancement failure, apply as a visible success-rate bonus, and are reset only when evolution succeeds for that grade.

## Shop

### `GET /shop/preview/{weaponId}`

Returns the purchase cost for a weapon.

### `GET /shop/sell-preview/{weaponId}`

Returns sale economics for the weapon.

Optional query params:
- `userId`: when present, returns user-specific fallback information.
- `amount`: optional sale amount, default `1`.

Recommended frontend call:

```http
GET /api/v1/shop/sell-preview/normal_02?userId=local_user&amount=1
```

Response includes:
- `weaponId`
- `investedGold`
- `sellGold`
- `profitMultiplier`
- `willFallbackToStarter`
- `unitGoldPrice`
- `totalGold`

### `POST /shop/purchase`

Buys an unlocked weapon and deducts materials.

Body:

```json
{
  "userId": "local_user",
  "weaponId": "normal_02"
}
```

### `POST /shop/sell`

Sells stored weapon copies and adds `materials.gold`.

Body:

```json
{
  "userId": "local_user",
  "weaponId": "normal_02",
  "amount": 1
}
```

Sale gold is at least `investedGold * 2`. If the last non-starter weapon is sold, the server restores and equips `normal_01`. Selling the only `normal_01` is rejected to prevent a starter-sale loop.

If the equipped weapon is sold and no copy remains, the server equips the best remaining owned weapon.

The server rejects sale of weapon ids listed in `lockedWeaponIds`.

## Idle Income

### `POST /idle/claim`

Claims server-authoritative idle/tick gold based on the currently equipped weapon's attack power.

Body:

```json
{
  "userId": "local_user"
}
```

Response includes:
- `damage`
- `isCritical`
- `goldGained`
- `totalGold`
- `lastClaimedAt`

## Evolution

### `GET /evolution/preview/{weaponId}`

Returns the material and weapon requirements for a grade evolution.

### `POST /evolution/attempt`

Runs evolution for the current equipped weapon of the user.

Body:

```json
{
  "userId": "local_user"
}
```

## Items

### `GET /items`

Returns the special item catalog.

### `GET /items/{itemId}`

Returns one special item by id.

### `POST /items/grant`

Adds special items to a player's save. This endpoint is admin-only when runtime security is enabled.

Body:

```json
{
  "userId": "local_user",
  "itemId": "enhance_rate_boost_5",
  "amount": 1
}
```

### `POST /items/consume`

Consumes special items from a player's save.

### `POST /items/purchase`

Buys special items with `materials.gold`.

Body:

```json
{
  "userId": "local_user",
  "itemId": "enhance_rate_boost_5",
  "amount": 1
}
```

## Probabilities

### `GET /probabilities/enhance/{weaponId}`

Returns the disclosed enhancement probability for a weapon.

Query params:
- `userId`: optional. When present, user-specific pity stacks are included.
- `useProtection`: optional boolean, default `false`.
- `rateBoostItemId`: optional enhancement-rate boost item id.

Response includes:
- `baseSuccessRate`
- `adjustedSuccessRate`
- `failRate`
- `goldCost`
- `enhancementAvailable`
- `requiresEvolution`

Normal-grade probability disclosure ignores protection and rate-boost item params, matching the gold-only normal enhancement rule.
- `pityStack` and `pityBonus`
- `protectionBonus`
- `rateBoostBonus`

## Logs

### `GET /logs/enhance/{userId}`

Returns paginated enhancement attempts for the user.

Query params:
- `outcome`: optional, for example `success`, `fail_destroyed`, `protected_fail`
- `protectionUsed`: optional boolean
- `page`: zero-based page number, default `0`
- `size`: page size from `1` to `100`, default `20`

Response `data` shape:

```json
{
  "content": [
    {
      "weaponId": "rare_01",
      "outcome": "fail_destroyed",
      "successThreshold": 0.82,
      "detailsJson": "{\"pityKey\":\"rare\"}"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

### `GET /logs/rewards/{userId}`

Returns paginated reward grant events for the user.

Query params:
- `rewardKind`: optional, for example `material`, `special_item`
- `sourceType`: optional, for example `enhance_attempt`, `item_grant`, `item_purchase`, `weapon_sale`
- `page`: zero-based page number, default `0`
- `size`: page size from `1` to `100`, default `20`

### `GET /logs/economy/{userId}`

Returns paginated signed economy ledger entries for the user.

Query params:
- `transactionType`: optional, for example `enhance_cost`, `weapon_sale`, `item_purchase_gold_cost`
- `resourceKind`: optional, for example `material`, `special_item`
- `resourceId`: optional, for example `gold`, `enhance_rate_boost_5`
- `page`: zero-based page number, default `0`
- `size`: page size from `1` to `100`, default `20`

Positive `amount` values are sources. Negative `amount` values are sinks. `balanceAfter` is the resource balance after the transaction.
