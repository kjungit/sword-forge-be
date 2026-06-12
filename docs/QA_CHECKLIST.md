# QA Checklist

## Core

- `GET /api/v1/health` returns `up`.
- `GET /api/v1/weapons` returns 27 swords.
- `GET /api/v1/weapons/{weaponId}` returns a valid sword.
- `POST /api/v1/weapons/equip` changes the current weapon to an owned stored sword.
- Equipping a sword that is not in `weaponInventory` is rejected.
- `POST /api/v1/weapons/lock` adds a sword id to `lockedWeaponIds`.
- `POST /api/v1/weapons/unlock` removes a sword id from `lockedWeaponIds`.
- Selling a locked sword is rejected.
- `/swagger-ui.html` loads the Swagger UI.
- `/v3/api-docs` returns OpenAPI JSON.

## Save

- `GET /api/v1/saves/{userId}` creates a default save on first access.
- `PUT /api/v1/saves/{userId}` persists save changes.
- Save responses include `pityStacks`.
- Default saves include starter `materials.gold`.
- Concurrent save mutations should return conflict instead of silently overwriting state.
- Concurrent enhancement attempts for the same current sword apply at most once.
- Concurrent sale attempts for the same stored sword copy apply at most once.

## Enhance

- Enhancement success advances the weapon id.
- Enhancement success keeps the previous sword in storage.
- Enhancement attempt consumes `goldCost`.
- Grade-cap enhancement is rejected and directs the flow to evolution.
- Enhancement failure adds failure rewards.
- Enhancement failure does not add sale gold.
- Protected failure does not destroy the weapon.
- Failure increments the grade pity stack.
- Same-grade success keeps the grade pity stack.
- Evolution success resets the source grade pity stack.
- Destroy failure preserves a discovered non-normal grade floor.
- Enhancement attempts are logged.
- Enhancement logs include `detailsJson`.
- Reward grants are logged.

## Shop

- Unlocked weapons can be purchased.
- Locked weapons are rejected.
- Purchase cost is deducted from materials.
- `GET /api/v1/shop/sell-preview/{weaponId}` returns sale gold.
- `POST /api/v1/shop/sell` adds gold and removes the sold stored sword copies.
- Selling every owned sword is rejected.
- Selling the equipped sword equips the best remaining owned sword if no copy remains.

## Evolution

- Evolution preview returns the correct target grade weapon.
- Evolution consumes required weapons and materials.
- Evolution updates the equipped weapon.

## Items

- Special item catalog returns all registered items.
- Item grant updates save data.
- Item consume decreases the stored amount.
- Item purchase consumes `materials.gold`.
- Enhancement boost items are consumed on use.

## Logs

- Enhancement logs can be queried by user with `page`, `size`, `outcome`, and `protectionUsed`.
- Reward logs can be queried by user with `page`, `size`, `rewardKind`, and `sourceType`.
- Economy logs can be queried by user with `page`, `size`, `transactionType`, `resourceKind`, and `resourceId`.
- Weapon sale gold uses `sourceType=weapon_sale`.
- Item purchases use `sourceType=item_purchase`.
- Weapon sale writes a positive economy ledger amount for `gold`.
- Item purchase writes a negative economy ledger amount for `gold`.

## Probabilities

- `GET /api/v1/probabilities/enhance/{weaponId}` returns base and adjusted success rates.
- Probability disclosure includes pity, protection, and rate boost bonuses.
- Grade-cap weapons report `requiresEvolution=true`.

## Security

- Runtime APIs require HTTP Basic authentication when `app.security.enabled=true`.
- Health, Swagger UI, and OpenAPI JSON remain public.
- Authenticated players can access their own `userId`.
- Authenticated players are forbidden from accessing another player's `userId`.
- Regular players are forbidden from `PUT /api/v1/saves/{userId}`.
- Regular players are forbidden from `POST /api/v1/items/grant`.
- Admin users can call direct save upsert and item grant endpoints.

## Data and Balance

- `scripts/validate_data_schemas.py` passes.
- `scripts/simulate_balance.py --runs 100 --max-attempts 1000` completes and writes source/sink output.
- Balance review checks reached rate, mean attempts, and p90 attempts.
- Validation covers weapon sale prices, enhancement costs, and item purchase prices.
