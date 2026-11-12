#!/usr/bin/env python3
"""Read-only Gunny TCP/HTTPS checks; no accounts or third-party modules needed."""
import argparse
import http.client
import json
import socket
import ssl
import struct
import sys
from urllib.parse import urlsplit


def receive(sock, size):
    data = bytearray()
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise EOFError("Server closed the connection before the full reply")
        data.extend(chunk)
    return bytes(data)


def frame(sock, kind, body=b""):
    # magic, kind, flags, empty Java UTF token, three kit bytes, body length
    sock.sendall(struct.pack(">IBIH3BI", 0x474E5954, kind, 65536, 0, 0, 1, 2, len(body)) + body)
    status, size = struct.unpack(">II", receive(sock, 8))
    if status != 200 or size > 131072:
        raise ValueError(f"Invalid TCP reply: status={status}, size={size}")
    return receive(sock, size)


def tcp_check(host, port, timeout):
    addresses = sorted({item[4][0] for item in socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)})
    with socket.create_connection((host, port), timeout=timeout) as sock:
        for _ in range(2):
            health = json.loads(frame(sock, 0))
            if not isinstance(health, dict) or health.get("ok") is not True or health.get("tcp") != 1 or health.get("protocol") != 1:
                raise ValueError("Endpoint does not advertise Gunny TCP protocol 1")
        # An unauthenticated rooms request must reach the HTTP bridge and be rejected.
        body = struct.pack(">IH", 0x474E5931, 5) + b"rooms"
        reply = frame(sock, 1, body)
        if len(reply) < 3 or reply[0] != 2:
            raise ValueError("TCP/API bridge did not return the expected authentication error")
        size = struct.unpack(">H", reply[1:3])[0]
        if len(reply) != 3 + size:
            raise ValueError("Incomplete authentication error from TCP/API bridge")
        return {"addresses": addresses, "peer": sock.getpeername()[0],
                "persistent_requests": 3, "tcp": True, "api_bridge": True}


def web_check(url, connect_ip, timeout):
    origin = urlsplit(url)
    if origin.scheme != "https" or not origin.hostname or origin.username or origin.query or origin.fragment or origin.path not in ("", "/"):
        raise ValueError("--web-url must be an HTTPS origin, e.g. https://gunnyjava.xubi.org")
    port = origin.port or 443
    # Connect to a LAN IP while retaining the real TLS hostname and certificate checks.
    with socket.create_connection((connect_ip or origin.hostname, port), timeout=timeout) as raw:
        with ssl.create_default_context().wrap_socket(raw, server_hostname=origin.hostname) as sock:
            authority = origin.netloc.encode("idna")
            sock.sendall(b"GET /health HTTP/1.1\r\nHost: " + authority + b"\r\nConnection: close\r\n\r\n")
            response = http.client.HTTPResponse(sock)
            response.begin()
            if response.status != 200:
                raise ValueError(f"Web health returned HTTP {response.status}")
            body = response.read(131073)
            if len(body) > 131072:
                raise ValueError("Web health response too large")
            health = json.loads(body)
            if not isinstance(health, dict) or health.get("ok") is not True or health.get("protocol") != 1:
                raise ValueError("Endpoint does not advertise Gunny protocol 1")
            return {"certificate_verified": True, "http_status": response.status,
                    "peer": sock.getpeername()[0], "protocol": health["protocol"]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tcp-host", action="append", help="Repeat to compare localhost, LAN and public DNS")
    parser.add_argument("--tcp-port", type=int, default=22122)
    parser.add_argument("--web-url", help="Public HTTPS origin")
    parser.add_argument("--web-connect-ip", help="Connect web to this IP while verifying the origin certificate")
    parser.add_argument("--timeout", type=float, default=5)
    args = parser.parse_args()
    if args.timeout <= 0 or not 1 <= args.tcp_port <= 65535:
        parser.error("timeout must be positive and TCP port must be 1..65535")
    if args.web_connect_ip and not args.web_url:
        parser.error("--web-connect-ip requires --web-url")
    checks = [(f"tcp://{host}:{args.tcp_port}", lambda h=host: tcp_check(h, args.tcp_port, args.timeout))
              for host in (args.tcp_host or ["127.0.0.1"])]
    if args.web_url:
        checks.append((args.web_url, lambda: web_check(args.web_url, args.web_connect_ip, args.timeout)))
    failed = False
    for endpoint, check in checks:
        try:
            print(json.dumps({"endpoint": endpoint, "ok": True, **check()}, ensure_ascii=False), flush=True)
        except (OSError, ValueError, EOFError, http.client.HTTPException) as error:
            failed = True
            print(json.dumps({"endpoint": endpoint, "ok": False, "error": str(error)}, ensure_ascii=False), flush=True)
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
