#!/usr/bin/env python3

import http.client
import os
import json
import ssl
import sys
import uuid
from urllib.parse import urlparse


BASE_URL = "http://localhost:8989"
USERNAME = "patient_li"
PASSWORD = "patient123"

MESSAGE = "头痛三天"
SCENE_TYPE = "PRE_CONSULTATION"
SESSION_ID = None
DEPARTMENT_ID = None
USE_STREAM = True

ENABLE_COLOR = sys.stdout.isatty() and os.getenv("NO_COLOR") is None

RESET = "\033[0m"
BLUE = "\033[34m"
GREEN = "\033[32m"
YELLOW = "\033[33m"
RED = "\033[31m"
CYAN = "\033[36m"
DIM = "\033[2m"


def style(text, color):
    if not ENABLE_COLOR:
        return text
    return f"{color}{text}{RESET}"


def print_status(label, icon, color):
    print(style(f"{icon} {label}", color))


def print_event_header(event_name):
    if event_name == "message":
        print(style("💬 message", BLUE))
        return
    if event_name == "meta":
        print(style("📦 meta", CYAN))
        return
    if event_name == "error":
        print(style("❌ error", RED))
        return
    if event_name == "end":
        print(style("✅ end", GREEN))
        return
    print(style(f"ℹ️  {event_name}", YELLOW))


def print_event_body(event_name, data_text):
    if not data_text:
        return
    try:
        payload = json.loads(data_text)
    except json.JSONDecodeError:
        print(data_text)
        return

    if event_name == "message" and isinstance(payload, dict) and "content" in payload:
        print(payload["content"])
        return

    print(json.dumps(payload, ensure_ascii=False, indent=2))
    if event_name == "error" and isinstance(payload, dict):
        print(style(f"code: {payload.get('code')}", RED))
        print(style(f"msg: {payload.get('msg')}", RED))


def build_connection(parsed_url):
    if parsed_url.scheme == "https":
        return http.client.HTTPSConnection(parsed_url.hostname, parsed_url.port or 443, context=ssl.create_default_context())
    return http.client.HTTPConnection(parsed_url.hostname, parsed_url.port or 80)


def request_json(method, path, body, headers=None):
    parsed_url = urlparse(BASE_URL)
    connection = build_connection(parsed_url)
    request_headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Request-Id": str(uuid.uuid4()),
    }
    if headers:
        request_headers.update(headers)
    payload = json.dumps(body, ensure_ascii=False).encode("utf-8")
    connection.request(method, path, body=payload, headers=request_headers)
    response = connection.getresponse()
    response_body = response.read().decode("utf-8")
    connection.close()
    return response.status, response_body


def require_success(status, response_body, operation):
    if status != 200:
        raise RuntimeError(f"{operation} failed: http_status={status}, body={response_body}")
    result = json.loads(response_body)
    if result["code"] != 0:
        raise RuntimeError(f"{operation} failed: code={result['code']}, msg={result['msg']}, body={response_body}")
    return result.get("data")


def login():
    status, body = request_json(
        "POST",
        "/api/v1/auth/login",
        {
            "username": USERNAME,
            "password": PASSWORD,
        },
    )
    data = require_success(status, body, "login")
    return data["accessToken"], data["refreshToken"]


def logout(access_token, refresh_token):
    status, body = request_json(
        "POST",
        "/api/v1/auth/logout",
        {
            "refreshToken": refresh_token,
        },
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )
    require_success(status, body, "logout")


def stream_sse(access_token):
    parsed_url = urlparse(BASE_URL)
    connection = build_connection(parsed_url)
    headers = {
        "Content-Type": "application/json",
        "Accept": "text/event-stream",
        "Authorization": f"Bearer {access_token}",
        "X-Request-Id": str(uuid.uuid4()),
    }
    body = json.dumps(
        {
            "sessionId": SESSION_ID,
            "message": MESSAGE,
            "departmentId": DEPARTMENT_ID,
            "sceneType": SCENE_TYPE,
            "useStream": USE_STREAM,
        },
        ensure_ascii=False,
    ).encode("utf-8")
    connection.request("POST", "/api/v1/ai/chat/stream", body=body, headers=headers)
    response = connection.getresponse()
    if response.status != 200:
        response_body = response.read().decode("utf-8")
        connection.close()
        raise RuntimeError(f"sse failed: http_status={response.status}, body={response_body}")

    print(style(f"HTTP {response.status} {response.reason}", DIM))
    print(style(f"Content-Type: {response.getheader('Content-Type')}", DIM))
    print(style(f"X-Request-Id: {response.getheader('X-Request-Id')}", DIM))
    print()

    event_name = None
    data_lines = []
    try:
        while True:
            raw_line = response.readline()
            if not raw_line:
                break

            line = raw_line.decode("utf-8").rstrip("\r\n")
            if line == "":
                if event_name is not None:
                    data_text = "\n".join(data_lines)
                    print_event_header(event_name)
                    print_event_body(event_name, data_text)
                    print()
                    if event_name in {"end", "error"}:
                        break
                event_name = None
                data_lines = []
                continue

            if line.startswith(":"):
                continue
            if line.startswith("event:"):
                event_name = line[len("event:"):].strip()
                continue
            if line.startswith("data:"):
                data_lines.append(line[len("data:"):].lstrip())
                continue
            data_lines.append(line)
    finally:
        connection.close()


def main():
    access_token = None
    refresh_token = None
    try:
        access_token, refresh_token = login()
        print_status("login ok", "🔐", GREEN)
        print()
        stream_sse(access_token)
    finally:
        if access_token and refresh_token:
            logout(access_token, refresh_token)
            print_status("logout ok", "🔓", GREEN)


if __name__ == "__main__":
    try:
        main()
    except Exception as exception:
        print(str(exception), file=sys.stderr)
        sys.exit(1)
