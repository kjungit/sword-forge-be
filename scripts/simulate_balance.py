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


def reward_rolls(group_id: str, reward_table: Dict[str, List[Tuple[str, int, int]]]) -> Dict[str, int]:
    rewards: Dict[str, int] = {}
    for material_id, min_amount, max_amount in reward_table[group_id]:
        rewards[material_id] = random.randint(min_amount, max_amount)
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


def pick_affordable_weapon(current: Weapon | None, unlocked: List[str], weapons_by_id: Dict[str, Weapon], materials: Counter) -> str:
    unlocked_weapons = sorted(
        (weapons_by_id[weapon_id] for weapon_id in unlocked),
        key=lambda weapon: (grade_order(weapon.grade), weapon.stage),
    )
    for weapon in reversed(unlocked_weapons):
        if can_afford(materials, calculate_cost(weapon)):
            return weapon.id
    return unlocked_weapons[0].id if unlocked_weapons else "normal_01"


def grade_order(grade: str) -> int:
    return {"normal": 0, "rare": 1, "epic": 2, "legendary": 3}.get(grade, 99)


def simulate_once(
    start_weapon_id: str,
    target_weapon_id: str,
    weapons_by_id: Dict[str, Weapon],
    enhance_table: Dict[str, Tuple[float, float]],
    reward_table: Dict[str, List[Tuple[str, int, int]]],
    evolution_requirements: Dict[str, dict],
    purchase_costs: Dict[str, Dict[str, int]],
    max_attempts: int,
) -> dict:
    current_weapon_id = start_weapon_id
    unlocked = [start_weapon_id]
    weapon_inventory = Counter({start_weapon_id: 1})
    materials = Counter()
    attempts = 0
    destructions = 0
    repurchases = 0
    evolutions = 0

    while current_weapon_id != target_weapon_id and attempts < max_attempts:
        if can_evolve(current_weapon_id, weapon_inventory, materials, evolution_requirements, weapons_by_id):
            current_weapon_id, weapon_inventory, materials = evolve_weapon(
                current_weapon_id,
                weapon_inventory,
                materials,
                evolution_requirements,
                weapons_by_id,
            )
            if current_weapon_id not in unlocked:
                unlocked.append(current_weapon_id)
            evolutions += 1
            continue

        weapon = weapons_by_id[current_weapon_id]
        success_rate, _fail_rate = enhance_table[current_weapon_id]
        attempts += 1
        roll = random.random()
        if roll < success_rate:
            next_weapon_id = weapon.next_weapon_id
            if next_weapon_id is None:
                break
            remove_exact(weapon_inventory, current_weapon_id, 1)
            weapon_inventory[next_weapon_id] += 1
            current_weapon_id = next_weapon_id
            if next_weapon_id not in unlocked:
                unlocked.append(next_weapon_id)
            continue

        destructions += 1
        remove_exact(weapon_inventory, current_weapon_id, 1)
        materials = merge_materials(materials, reward_rolls(weapon.failure_reward_group, reward_table))
        current_weapon_id = "normal_01"
        weapon_inventory["normal_01"] += 1

        if unlocked:
            purchase_weapon_id = pick_affordable_weapon(None, unlocked, weapons_by_id, materials)
            purchase_cost = purchase_costs[purchase_weapon_id]
            if can_afford(materials, purchase_cost):
                materials = spend(materials, purchase_cost)
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
        "reached": current_weapon_id == target_weapon_id,
        "remaining_materials": dict(materials),
    }


def summarize(rows: List[dict]) -> str:
    attempts = [row["attempts"] for row in rows]
    destructions = [row["destructions"] for row in rows]
    repurchases = [row["repurchases"] for row in rows]
    evolutions = [row["evolutions"] for row in rows]
    reached_count = sum(1 for row in rows if row["reached"])

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
        f"- p10 attempts: {p10:.2f}",
        f"- p90 attempts: {p90:.2f}",
        "",
    ]

    if statistics.mean(attempts) > 500:
        lines.append("- warning: progression is very slow")
    if reached_count / len(rows) < 0.5:
        lines.append("- warning: many runs fail to reach the target")

    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="Sword Growth balance simulator")
    parser.add_argument("--runs", type=int, default=10000)
    parser.add_argument("--max-attempts", type=int, default=5000)
    args = parser.parse_args()

    weapons = load_weapons()
    enhance_table = load_enhance_table()
    reward_table = load_reward_table()
    evolution_requirements = load_evolution_requirements()
    purchase_costs = json.loads(PURCHASE_COSTS_PATH.read_text(encoding="utf-8"))
    purchase_costs_by_id = {entry["weaponId"]: entry["cost"] for entry in purchase_costs}
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
                    "reached",
                    "remaining_materials",
                ],
            )
        writer.writeheader()
        for row in rows:
            writer.writerow({**row, "remaining_materials": json.dumps(row["remaining_materials"], ensure_ascii=False)})

    OUTPUT_MD.write_text(summarize(rows), encoding="utf-8")
    print(f"wrote {OUTPUT_CSV}")
    print(f"wrote {OUTPUT_MD}")


if __name__ == "__main__":
    main()
