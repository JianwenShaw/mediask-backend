#!/usr/bin/env python3

import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any, Dict, Optional


BASE_URL = os.environ.get("BASE_URL", "http://localhost:8989")
USERNAME = os.environ.get("USERNAME", "patient_li")
PASSWORD = os.environ.get("PASSWORD", "patient123")
DEPARTMENT_ID = int(os.environ.get("DEPARTMENT_ID", "3103"))
SCENE_TYPE = os.environ.get("SCENE_TYPE", "PRE_CONSULTATION")
FIRST_MESSAGE = os.environ.get("FIRST_MESSAGE", "头痛三天，伴有低烧，应该先看什么科？")
FOLLOW_UP_MESSAGES = [
    message.strip()
    for message in os.environ.get(
        "FOLLOW_UP_MESSAGES",
        "头痛是持续性的，体温最高38.2度。||另外有点恶心，没有呕吐。||没有胸痛、呼吸困难、意识改变、肢体无力或言语不清。||这三天症状逐渐加重，吃过退烧药后体温会降一点。",
    ).split("||")
    if message.strip()
]
MAX_PATIENT_TURNS = int(os.environ.get("MAX_PATIENT_TURNS", "5"))


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    sys.exit(1)


def request(
    name: str,
    method: str,
    path: str,
    body: Optional[Dict[str, Any]],
    token: Optional[str] = None,
    request_id: Optional[str] = None,
):
    url = BASE_URL.rstrip("/") + path
    payload = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if request_id:
        headers["X-Request-Id"] = request_id

    print(f"\n==> {name}")
    print(f"{method} {url}")

    req = urllib.request.Request(url, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            raw = resp.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        status = exc.code
        raw = exc.read().decode("utf-8")
    except urllib.error.URLError as exc:
        fail(f"{name} transport error: {exc}")

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        print(raw)
        fail(f"{name} response is not valid json")

    print(json.dumps(data, ensure_ascii=False, indent=2))
    return status, data


def assert_true(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def in_values(value, *expected):
    return value in expected


def assert_triage_result(name: str, body: Dict[str, Any]) -> str:
    assert_true(body.get("code") == 0, f"{name} expected code=0, got {body.get('code')}")
    triage = body["data"]["triageResult"]
    stage = triage["triageStage"]
    assert_true(in_values(stage, "COLLECTING", "READY", "BLOCKED"), f"{name} invalid triageStage: {stage}")

    if stage == "COLLECTING":
        questions = triage.get("followUpQuestions") or []
        assert_true(triage["nextAction"] == "CONTINUE_TRIAGE", f"{name} COLLECTING must map to CONTINUE_TRIAGE")
        assert_true(1 <= len(questions) <= 2, f"{name} COLLECTING must return 1-2 followUpQuestions")
    elif stage == "READY":
        assert_true(triage["nextAction"] == "VIEW_TRIAGE_RESULT", f"{name} READY must map to VIEW_TRIAGE_RESULT")
    else:
        assert_true(
            in_values(triage["nextAction"], "EMERGENCY_OFFLINE", "MANUAL_SUPPORT"),
            f"{name} BLOCKED must map to EMERGENCY_OFFLINE or MANUAL_SUPPORT",
        )
    return stage


def main() -> None:
    login_status, login_body = request(
        "login",
        "POST",
        "/api/v1/auth/login",
        {"username": USERNAME, "password": PASSWORD},
    )
    assert_true(login_status == 200, f"login expected http 200, got {login_status}")
    assert_true(login_body.get("code") == 0, f"login expected code=0, got {login_body.get('code')}")

    token = login_body["data"]["accessToken"]
    refresh_token = login_body["data"]["refreshToken"]

    chat1_status, chat1_body = request(
        "chat1",
        "POST",
        "/api/v1/ai/chat",
        {
            "sessionId": None,
            "message": FIRST_MESSAGE,
            "departmentId": DEPARTMENT_ID,
            "sceneType": SCENE_TYPE,
            "useStream": False,
        },
        token=token,
        request_id="req_py_ai_chat_001",
    )
    assert_true(chat1_status == 200, f"chat1 expected http 200, got {chat1_status}")
    stage = assert_triage_result("chat1", chat1_body)

    session_id = chat1_body["data"]["sessionId"]
    turn_no = 1
    for index, message in enumerate(FOLLOW_UP_MESSAGES, start=2):
        if stage != "COLLECTING":
            break
        chat_status, chat_body = request(
            f"chat{index}",
            "POST",
            "/api/v1/ai/chat",
            {
                "sessionId": int(session_id),
                "message": message,
                "departmentId": DEPARTMENT_ID,
                "sceneType": SCENE_TYPE,
                "useStream": False,
            },
            token=token,
            request_id=f"req_py_ai_chat_{index:03d}",
        )
        assert_true(chat_status == 200, f"chat{index} expected http 200, got {chat_status}")
        stage = assert_triage_result(f"chat{index}", chat_body)
        turn_no = index

    assert_true(
        stage in ("READY", "BLOCKED"),
        f"chat should finalize within {MAX_PATIENT_TURNS} patient turns, last stage={stage}, lastTurn={turn_no}",
    )

    reject_status, reject_body = request(
        "chat_use_stream",
        "POST",
        "/api/v1/ai/chat",
        {
            "sessionId": None,
            "message": "这个请求应该被拒绝",
            "departmentId": DEPARTMENT_ID,
            "sceneType": SCENE_TYPE,
            "useStream": True,
        },
        token=token,
        request_id="req_py_ai_chat_003",
    )
    assert_true(reject_status == 400, f"useStream=true expected http 400, got {reject_status}")
    assert_true(reject_body.get("code") == 1002, f"useStream=true expected code=1002, got {reject_body.get('code')}")

    triage_status, triage_body = request(
        "triage_result",
        "GET",
        f"/api/v1/ai/sessions/{session_id}/triage-result",
        None,
        token=token,
        request_id="req_py_ai_triage_001",
    )
    assert_true(triage_status == 200, f"triage-result expected http 200 after finalized chat, got {triage_status}")
    assert_true(triage_body.get("code") == 0, f"triage-result expected code=0, got {triage_body.get('code')}")
    triage_data = triage_body["data"]
    assert_true(in_values(triage_data["resultStatus"], "CURRENT", "UPDATING"), "invalid resultStatus")
    assert_true(in_values(triage_data["triageStage"], "READY", "BLOCKED"), "triage-result must be READY or BLOCKED")

    handoff_status, handoff_body = request(
        "handoff",
        "POST",
        f"/api/v1/ai/sessions/{session_id}/registration-handoff",
        {},
        token=token,
        request_id="req_py_ai_handoff_001",
    )
    if handoff_status == 200:
        assert_true(handoff_body.get("code") == 0, f"handoff expected code=0, got {handoff_body.get('code')}")
        if triage_data["triageStage"] == "BLOCKED":
            assert_true(
                handoff_body["data"]["blockedReason"] == "EMERGENCY_OFFLINE",
                "BLOCKED handoff must return EMERGENCY_OFFLINE",
            )
    elif handoff_status == 409:
        assert_true(handoff_body.get("code") == 6020, f"handoff expected code=6020, got {handoff_body.get('code')}")
    else:
        fail(f"handoff expected http 200 or 409 after finalized chat, got {handoff_status}")

    logout_status, logout_body = request(
        "logout",
        "POST",
        "/api/v1/auth/logout",
        {"refreshToken": refresh_token},
        token=token,
        request_id="req_py_ai_logout_001",
    )
    assert_true(logout_status == 200, f"logout expected http 200, got {logout_status}")
    assert_true(logout_body.get("code") == 0, f"logout expected code=0, got {logout_body.get('code')}")

    print("\nAI chat python test passed")
    print(f"sessionId={session_id}")


if __name__ == "__main__":
    main()
