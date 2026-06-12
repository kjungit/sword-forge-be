#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from datetime import datetime
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "src/main/resources/data"
SCHEMA_DIR = ROOT / "schemas"

SCHEMAS = {
    "weapons.json": "weapon.schema.json",
    "enhance_table.json": "enhance.schema.json",
    "failure_rewards.json": "failure_reward.schema.json",
    "weapon_purchase_costs.json": "weapon_purchase.schema.json",
    "weapon_sale_prices.json": "weapon_sale.schema.json",
    "enhance_costs.json": "enhance_cost.schema.json",
    "evolution_requirements.json": "evolution_requirement.schema.json",
    "special_items.json": "special_item.schema.json",
    "item_purchase_prices.json": "item_purchase.schema.json",
}

KNOWN_MATERIAL_IDS = {
    "cracked_iron_piece",
    "cold_ember_powder",
    "blue_black_jade_fragment",
    "grudge_iron_dust",
    "cracked_soul_stone",
    "abyss_trace",
    "sealed_stardust",
    "cataclysm_remnant",
    "evolution_fixed_stone",
    "black_jade_heart",
    "gold",
}

GRADE_ORDER = {"normal": 0, "rare": 1, "epic": 2, "legendary": 3}


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def validate_schema(schema: dict[str, Any], value: Any, label: str, path: str = "$") -> list[str]:
    errors: list[str] = []

    schema_type = schema.get("type")
    if schema_type == "object":
        if not isinstance(value, dict):
            return [f"{label}{path}: expected object"]
        properties = schema.get("properties", {})
        required = schema.get("required", [])
        for key in required:
            if key not in value:
                errors.append(f"{label}{path}: missing required property '{key}'")
        if schema.get("additionalProperties", True) is False:
            allowed_keys = set(properties.keys())
            extra_keys = set(value.keys()) - allowed_keys
            if extra_keys:
                errors.append(f"{label}{path}: additional properties not allowed: {sorted(extra_keys)}")
        for key, child_schema in properties.items():
            if key in value:
                errors.extend(validate_schema(child_schema, value[key], label, f"{path}.{key}"))
        additional_properties = schema.get("additionalProperties", True)
        if isinstance(additional_properties, dict):
            known_keys = set(properties.keys())
            for key, child_value in value.items():
                if key not in known_keys:
                    errors.extend(validate_schema(additional_properties, child_value, label, f"{path}.{key}"))
        return errors

    if schema_type == "array":
        if not isinstance(value, list):
            return [f"{label}{path}: expected array"]
        item_schema = schema.get("items")
        if item_schema is not None:
            for index, item in enumerate(value):
                errors.extend(validate_schema(item_schema, item, label, f"{path}[{index}]"))
        return errors

    if schema_type == "string":
        if not isinstance(value, str):
            return [f"{label}{path}: expected string"]
        if "minLength" in schema and len(value) < schema["minLength"]:
            errors.append(f"{label}{path}: string shorter than minLength {schema['minLength']}")
        if "enum" in schema and value not in schema["enum"]:
            errors.append(f"{label}{path}: value '{value}' not in enum {schema['enum']}")
        if schema.get("format") == "date-time":
            if not is_datetime(value):
                errors.append(f"{label}{path}: invalid date-time format")
        return errors

    if schema_type == "integer":
        if not is_integer(value):
            return [f"{label}{path}: expected integer"]
        if "minimum" in schema and value < schema["minimum"]:
            errors.append(f"{label}{path}: value {value} < minimum {schema['minimum']}")
        if "maximum" in schema and value > schema["maximum"]:
            errors.append(f"{label}{path}: value {value} > maximum {schema['maximum']}")
        return errors

    if schema_type == "number":
        if not is_number(value):
            return [f"{label}{path}: expected number"]
        if "minimum" in schema and value < schema["minimum"]:
            errors.append(f"{label}{path}: value {value} < minimum {schema['minimum']}")
        if "maximum" in schema and value > schema["maximum"]:
            errors.append(f"{label}{path}: value {value} > maximum {schema['maximum']}")
        return errors

    if schema_type == "boolean":
        if not isinstance(value, bool):
            return [f"{label}{path}: expected boolean"]
        return errors

    return errors


