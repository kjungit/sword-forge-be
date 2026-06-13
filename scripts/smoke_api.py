#!/usr/bin/env python3
import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    sys.exit(1)


def pass_step(message: str) -> None:
    print(f"PASS: {message}")


class ApiClient:
    def __init__(self, base_url: str, username: str, password: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.root_url = self._root_url(self.base_url)
        self.username = username
        self.password = password
        self.csrf_header = ""
        self.csrf_token = ""
        self.csrf_cookie_name = ""

    def _root_url(self, base_url: str) -> str:
        suffix = "/api/v1"
        if base_url.endswith(suffix):
            return base_url[: -len(suffix)]
        return base_url

    def fetch_csrf(self) -> dict:
        data = self.request("GET", "/security/csrf", auth=False)
        self.csrf_header = str(data["headerName"])
        self.csrf_token = str(data["token"])
        self.csrf_cookie_name = str(data.get("cookieName", "XSRF-TOKEN"))
        return data

    def request(
        self,
        method: str,
        path: str,
        payload: dict | None = None,
        auth: bool = True,
        csrf: bool = False,
        root: bool = False,
    ) -> dict:
        url = path if path.startswith("http") else f"{self.root_url if root else self.base_url}{path}"
        body = None
        headers = {"Accept": "application/json"}

        if payload is not None:
            body = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"

        if auth:
            token = base64.b64encode(f"{self.username}:{self.password}".encode("utf-8")).decode("ascii")
            headers["Authorization"] = f"Basic {token}"

        if csrf:
            if not self.csrf_token:
                self.fetch_csrf()
            headers[self.csrf_header] = self.csrf_token
            headers["Cookie"] = f"{self.csrf_cookie_name}={self.csrf_token}"

        req = urllib.request.Request(url, data=body, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=15) as response:
                raw = response.read().decode("utf-8")
                status = response.status
        except urllib.error.HTTPError as exc:
            raw = exc.read().decode("utf-8", errors="replace")
            fail(f"{method} {url} returned HTTP {exc.code}: {raw}")
        except urllib.error.URLError as exc:
            fail(f"{method} {url} failed: {exc.reason}")

        if status < 200 or status >= 300:
            fail(f"{method} {url} returned HTTP {status}: {raw}")

        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            fail(f"{method} {url} did not return JSON: {raw}")

        if isinstance(parsed, dict) and parsed.get("success") is False:
            fail(f"{method} {url} returned success=false: {parsed}")

        if isinstance(parsed, dict) and "data" in parsed:
            return parsed["data"]
        return parsed


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def main() -> None:
    parser = argparse.ArgumentParser(description="Smoke test a running Sword Forge backend.")
    parser.add_argument("--base-url", default=os.getenv("SWORD_FORGE_API_BASE_URL", "http://127.0.0.1:8080/api/v1"))
    parser.add_argument("--user", default=os.getenv("SWORD_FORGE_API_USER", "local_user"))
    parser.add_argument("--password", default=os.getenv("SWORD_FORGE_API_PASSWORD", "local_password"))
    parser.add_argument("--with-enhance-attempt", action="store_true", help="Also run one real enhancement attempt.")
    args = parser.parse_args()

    client = ApiClient(args.base_url, args.user, args.password)

    health = client.request("GET", "/health", auth=False)
    require(health.get("status") == "up", f"health status should be up, got {health}")
    pass_step("health endpoint is up")

    openapi = client.request("GET", "/v3/api-docs", auth=False, root=True)
    require(openapi.get("openapi"), "OpenAPI document should include openapi version")
    pass_step("OpenAPI JSON is public")

    csrf = client.fetch_csrf()
    require(csrf.get("headerName") and csrf.get("token") and csrf.get("cookieName"), f"invalid CSRF response: {csrf}")
    pass_step("CSRF endpoint returns header, token, and cookie name")

    weapons = client.request("GET", "/weapons")
    require(isinstance(weapons, list) and len(weapons) >= 1, "weapon catalog should be a non-empty list")
    pass_step(f"weapon catalog loaded ({len(weapons)} weapons)")

    save = client.request("GET", f"/saves/{urllib.parse.quote(args.user)}")
    require(save.get("currentWeaponId") == "normal_01", f"default current weapon should be normal_01, got {save}")
    require(int(save.get("materials", {}).get("gold", 0)) >= 100000, f"default gold should be at least 100000, got {save}")
    pass_step("save load/create returns starter sword and starting gold")

    preview = client.request(
        "POST",
        "/enhance/preview",
        payload={"userId": args.user, "weaponId": "normal_01", "useProtection": False},
        csrf=True,
    )
    require(preview.get("weaponId") == "normal_01", f"enhance preview weapon mismatch: {preview}")
    require(preview.get("requiredItems") == [], f"normal enhance should require no items: {preview}")
    require("canRetry" not in preview, "preview should not include attempt-only canRetry")
    pass_step("normal enhance preview is gold-only")

    sale_path = f"/shop/sell-preview/normal_02?{urllib.parse.urlencode({'userId': args.user, 'amount': 1})}"
    sale = client.request("GET", sale_path)
    invested = int(sale.get("investedGold", 0))
    sell_gold = int(sale.get("sellGold", 0))
    require(sell_gold >= invested * 2, f"sale price should be at least 2x investment: {sale}")
    pass_step("sale preview satisfies invested-gold floor")

    idle = client.request(
        "POST",
        "/idle/claim",
        payload={"userId": args.user},
        csrf=True,
    )
    require("damage" in idle and "goldGained" in idle and "totalGold" in idle, f"invalid idle claim response: {idle}")
    pass_step("idle claim mutation succeeds with CSRF")

    if args.with_enhance_attempt:
        attempt = client.request(
            "POST",
            "/enhance/attempt",
            payload={"userId": args.user, "weaponId": "normal_01", "useProtection": False},
            csrf=True,
        )
        require("outcome" in attempt and "nextPreview" in attempt and "canRetry" in attempt, f"invalid attempt response: {attempt}")
        pass_step("enhance attempt returns retry context")

    print("Smoke test complete.")


if __name__ == "__main__":
    main()
