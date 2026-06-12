#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import random
import statistics
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Tuple


ROOT = Path(__file__).resolve().parents[1]
WEAPONS_PATH = ROOT / "src/main/resources/data/weapons.json"
ENHANCE_TABLE_PATH = ROOT / "src/main/resources/data/enhance_table.json"
FAILURE_REWARDS_PATH = ROOT / "src/main/resources/data/failure_rewards.json"
EVOLUTION_REQUIREMENTS_PATH = ROOT / "src/main/resources/data/evolution_requirements.json"
PURCHASE_COSTS_PATH = ROOT / "src/main/resources/data/weapon_purchase_costs.json"
WEAPON_SALE_PRICES_PATH = ROOT / "src/main/resources/data/weapon_sale_prices.json"
ENHANCE_COSTS_PATH = ROOT / "src/main/resources/data/enhance_costs.json"
OUTPUT_CSV = ROOT / "outputs/balance_result.csv"
OUTPUT_MD = ROOT / "outputs/balance_summary.md"


@dataclass(frozen=True)
class Weapon:
    id: str
    grade: str
    stage: int
    success_rate: float
    fail_rate: float
    failure_reward_group: str
    next_weapon_id: str | None


def load_weapons() -> List[Weapon]:
    raw = json.loads(WEAPONS_PATH.read_text(encoding="utf-8"))
    return [
        Weapon(
            id=item["id"],
            grade=item["grade"],
            stage=item["stage"],
            success_rate=item["successRate"],
            fail_rate=item["failRate"],
            failure_reward_group=item["failureRewardGroup"],
            next_weapon_id=item.get("nextWeaponId"),
        )
        for item in raw
    ]


def load_enhance_table() -> Dict[str, Tuple[float, float]]:
    raw = json.loads(ENHANCE_TABLE_PATH.read_text(encoding="utf-8"))
    return {
        item["weaponId"]: (item["successRate"], item["failRate"])
        for item in raw
    }


def load_reward_table() -> Dict[str, List[Tuple[str, int, int]]]:
    raw = json.loads(FAILURE_REWARDS_PATH.read_text(encoding="utf-8"))
    reward_table: Dict[str, List[Tuple[str, int, int]]] = {}
    for item in raw:
        reward_table[item["groupId"]] = [
            (reward["materialId"], reward["minAmount"], reward["maxAmount"])
            for reward in item["rewards"]
        ]
    return reward_table


def load_evolution_requirements() -> Dict[str, dict]:
    raw = json.loads(EVOLUTION_REQUIREMENTS_PATH.read_text(encoding="utf-8"))
    return {item["fromWeaponId"]: item for item in raw}


def load_weapon_gold_values(path: Path, key: str) -> Dict[str, int]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    return {item["weaponId"]: item[key] for item in raw}


def reward_rolls(group_id: str, reward_table: Dict[str, List[Tuple[str, int, int]]]) -> Dict[str, int]:
    rewards: Dict[str, int] = {}
    for material_id, min_amount, max_amount in reward_table[group_id]:
        amount = random.randint(min_amount, max_amount)
        if amount > 0:
            rewards[material_id] = amount
    return rewards


def merge_materials(materials: Counter, rewards: Dict[str, int]) -> Counter:
    merged = Counter(materials)
    merged.update(rewards)
    return merged


def can_afford(materials: Counter, cost: Dict[str, int]) -> bool:
    return all(materials.get(key, 0) >= value for key, value in cost.items())


def spend(materials: Counter, cost: Dict[str, int]) -> Counter:
    updated = Counter(materials)
    for key, value in cost.items():
        updated[key] -= value
        if updated[key] <= 0:
            del updated[key]
    return updated


def find_weapon_by_grade(weapons_by_id: Dict[str, Weapon], grade: str) -> Weapon:
    candidates = [weapon for weapon in weapons_by_id.values() if weapon.grade == grade]
    return min(candidates, key=lambda weapon: weapon.stage)


def can_evolve(
    current_weapon_id: str,
    inventory: Counter,
    materials: Counter,
    evolution_requirements: Dict[str, dict],
    weapons_by_id: Dict[str, Weapon],
) -> bool:
    requirement = evolution_requirements.get(current_weapon_id)
    if requirement is None:
        return False
    return can_pay_evolution_requirement(current_weapon_id, inventory, materials, requirement, weapons_by_id)