def is_integer(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def is_number(value: Any) -> bool:
    return (isinstance(value, int) or isinstance(value, float)) and not isinstance(value, bool)


def is_datetime(value: str) -> bool:
    normalized = value[:-1] + "+00:00" if value.endswith("Z") else value
    try:
        datetime.fromisoformat(normalized)
        return True
    except ValueError:
        return False


def validate_schema_file(schema_filename: str, data_filename: str) -> list[str]:
    schema = load_json(SCHEMA_DIR / schema_filename)
    data = load_json(DATA_DIR / data_filename)
    if isinstance(data, list):
        errors: list[str] = []
        for index, item in enumerate(data):
            errors.extend(validate_schema(schema, item, data_filename, f"$[{index}]"))
        return errors
    return validate_schema(schema, data, data_filename)


def cross_validate() -> list[str]:
    errors: list[str] = []
    weapons = load_json(DATA_DIR / "weapons.json")
    weapon_ids = [item["id"] for item in weapons]
    weapon_id_set = set(weapon_ids)
    weapons_by_id = {item["id"]: item for item in weapons}

    if len(weapon_ids) != len(weapon_id_set):
        errors.append("weapons.json: duplicate weapon ids detected")

    for weapon in weapons:
        next_weapon_id = weapon.get("nextWeaponId")
        if next_weapon_id is not None and next_weapon_id not in weapon_id_set:
            errors.append(f"weapons.json: unknown nextWeaponId {next_weapon_id} for {weapon['id']}")
            continue
        if next_weapon_id is not None:
            next_weapon = weapons_by_id[next_weapon_id]
            if next_weapon["grade"] != weapon["grade"]:
                errors.append(
                    f"weapons.json: grade-crossing nextWeaponId is not allowed: {weapon['id']} -> {next_weapon_id}"
                )
            if next_weapon["stage"] != weapon["stage"] + 1:
                errors.append(
                    f"weapons.json: nextWeaponId must advance exactly one stage: {weapon['id']} -> {next_weapon_id}"
                )

    enhance_table = load_json(DATA_DIR / "enhance_table.json")
    enhance_ids = set()
    for row in enhance_table:
        weapon_id = row["weaponId"]
        if weapon_id not in weapon_id_set:
            errors.append(f"enhance_table.json: unknown weaponId {weapon_id}")
        if weapon_id in enhance_ids:
            errors.append(f"enhance_table.json: duplicate weaponId {weapon_id}")
        enhance_ids.add(weapon_id)
        if round(row["successRate"] + row["failRate"], 10) != 1.0:
            errors.append(f"enhance_table.json: successRate + failRate must equal 1.0 for {weapon_id}")

    purchase_costs = load_json(DATA_DIR / "weapon_purchase_costs.json")
    purchase_weapon_ids = set()
    for row in purchase_costs:
        weapon_id = row["weaponId"]
        if weapon_id not in weapon_id_set:
            errors.append(f"weapon_purchase_costs.json: unknown weaponId {weapon_id}")
        if weapon_id in purchase_weapon_ids:
            errors.append(f"weapon_purchase_costs.json: duplicate weaponId {weapon_id}")
        purchase_weapon_ids.add(weapon_id)
        for material_id in row["cost"]:
            if material_id not in KNOWN_MATERIAL_IDS:
                errors.append(f"weapon_purchase_costs.json: unknown materialId {material_id} for {weapon_id}")
    if purchase_weapon_ids != weapon_id_set:
        missing = sorted(weapon_id_set - purchase_weapon_ids)
        extra = sorted(purchase_weapon_ids - weapon_id_set)
        if missing:
            errors.append(f"weapon_purchase_costs.json: missing weapon ids {missing}")
        if extra:
            errors.append(f"weapon_purchase_costs.json: extra weapon ids {extra}")

    sale_prices = load_json(DATA_DIR / "weapon_sale_prices.json")
    sale_weapon_ids = set()
    for row in sale_prices:
        weapon_id = row["weaponId"]
        if weapon_id not in weapon_id_set:
            errors.append(f"weapon_sale_prices.json: unknown weaponId {weapon_id}")
        if weapon_id in sale_weapon_ids:
            errors.append(f"weapon_sale_prices.json: duplicate weaponId {weapon_id}")
        sale_weapon_ids.add(weapon_id)
    if sale_weapon_ids != weapon_id_set:
        missing = sorted(weapon_id_set - sale_weapon_ids)
        extra = sorted(sale_weapon_ids - weapon_id_set)
        if missing:
            errors.append(f"weapon_sale_prices.json: missing weapon ids {missing}")
        if extra:
            errors.append(f"weapon_sale_prices.json: extra weapon ids {extra}")

    enhance_costs = load_json(DATA_DIR / "enhance_costs.json")
    enhance_cost_weapon_ids = set()
    for row in enhance_costs:
        weapon_id = row["weaponId"]
        if weapon_id not in weapon_id_set:
            errors.append(f"enhance_costs.json: unknown weaponId {weapon_id}")
        if weapon_id in enhance_cost_weapon_ids:
            errors.append(f"enhance_costs.json: duplicate weaponId {weapon_id}")
        enhance_cost_weapon_ids.add(weapon_id)
    if enhance_cost_weapon_ids != weapon_id_set:
        missing = sorted(weapon_id_set - enhance_cost_weapon_ids)
        extra = sorted(enhance_cost_weapon_ids - weapon_id_set)
        if missing:
            errors.append(f"enhance_costs.json: missing weapon ids {missing}")
        if extra:
            errors.append(f"enhance_costs.json: extra weapon ids {extra}")

    failure_rewards = load_json(DATA_DIR / "failure_rewards.json")
    reward_group_ids = [row["groupId"] for row in failure_rewards]
    if len(reward_group_ids) != len(set(reward_group_ids)):
        errors.append("failure_rewards.json: duplicate group ids detected")
    reward_group_id_set = set(reward_group_ids)
    reward_materials_by_group = {}
    for row in failure_rewards:
        reward_materials_by_group[row["groupId"]] = {
            reward["materialId"]
            for reward in row["rewards"]
            if reward["maxAmount"] > 0
        }
        for reward in row["rewards"]:
            material_id = reward["materialId"]
            if material_id not in KNOWN_MATERIAL_IDS:
                errors.append(f"failure_rewards.json: unknown materialId {material_id} in {row['groupId']}")
            if reward["minAmount"] > reward["maxAmount"]:
                errors.append(f"failure_rewards.json: minAmount > maxAmount in {row['groupId']}:{material_id}")

    for weapon in weapons:
        if weapon["failureRewardGroup"] not in reward_group_id_set:
            errors.append(f"weapons.json: unknown failureRewardGroup {weapon['failureRewardGroup']} for {weapon['id']}")

    accessible_reward_materials_by_grade = {}
    for weapon in weapons:
        if weapon.get("nextWeaponId") is None:
            continue
        accessible_reward_materials_by_grade.setdefault(weapon["grade"], set()).update(
            reward_materials_by_group.get(weapon["failureRewardGroup"], set())
        )

    evolution_requirements = load_json(DATA_DIR / "evolution_requirements.json")
    max_stage_by_grade = {}
    for weapon in weapons:
        max_stage_by_grade[weapon["grade"]] = max(max_stage_by_grade.get(weapon["grade"], 0), weapon["stage"])
    for row in evolution_requirements:
        from_weapon_id = row["fromWeaponId"]
        if from_weapon_id not in weapon_id_set:
            errors.append(f"evolution_requirements.json: unknown fromWeaponId {from_weapon_id}")
            continue
        source_weapon = weapons_by_id[from_weapon_id]
        if source_weapon["stage"] != max_stage_by_grade[source_weapon["grade"]]:
            errors.append(f"evolution_requirements.json: fromWeaponId must be the max stage for its grade: {from_weapon_id}")
        to_grade = row["toGrade"]
        if to_grade not in {"rare", "epic", "legendary"}:
            errors.append(f"evolution_requirements.json: invalid toGrade {to_grade}")
        elif GRADE_ORDER[to_grade] != GRADE_ORDER[source_weapon["grade"]] + 1:
            errors.append(f"evolution_requirements.json: toGrade must be the next grade for {from_weapon_id}")
        if source_weapon.get("nextWeaponId") is not None:
            errors.append(f"evolution_requirements.json: evolution source must not also have nextWeaponId: {from_weapon_id}")
        for material_id in row["requiredMaterials"]:
            if material_id not in KNOWN_MATERIAL_IDS:
                errors.append(f"evolution_requirements.json: unknown materialId {material_id} for {from_weapon_id}")
            elif material_id not in accessible_reward_materials_by_grade.get(source_weapon["grade"], set()):
                errors.append(
                    "evolution_requirements.json: "
                    f"required material {material_id} for {from_weapon_id} is not available before the grade cap"
                )
        for requirement_id in row["requiredWeapons"]:
            if requirement_id.endswith("_or_higher"):
                base_id = requirement_id[: -len("_or_higher")]
                if base_id not in weapon_id_set:
                    errors.append(f"evolution_requirements.json: unknown range weapon base {requirement_id}")
            elif requirement_id not in weapon_id_set:
                errors.append(f"evolution_requirements.json: unknown required weapon {requirement_id}")

    special_items = load_json(DATA_DIR / "special_items.json")
    special_ids = [item["id"] for item in special_items]
    special_id_set = set(special_ids)
    if len(special_ids) != len(set(special_ids)):
        errors.append("special_items.json: duplicate item ids detected")

    item_prices = load_json(DATA_DIR / "item_purchase_prices.json")
    item_price_ids = set()
    for row in item_prices:
        item_id = row["itemId"]
        if item_id not in special_id_set:
            errors.append(f"item_purchase_prices.json: unknown itemId {item_id}")
        if item_id in item_price_ids:
            errors.append(f"item_purchase_prices.json: duplicate itemId {item_id}")
        item_price_ids.add(item_id)
    if item_price_ids != special_id_set:
        missing = sorted(special_id_set - item_price_ids)
        extra = sorted(item_price_ids - special_id_set)
        if missing:
            errors.append(f"item_purchase_prices.json: missing item ids {missing}")
        if extra:
            errors.append(f"item_purchase_prices.json: extra item ids {extra}")

    default_save = {
        "userId": "sample-user",
        "currentWeaponId": "normal_01",
        "materials": {"gold": 20},
        "specialItems": {},
        "weaponInventory": {"normal_01": 1},
        "ownedWeaponIds": ["normal_01"],
        "unlockedWeaponShop": ["normal_01"],
        "discoveredWeaponIds": ["normal_01"],
        "highestReachedWeaponId": "normal_01",
        "pityStacks": {},
        "updatedAt": "2026-01-01T00:00:00Z",
    }
    save_schema = load_json(SCHEMA_DIR / "save_data.schema.json")
    errors.extend(validate_schema(save_schema, default_save, "save_data.schema.json(default save)"))

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Sword Forge JSON schemas and cross-file references.")
    parser.parse_args()

    errors: list[str] = []
    for data_filename, schema_filename in SCHEMAS.items():
        errors.extend(validate_schema_file(schema_filename, data_filename))
    errors.extend(cross_validate())

    if errors:
        print("validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print("all JSON schemas and cross-file checks passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
