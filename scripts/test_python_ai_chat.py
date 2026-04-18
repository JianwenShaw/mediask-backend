#!/usr/bin/env python3

import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any, Dict, List, Optional


# Local config: edit these 4 values for your environment if needed.
DEFAULT_PYTHON_AI_BASE_URL = "http://localhost:8000"
DEFAULT_PYTHON_AI_API_KEY = "python_api_key"
DEFAULT_JAVA_CATALOG_BASE_URL = "http://localhost:8989"
DEFAULT_JAVA_CATALOG_API_KEY = "dev-api-key"

BASE_URL = os.environ.get("PYTHON_AI_BASE_URL", DEFAULT_PYTHON_AI_BASE_URL)
API_KEY = os.environ.get("PYTHON_AI_API_KEY", DEFAULT_PYTHON_AI_API_KEY)
JAVA_CATALOG_BASE_URL = os.environ.get("JAVA_CATALOG_BASE_URL", DEFAULT_JAVA_CATALOG_BASE_URL)
JAVA_CATALOG_API_KEY = os.environ.get("JAVA_CATALOG_API_KEY", DEFAULT_JAVA_CATALOG_API_KEY)
MODEL_RUN_ID = int(os.environ.get("MODEL_RUN_ID", "900001"))
TURN_ID = int(os.environ.get("TURN_ID", "900011"))
SESSION_UUID = os.environ.get("SESSION_UUID", "sess-test-001")
DEPARTMENT_ID = os.environ.get("DEPARTMENT_ID")
HOSPITAL_SCOPE = os.environ.get("HOSPITAL_SCOPE", "default-hospital")
DEPARTMENT_CATALOG_VERSION = os.environ.get("DEPARTMENT_CATALOG_VERSION", "deptcat-test-v1")
PATIENT_TURN_NO = int(os.environ.get("PATIENT_TURN_NO_IN_ACTIVE_CYCLE", "1"))
FORCE_FINALIZE = os.environ.get("FORCE_FINALIZE", "false").lower() == "true"
SCENE_TYPE = os.environ.get("SCENE_TYPE", "PRE_CONSULTATION")
MESSAGE = os.environ.get("MESSAGE", "头痛三天，伴有低烧，应该先看什么科？")
CONTEXT_SUMMARY = os.environ.get("CONTEXT_SUMMARY")
USE_RAG = os.environ.get("USE_RAG", "false").lower() == "true"
KNOWLEDGE_BASE_IDS_RAW = os.environ.get("KNOWLEDGE_BASE_IDS", "")
REQUEST_ID = os.environ.get("REQUEST_ID", "req_py_direct_chat_001")


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    sys.exit(1)


def parse_knowledge_base_ids(raw: str) -> Optional[List[int]]:
    if not raw.strip():
        return None
    values = []
    for part in raw.split(","):
        value = part.strip()
        if value:
            values.append(int(value))
    return values or None