def can_pay_evolution_requirement(
    current_weapon_id: str,
    inventory: Counter,
    materials: Counter,
    requirement: dict,
    weapons_by_id: Dict[str, Weapon],
) -> bool:
    if not all(materials.get(key, 0) >= value for key, value in requirement["requiredMaterials"].items()):
        return False

    simulated_inventory = Counter(inventory)
    try:
        consume_weapon_requirement(simulated_inventory, requirement["requiredWeapons"], current_weapon_id, weapons_by_id)
    except ValueError:
        return False
    return True


def consume_weapon_requirement(
    inventory: Counter,
    required_weapons: Dict[str, int],
    current_weapon_id: str,
    weapons_by_id: Dict[str, Weapon],
) -> None:
    for requirement_id, amount in required_weapons.items():
        if requirement_id.endswith("_or_higher"):
            base_id = requirement_id[: -len("_or_higher")]
            base_weapon = weapons_by_id[base_id]
            candidates = sorted(
                (
                    weapon
                    for weapon in weapons_by_id.values()
                    if weapon.grade == base_weapon.grade and weapon.stage >= base_weapon.stage
                ),
                key=lambda weapon: weapon.stage,
                reverse=True,
            )
            remaining = amount
            for candidate in candidates:
                owned = inventory.get(candidate.id, 0)
                if owned <= 0:
                    continue
                used = min(owned, remaining)
                remove_exact(inventory, candidate.id, used)
                remaining -= used
                if remaining <= 0:
                    break
            if remaining > 0:
                raise ValueError(f"not enough range weapons for: {requirement_id}")
            continue

        next_amount = amount
        if requirement_id == current_weapon_id:
            next_amount -= 1
            if next_amount < 0:
                raise ValueError("invalid evolution requirement count for current weapon")
        if next_amount > 0:
            remove_exact(inventory, requirement_id, next_amount)

    if inventory.get(current_weapon_id, 0) > 0:
        remove_exact(inventory, current_weapon_id, 1)


def remove_exact(inventory: Counter, weapon_id: str, amount: int) -> None:
    current = inventory.get(weapon_id, 0)
    if current < amount:
        raise ValueError(f"not enough weapon copies: {weapon_id}")
    remaining = current - amount
    if remaining <= 0:
        del inventory[weapon_id]
    else:
        inventory[weapon_id] = remaining


def evolve_weapon(
    current_weapon_id: str,
    inventory: Counter,
    materials: Counter,
    evolution_requirements: Dict[str, dict],
    weapons_by_id: Dict[str, Weapon],
) -> tuple[str, Counter, Counter]:
    requirement = evolution_requirements[current_weapon_id]
    updated_materials = Counter(materials)
    for material_id, amount in requirement["requiredMaterials"].items():
        updated_materials[material_id] -= amount
        if updated_materials[material_id] <= 0:
            del updated_materials[material_id]

    updated_inventory = Counter(inventory)
    consume_weapon_requirement(updated_inventory, requirement["requiredWeapons"], current_weapon_id, weapons_by_id)
    target_weapon = find_weapon_by_grade(weapons_by_id, requirement["toGrade"])
    updated_inventory[target_weapon.id] += 1
    return target_weapon.id, updated_inventory, updated_materials


def pick_affordable_weapon(
    unlocked: List[str],
    weapons_by_id: Dict[str, Weapon],
    materials: Counter,
    purchase_costs: Dict[str, Dict[str, int]],
    require_progressable: bool = False,
) -> str:
    unlocked_weapons = sorted(
        (
            weapons_by_id[weapon_id]
            for weapon_id in unlocked
            if not require_progressable or weapons_by_id[weapon_id].next_weapon_id is not None
        ),
        key=lambda weapon: (grade_order(weapon.grade), weapon.stage),
    )
    for weapon in reversed(unlocked_weapons):
        if can_afford(materials, purchase_costs.get(weapon.id, {})):
            return weapon.id
    return unlocked_weapons[0].id if unlocked_weapons else "normal_01"


def grade_order(grade: str) -> int:
    return {"normal": 0, "rare": 1, "epic": 2, "legendary": 3}.get(grade, 99)


def choose_best_owned_weapon(inventory: Counter, weapons_by_id: Dict[str, Weapon]) -> str:
    owned = [
        weapons_by_id[weapon_id]
        for weapon_id, amount in inventory.items()
        if amount > 0 and weapon_id in weapons_by_id
    ]
    if not owned:
        return "normal_01"
    return max(owned, key=lambda weapon: (grade_order(weapon.grade), weapon.stage)).id


