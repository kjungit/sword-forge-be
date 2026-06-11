# QA Checklist

## Core

- `GET /api/v1/health` returns `up`.
- `GET /api/v1/weapons` returns 27 swords.
- `GET /api/v1/weapons/{weaponId}` returns a valid sword.

## Save

- `GET /api/v1/saves/{userId}` creates a default save on first access.
- `PUT /api/v1/saves/{userId}` persists save changes.

## Enhance

- Enhancement success advances the weapon id.
- Enhancement failure adds failure rewards.
- Protected failure does not destroy the weapon.
- Enhancement attempts are logged.
- Reward grants are logged.

## Shop

- Unlocked weapons can be purchased.
- Locked weapons are rejected.
- Purchase cost is deducted from materials.

## Evolution

- Evolution preview returns the correct target grade weapon.
- Evolution consumes required weapons and materials.
- Evolution updates the equipped weapon.

## Items

- Special item catalog returns all registered items.
- Item grant updates save data.
- Item consume decreases the stored amount.
- Enhancement boost items are consumed on use.

## Logs

- Recent enhancement logs can be queried by user.
- Recent reward logs can be queried by user.
