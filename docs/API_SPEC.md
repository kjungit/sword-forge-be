# API Spec

Base path: `/api/v1`

## Health

### `GET /health`

Returns service status.

## Weapons

### `GET /weapons`

Returns the full weapon catalog.

### `GET /weapons/{weaponId}`

Returns one weapon by id.

## Save Data

### `GET /saves/{userId}`

Loads or creates a player save.

### `PUT /saves/{userId}`

Replaces the player save payload.

Body:

```json
{
  "currentWeaponId": "normal_01",
  "materials": {},
  "specialItems": {},
  "weaponInventory": { "normal_01": 1 },
  "ownedWeaponIds": ["normal_01"],
  "unlockedWeaponShop": ["normal_01"],
  "discoveredWeaponIds": ["normal_01"],
  "highestReachedWeaponId": "normal_01"
}
```

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

Response includes:
- outcome
- roll value
- success threshold
- failure rewards

## Shop

### `GET /shop/preview/{weaponId}`

Returns the purchase cost for a weapon.

### `POST /shop/purchase`

Buys an unlocked weapon and deducts materials.

Body:

```json
{
  "userId": "local_user",
  "weaponId": "normal_02"
}
```

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

## Logs

### `GET /logs/enhance/{userId}`

Returns the latest enhancement attempts for the user.

### `GET /logs/rewards/{userId}`

Returns the latest reward grant events for the user.
