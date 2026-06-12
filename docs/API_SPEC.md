# API Spec

Base path: `/api/v1`

Swagger UI: `/swagger-ui.html`

OpenAPI JSON: `/v3/api-docs`

Runtime API requests require HTTP Basic authentication unless `app.security.enabled=false` is set for a local or test profile. Health and Swagger/OpenAPI endpoints are public.

When runtime security is enabled, regular players can only access requests whose `userId` matches the authenticated username. Admin users are allowed to access other player ids.

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

Replaces the player save payload.

Body:

```json
{
  "currentWeaponId": "normal_01",
  "materials": { "gold": 20 },
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

### `POST /enhance/attempt`

Runs an enhancement attempt, writes the save data, and logs the attempt.

Enhancement only advances within the same grade. A grade-cap weapon, such as `normal_10` or `rare_08`, must use the evolution API instead of enhancement.

Response includes:
- outcome
- roll value
- success threshold
- `goldCost` and `remainingGold`
- failure rewards
- rate boost item id, when one was consumed
- `pityKey`, `pityStackBefore`, `pityStackAfter`, and `pityBonus`

Enhancement attempts consume the configured gold fee whether the attempt succeeds, fails with protection, or fails with destruction. Destroyed weapons do not grant sale gold.

Pity stacks increase on enhancement failure, apply as a visible success-rate bonus, and are reset only when evolution succeeds for that grade.

## Shop

### `GET /shop/preview/{weaponId}`

Returns the purchase cost for a weapon.

### `GET /shop/sell-preview/{weaponId}`

Returns the gold price paid when selling one copy of the weapon.

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

The server rejects selling every owned weapon. If the equipped weapon is sold and no copy remains, the server equips the best remaining owned weapon.

The server rejects sale of weapon ids listed in `lockedWeaponIds`.

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

Adds special items to a player's save.

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