def choose_best_owned_progressable_weapon(inventory: Counter, weapons_by_id: Dict[str, Weapon]) -> str | None:
    owned = [
        weapons_by_id[weapon_id]
        for weapon_id, amount in inventory.items()
        if amount > 0 and weapon_id in weapons_by_id and weapons_by_id[weapon_id].next_weapon_id is not None
    ]
    if not owned:
        return None
    return max(owned, key=lambda weapon: (grade_order(weapon.grade), weapon.stage)).id


def sell_spare_weapon_for_gold(
    current_weapon_id: str,
    inventory: Counter,
    materials: Counter,
    sale_prices: Dict[str, int],
    weapons_by_id: Dict[str, Weapon],
) -> int:
    sellable = [
        weapons_by_id[weapon_id]
        for weapon_id, amount in inventory.items()
        if amount > 0 and weapon_id != current_weapon_id and weapon_id in weapons_by_id
    ]
    if not sellable:
        return 0

    weapon = min(sellable, key=lambda candidate: (grade_order(candidate.grade), candidate.stage))
    remove_exact(inventory, weapon.id, 1)
    gold = sale_prices[weapon.id]
    materials["gold"] += gold
    return gold


def ensure_grade_floor(
    inventory: Counter,
    unlocked: List[str],
    destroyed_grade: str,
    weapons_by_id: Dict[str, Weapon],
) -> Counter:
    if destroyed_grade == "normal":
        return inventory
    has_grade_weapon = any(
        amount > 0 and weapons_by_id[weapon_id].grade == destroyed_grade
        for weapon_id, amount in inventory.items()
        if weapon_id in weapons_by_id
    )
    if has_grade_weapon:
        return inventory
    floor_weapon = find_weapon_by_grade(weapons_by_id, destroyed_grade)
    if floor_weapon.id not in unlocked:
        return inventory
    inventory[floor_weapon.id] += 1
    return inventory


def simulate_once(
    start_weapon_id: str,
    target_weapon_id: str,
    weapons_by_id: Dict[str, Weapon],
    enhance_table: Dict[str, Tuple[float, float]],
    reward_table: Dict[str, List[Tuple[str, int, int]]],
    evolution_requirements: Dict[str, dict],
    purchase_costs: Dict[str, Dict[str, int]],
    sale_prices: Dict[str, int],
    enhance_costs: Dict[str, int],
    max_attempts: int,
) -> dict:
    current_weapon_id = start_weapon_id
    unlocked = [start_weapon_id]
    weapon_inventory = Counter({start_weapon_id: 1})
    initial_gold = max(20, sale_prices[start_weapon_id])
    materials = Counter({"gold": initial_gold})
    pity_stacks = Counter()
    material_sources = Counter()
    material_sources["gold"] += initial_gold
    material_sinks = Counter()
    attempts = 0
    destructions = 0
    repurchases = 0
    evolutions = 0
    cap_resets = 0

    while current_weapon_id != target_weapon_id and attempts < max_attempts:
        if can_evolve(current_weapon_id, weapon_inventory, materials, evolution_requirements, weapons_by_id):
            requirement = evolution_requirements[current_weapon_id]
            current_weapon_id, weapon_inventory, materials = evolve_weapon(
                current_weapon_id,
                weapon_inventory,
                materials,
                evolution_requirements,
                weapons_by_id,
            )
            material_sinks.update(requirement["requiredMaterials"])
            pity_stacks[weapons_by_id[requirement["fromWeaponId"]].grade] = 0
            if current_weapon_id not in unlocked:
                unlocked.append(current_weapon_id)
            evolutions += 1
            continue

        weapon = weapons_by_id[current_weapon_id]
        if weapon.next_weapon_id is None:
            owned_progressable_weapon_id = choose_best_owned_progressable_weapon(weapon_inventory, weapons_by_id)
            if owned_progressable_weapon_id is not None:
                current_weapon_id = owned_progressable_weapon_id
                cap_resets += 1
                continue

            purchase_weapon_id = pick_affordable_weapon(
                unlocked,
                weapons_by_id,
                materials,
                purchase_costs,
                require_progressable=True,
            )
            purchase_cost = purchase_costs[purchase_weapon_id]
            if can_afford(materials, purchase_cost):
                materials = spend(materials, purchase_cost)
                material_sinks.update(purchase_cost)
                weapon_inventory[purchase_weapon_id] += 1
                current_weapon_id = purchase_weapon_id
                repurchases += 1
            else:
                current_weapon_id = "normal_01"
                weapon_inventory["normal_01"] += 1
            cap_resets += 1
            continue

        success_rate, _fail_rate = enhance_table[current_weapon_id]
        gold_cost = enhance_costs[current_weapon_id]
        while materials.get("gold", 0) < gold_cost:
            sold_gold = sell_spare_weapon_for_gold(
                current_weapon_id,
                weapon_inventory,
                materials,
                sale_prices,
                weapons_by_id,
            )
            if sold_gold <= 0:
                break
            material_sources["gold"] += sold_gold
        if materials.get("gold", 0) < gold_cost:
            break

        materials = spend(materials, {"gold": gold_cost})
        material_sinks["gold"] += gold_cost
        pity_bonus = min(0.20, pity_stacks[weapon.grade] * 0.02)
        success_threshold = min(1.0, success_rate + pity_bonus)
        attempts += 1
        roll = random.random()
        if roll < success_threshold:
            next_weapon_id = weapon.next_weapon_id
            if next_weapon_id is None:
                break
            weapon_inventory[next_weapon_id] += 1
            current_weapon_id = next_weapon_id
            if next_weapon_id not in unlocked:
                unlocked.append(next_weapon_id)
            continue

        destructions += 1
        remove_exact(weapon_inventory, current_weapon_id, 1)
        rewards = reward_rolls(weapon.failure_reward_group, reward_table)
        materials = merge_materials(materials, rewards)
        material_sources.update(rewards)
        pity_stacks[weapon.grade] += 1
        weapon_inventory["normal_01"] += 1
        weapon_inventory = ensure_grade_floor(weapon_inventory, unlocked, weapon.grade, weapons_by_id)
        current_weapon_id = choose_best_owned_weapon(weapon_inventory, weapons_by_id)

        if unlocked:
            purchase_weapon_id = pick_affordable_weapon(
                unlocked,
                weapons_by_id,
                materials,
                purchase_costs,
                require_progressable=True,
            )
            purchase_cost = purchase_costs[purchase_weapon_id]
            if can_afford(materials, purchase_cost):
                materials = spend(materials, purchase_cost)
                material_sinks.update(purchase_cost)
                weapon_inventory[purchase_weapon_id] += 1
                current_weapon_id = purchase_weapon_id
                repurchases += 1

    return {
        "start_weapon_id": start_weapon_id,
        "target_weapon_id": target_weapon_id,
        "attempts": attempts,
        "destructions": destructions,
        "repurchases": repurchases,
        "evolutions": evolutions,
        "cap_resets": cap_resets,
        "reached": current_weapon_id == target_weapon_id,
        "initial_gold": initial_gold,
        "material_sources": dict(material_sources),
        "material_sinks": dict(material_sinks),
        "remaining_materials": dict(materials),
    }