def request(body: Dict[str, Any]) -> tuple[int, Dict[str, Any]]:
    url = BASE_URL.rstrip("/") + "/api/v1/chat"
    payload = json.dumps(body).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "X-Request-Id": REQUEST_ID,
    }
    if API_KEY:
        headers["X-API-Key"] = API_KEY

    print("==> python chat")
    print("POST", url)
    print(json.dumps(body, ensure_ascii=False, indent=2))

    req = urllib.request.Request(url, data=payload, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            raw = resp.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        status = exc.code
        raw = exc.read().decode("utf-8")
    except urllib.error.URLError as exc:
        fail(f"transport error: {exc}")

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        print(raw)
        fail("response is not valid json")

    print(json.dumps(data, ensure_ascii=False, indent=2))
    return status, data


def fetch_java_catalog() -> Optional[Dict[str, Any]]:
    if not JAVA_CATALOG_BASE_URL:
        return None

    url = JAVA_CATALOG_BASE_URL.rstrip("/") + f"/api/v1/internal/triage-department-catalogs/{HOSPITAL_SCOPE}"
    headers = {"X-Request-Id": REQUEST_ID}
    if JAVA_CATALOG_API_KEY:
        headers["X-API-Key"] = JAVA_CATALOG_API_KEY

    print("==> java catalog")
    print("GET", url)

    req = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            raw = resp.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        status = exc.code
        raw = exc.read().decode("utf-8")
    except urllib.error.URLError as exc:
        fail(f"java catalog transport error: {exc}")

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        print(raw)
        fail("java catalog response is not valid json")

    print(json.dumps(data, ensure_ascii=False, indent=2))
    assert_true(status == 200, f"java catalog expected http 200, got {status}")
    assert_true(data.get("department_catalog_version"), "java catalog missing department_catalog_version")
    assert_true(isinstance(data.get("department_candidates"), list), "java catalog missing department_candidates")
    return data


def assert_true(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def in_values(value: Any, *expected: str) -> bool:
    return value in expected


def main() -> None:
    knowledge_base_ids = parse_knowledge_base_ids(KNOWLEDGE_BASE_IDS_RAW)
    if USE_RAG and not knowledge_base_ids:
        fail("USE_RAG=true 时必须传 KNOWLEDGE_BASE_IDS，例如 KNOWLEDGE_BASE_IDS=1001")

    catalog = fetch_java_catalog()
    department_catalog_version = DEPARTMENT_CATALOG_VERSION
    department_id = int(DEPARTMENT_ID) if DEPARTMENT_ID else None

    if catalog is not None:
        department_catalog_version = catalog["department_catalog_version"]
        if department_id is None and catalog["department_candidates"]:
            department_id = catalog["department_candidates"][0]["department_id"]

    body: Dict[str, Any] = {
        "model_run_id": MODEL_RUN_ID,
        "turn_id": TURN_ID,
        "session_uuid": SESSION_UUID,
        "department_id": department_id,
        "hospital_scope": HOSPITAL_SCOPE,
        "department_catalog_version": department_catalog_version,
        "patient_turn_no_in_active_cycle": PATIENT_TURN_NO,
        "force_finalize": FORCE_FINALIZE,
        "scene_type": SCENE_TYPE,
        "context_summary": CONTEXT_SUMMARY,
        "message": MESSAGE,
        "use_rag": USE_RAG,
        "knowledge_base_ids": knowledge_base_ids,
    }

    status, response = request(body)
    assert_true(status == 200, "python /api/v1/chat expected http 200")
    assert_true(response.get("provider_run_id"), "provider_run_id must not be empty")
    assert_true(in_values(response.get("triage_stage"), "COLLECTING", "READY", "BLOCKED"), "invalid triage_stage")
    assert_true(response.get("answer"), "answer must not be empty")

    triage_stage = response["triage_stage"]
    if triage_stage == "COLLECTING":
        questions = response.get("follow_up_questions") or []
        assert_true(response.get("triage_completion_reason") in (None, ""), "COLLECTING must not have completion reason")
        assert_true(1 <= len(questions) <= 2, "COLLECTING must have 1-2 follow_up_questions")
        assert_true(not response.get("recommended_departments"), "COLLECTING must not return recommended_departments")
        assert_true(not response.get("care_advice"), "COLLECTING must not return care_advice")
    else:
        assert_true(
            in_values(response.get("triage_completion_reason"), "SUFFICIENT_INFO", "MAX_TURNS_REACHED", "HIGH_RISK_BLOCKED"),
            "finalized stage must have valid triage_completion_reason",
        )
        assert_true(not response.get("follow_up_questions"), "READY/BLOCKED must not return follow_up_questions")
        assert_true(isinstance(response.get("recommended_departments"), list), "READY/BLOCKED must return recommended_departments")
        assert_true(response.get("care_advice"), "READY/BLOCKED must return care_advice")

    print("\nPython /api/v1/chat test passed")


if __name__ == "__main__":
    main()
