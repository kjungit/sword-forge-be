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
    "evolution_requirements.json": "evolution_requirement.schema.json",
    "special_items.json": "special_item.schema.json",
}


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

    if len(weapon_ids) != len(weapon_id_set):
        errors.append("weapons.json: duplicate weapon ids detected")

    for weapon in weapons:
        next_weapon_id = weapon.get("nextWeaponId")
        if next_weapon_id is not None and next_weapon_id not in weapon_id_set:
            if weapon["id"] == "legendary_04" and next_weapon_id == "final_awakened_sword":
                continue
            errors.append(f"weapons.json: unknown nextWeaponId {next_weapon_id} for {weapon['id']}")

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
    for row in purchase_costs:
        weapon_id = row["weaponId"]
        if weapon_id not in weapon_id_set:
            errors.append(f"weapon_purchase_costs.json: unknown weaponId {weapon_id}")

    evolution_requirements = load_json(DATA_DIR / "evolution_requirements.json")
    for row in evolution_requirements:
        from_weapon_id = row["fromWeaponId"]
        if from_weapon_id not in weapon_id_set:
            errors.append(f"evolution_requirements.json: unknown fromWeaponId {from_weapon_id}")
        to_grade = row["toGrade"]
        if to_grade not in {"rare", "epic", "legendary"}:
            errors.append(f"evolution_requirements.json: invalid toGrade {to_grade}")

    special_items = load_json(DATA_DIR / "special_items.json")
    special_ids = [item["id"] for item in special_items]
    if len(special_ids) != len(set(special_ids)):
        errors.append("special_items.json: duplicate item ids detected")

    default_save = {
        "userId": "sample-user",
        "currentWeaponId": "normal_01",
        "materials": {},
        "specialItems": {},
        "weaponInventory": {"normal_01": 1},
        "ownedWeaponIds": ["normal_01"],
        "unlockedWeaponShop": ["normal_01"],
        "discoveredWeaponIds": ["normal_01"],
        "highestReachedWeaponId": "normal_01",
        "updatedAt": "2026-01-01T00:00:00Z",
    }
    save_schema = load_json(SCHEMA_DIR / "save_data.schema.json")
    errors.extend(validate_schema(save_schema, default_save, "save_data.schema.json(default save)"))

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Sword Growth JSON schemas and cross-file references.")
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
