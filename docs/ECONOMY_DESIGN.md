# Economy Design

## Materials

- `cracked_iron_piece`
- `cold_ember_powder`
- `blue_black_jade_fragment`
- `grudge_iron_dust`
- `cracked_soul_stone`
- `abyss_trace`
- `sealed_stardust`
- `cataclysm_remnant`
- `evolution_fixed_stone`
- `black_jade_heart`
- `gold`

`gold` is stored in the same save map as materials as `materials.gold`, but it is treated as soft currency in game design.

## Sources

- Failure rewards generate the main material flow.
- Higher grades drop higher-tier materials.
- Weapon sale generates `gold`.
- Idle claims generate `gold` from current weapon damage.

## Sinks

- Enhancement attempts consume a small configured `gold` fee.
- Weapon repurchase.
- Special item purchases consume `gold`.
- Evolution materials for grade upgrades.

## Current server behavior

- Enhancement failure grants material rewards.
- Enhancement failure does not grant `gold` unless the player separately sells stored weapons.
- Enhancement success stores the next sword while keeping the previous sword copy.
- Weapon sale removes stored sword copies and adds `gold`.
- Sale gold is at least `investedGold * 2`, where `investedGold` is the cumulative enhancement gold needed to reach the sold weapon.
- The configured sale table is treated as a floor only when it is higher than the loop-protection value.
- Selling the last non-starter weapon restores `normal_01`; selling the only `normal_01` is rejected.
- Normal-grade enhancement is gold-only; protection and boost items are not required or consumed in normal enhancement.
- `/idle/claim` grants gold from current weapon damage and records the source in the economy ledger.
- Weapon purchase deducts materials.
- Special item purchase deducts `gold`.
- Protection tickets and boost items are consumed when used.
- Evolution consumes configured weapons and materials.
- Enhancement can only progress within the current grade; grade upgrades require evolution.
- Pity stacks increase after failures and add a capped success-rate bonus for that grade.
- Pity stacks are visible in preview/attempt responses and reset on evolution success.
- Destroy failures keep a discovered non-normal grade floor, so a rare or higher player does not fall back to normal progression.
- Save data stores current inventory state.
- Save data uses optimistic locking to reject concurrent overwrite races.
- Economy ledger entries store signed resource deltas for sources and sinks.
- Positive ledger amounts are gains; negative ledger amounts are costs or consumption.

## Balance workflow

- `scripts/validate_data_schemas.py` validates JSON schemas and cross-file references.
- Validation also checks that evolution-required materials can drop before the grade cap that consumes them.
- Validation checks that every weapon has purchase, sale, and enhancement-cost data.
- Validation checks that every special item has purchase-price data.
- `scripts/simulate_balance.py` runs progression simulations from each grade milestone to the next and the early-loop target `normal_01 -> epic_02`.
- Simulator output includes completion rate, attempt percentiles, destruction count, repurchases, evolutions, cap resets, material/gold sources, and material/gold sinks.
- Balance tuning should watch both mean attempts and p90 attempts, because probability systems can feel broken when only the long-tail cases are bad.

## Current Balance Checkpoint

Checked with `python3 scripts/simulate_balance.py --runs 300 --max-attempts 1000`.

- Overall reached rate: 99.40%
- Overall mean attempts: 26.75
- Overall p90 attempts: 52
- Overall p95 attempts: 62
- `normal_01 -> epic_02` reached rate: 100.00%
- `normal_01 -> epic_02` p95 attempts: 86
- Legendary segment reached rate: 97.00%
- Legendary segment p90 attempts: 17

Early and mid progression now use sale values that preserve the retry loop instead of punishing failed enhancement chains with gold starvation.