def percentile(values: List[int], ratio: float) -> int:
    if not values:
        return 0
    sorted_values = sorted(values)
    index = int(ratio * (len(sorted_values) - 1))
    return sorted_values[index]


def summarize(rows: List[dict]) -> str:
    attempts = [row["attempts"] for row in rows]
    destructions = [row["destructions"] for row in rows]
    repurchases = [row["repurchases"] for row in rows]
    evolutions = [row["evolutions"] for row in rows]
    cap_resets = [row["cap_resets"] for row in rows]
    reached_count = sum(1 for row in rows if row["reached"])
    material_sources = Counter()
    material_sinks = Counter()
    for row in rows:
        material_sources.update(row["material_sources"])
        material_sinks.update(row["material_sinks"])

    if len(attempts) >= 10:
        p10 = statistics.quantiles(attempts, n=10)[0]
        p90 = statistics.quantiles(attempts, n=10)[-1]
    else:
        p10 = min(attempts)
        p90 = max(attempts)

    lines = [
        "# Balance Summary",
        "",
        f"- simulations: {len(rows)}",
        f"- reached rate: {reached_count / len(rows):.2%}",
        f"- mean attempts: {statistics.mean(attempts):.2f}",
        f"- median attempts: {statistics.median(attempts):.2f}",
        f"- mean destructions: {statistics.mean(destructions):.2f}",
        f"- mean repurchases: {statistics.mean(repurchases):.2f}",
        f"- mean evolutions: {statistics.mean(evolutions):.2f}",
        f"- mean cap resets: {statistics.mean(cap_resets):.2f}",
        f"- p10 attempts: {p10:.2f}",
        f"- p90 attempts: {p90:.2f}",
        f"- p95 attempts: {percentile(attempts, 0.95):.2f}",
        "",
        "## Material Sources",
        "",
        *[f"- {material_id}: {amount}" for material_id, amount in sorted(material_sources.items())],
        "",
        "## Material Sinks",
        "",
        *[f"- {material_id}: {amount}" for material_id, amount in sorted(material_sinks.items())],
        "",
    ]

    lines.extend(segment_summary(rows))

    if statistics.mean(attempts) > 500:
        lines.append("- warning: progression is very slow")
    if reached_count / len(rows) < 0.5:
        lines.append("- warning: many runs fail to reach the target")

    return "\n".join(lines)


def segment_summary(rows: List[dict]) -> List[str]:
    grouped: Dict[Tuple[str, str], List[dict]] = {}
    for row in rows:
        key = (row["start_weapon_id"], row["target_weapon_id"])
        grouped.setdefault(key, []).append(row)

    lines = ["## Segments", ""]
    for key, segment_rows in grouped.items():
        segment_attempts = [row["attempts"] for row in segment_rows]
        reached_count = sum(1 for row in segment_rows if row["reached"])
        destructions = [row["destructions"] for row in segment_rows]
        repurchases = [row["repurchases"] for row in segment_rows]
        cap_resets = [row["cap_resets"] for row in segment_rows]
        lines.extend([
            f"### {key[0]} -> {key[1]}",
            "",
            f"- simulations: {len(segment_rows)}",
            f"- reached rate: {reached_count / len(segment_rows):.2%}",
            f"- mean attempts: {statistics.mean(segment_attempts):.2f}",
            f"- p90 attempts: {percentile(segment_attempts, 0.90):.2f}",
            f"- p95 attempts: {percentile(segment_attempts, 0.95):.2f}",
            f"- max attempts: {max(segment_attempts)}",
            f"- mean destructions: {statistics.mean(destructions):.2f}",
            f"- mean repurchases: {statistics.mean(repurchases):.2f}",
            f"- mean cap resets: {statistics.mean(cap_resets):.2f}",
            "",
        ])
    return lines


def main() -> None:
    parser = argparse.ArgumentParser(description="Sword Forge balance simulator")
    parser.add_argument("--runs", type=int, default=10000)
    parser.add_argument("--max-attempts", type=int, default=5000)
    args = parser.parse_args()

    weapons = load_weapons()
    enhance_table = load_enhance_table()
    reward_table = load_reward_table()
    evolution_requirements = load_evolution_requirements()
    purchase_costs = json.loads(PURCHASE_COSTS_PATH.read_text(encoding="utf-8"))
    purchase_costs_by_id = {entry["weaponId"]: entry["cost"] for entry in purchase_costs}
    sale_prices = load_weapon_gold_values(WEAPON_SALE_PRICES_PATH, "goldPrice")
    enhance_costs = load_weapon_gold_values(ENHANCE_COSTS_PATH, "goldCost")
    weapons_by_id = {weapon.id: weapon for weapon in weapons}

    targets = [
        ("normal_01", "rare_01"),
        ("rare_01", "epic_01"),
        ("epic_01", "legendary_01"),
        ("legendary_01", "legendary_04"),
    ]

    OUTPUT_CSV.parent.mkdir(parents=True, exist_ok=True)
    rows: List[dict] = []

    for start_weapon_id, target_weapon_id in targets:
        for _ in range(args.runs):
            rows.append(
                simulate_once(
                    start_weapon_id=start_weapon_id,
                    target_weapon_id=target_weapon_id,
                    weapons_by_id=weapons_by_id,
                    enhance_table=enhance_table,
                    reward_table=reward_table,
                    evolution_requirements=evolution_requirements,
                    purchase_costs=purchase_costs_by_id,
                    sale_prices=sale_prices,
                    enhance_costs=enhance_costs,
                    max_attempts=args.max_attempts,
                )
            )

    with OUTPUT_CSV.open("w", newline="", encoding="utf-8") as fp:
        writer = csv.DictWriter(
            fp,
                fieldnames=[
                    "start_weapon_id",
                    "target_weapon_id",
                    "attempts",
                    "destructions",
                    "repurchases",
                    "evolutions",
                    "cap_resets",
                    "reached",
                    "initial_gold",
                    "material_sources",
                    "material_sinks",
                    "remaining_materials",
                ],
            )
        writer.writeheader()
        for row in rows:
            writer.writerow({
                **row,
                "material_sources": json.dumps(row["material_sources"], ensure_ascii=False),
                "material_sinks": json.dumps(row["material_sinks"], ensure_ascii=False),
                "remaining_materials": json.dumps(row["remaining_materials"], ensure_ascii=False),
            })

    OUTPUT_MD.write_text(summarize(rows), encoding="utf-8")
    print(f"wrote {OUTPUT_CSV}")
    print(f"wrote {OUTPUT_MD}")


if __name__ == "__main__":
    main()
