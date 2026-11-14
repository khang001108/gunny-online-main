#!/usr/bin/env python3
"""Interactive Docker updater for Gunny 7.8.0. Never executes a SQL migration."""
import argparse
import contextlib
import datetime
import fcntl
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
VERSION = "7.8.0"
PROJECT = "mnquwasxacifkktmckdv"
MIGRATION = ROOT / "supabase/migrations/202611120001_virtual_accounts_retention.sql"
EXPANSION = ROOT / "supabase/migrations/202611130001_forge_dungeon.sql"


class UpdateError(RuntimeError):
    pass


class Docker:
    def call(self, *args, missing_ok=False):
        result = subprocess.run(["docker", *args], capture_output=True, text=True)
        if result.returncode:
            if missing_ok and ("No such" in result.stderr or "not found" in result.stderr):
                return None
            if "permission denied" in result.stderr.lower():
                raise UpdateError("Chưa có quyền Docker. Chạy lại tool bằng sudo.")
            raise UpdateError("Docker không xử lý được thao tác " + args[0] + ": " + result.stderr.strip()[:400])
        return result.stdout.strip()

    def container(self, name, missing_ok=False):
        data = self.call("container", "inspect", name, missing_ok=missing_ok)
        return json.loads(data)[0] if data is not None else None

    def image(self, name, missing_ok=False):
        data = self.call("image", "inspect", name, missing_ok=missing_ok)
        return json.loads(data)[0] if data is not None else None

    def build(self, image):
        print("Đang build image; container hiện tại vẫn chạy.", flush=True)
        result = subprocess.run(["docker", "build", "-t", image, str(ROOT)])
        if result.returncode:
            raise UpdateError("Build image thất bại; chưa thay container đang chạy.")


def environment(container):
    return dict(line.split("=", 1) for line in container["Config"].get("Env", []) if "=" in line)


def private_json(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    fd, temporary = tempfile.mkstemp(dir=path.parent, prefix=".gunny-")
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, "w") as out:
            json.dump(data, out, ensure_ascii=False, indent=2)
            out.flush()
            os.fsync(out.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


@contextlib.contextmanager
def locked(state_dir):
    state_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    with (state_dir / "update.lock").open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise UpdateError("Một lần cập nhật khác đang chạy.")
        yield


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def migration_status(env):
    origin = env.get("SUPABASE_URL", "").rstrip("/")
    url = urllib.parse.urlsplit(origin)
    if url.scheme != "https" or url.hostname != PROJECT + ".supabase.co" or url.username or url.query or url.fragment or url.path:
        raise UpdateError("SUPABASE_URL không phải project đang dùng: " + PROJECT)
    secret = env.get("SUPABASE_SECRET_KEY") or env.get("SUPABASE_SERVICE_ROLE_KEY")
    if not secret:
        raise UpdateError("Container thiếu khóa server Supabase.")
    headers = {"apikey": secret, "Content-Type": "application/json"}
    if not secret.startswith("sb_secret_"):
        headers["Authorization"] = "Bearer " + secret
    opener = urllib.request.build_opener(NoRedirect())

    def request(path, body=None):
        req = urllib.request.Request(origin + path, headers=headers,
                                     data=None if body is None else json.dumps(body).encode())
        try:
            with opener.open(req, timeout=15) as response:
                return json.load(response)
        except (urllib.error.URLError, ValueError, OSError):
            raise UpdateError("Chưa xác minh được SQL 7.7.0 + 7.8.0. Tự chạy file migration trong Supabase SQL Editor rồi thử lại. Tool không chạy SQL.") from None

    policy = request("/rest/v1/rpc/gunny_admin", {"p_op": "retention-policy", "p_data": {}})
    if not isinstance(policy, dict) or type(policy.get("enabled")) is not bool or type(policy.get("offline_days")) is not int or not 1 <= policy["offline_days"] <= 3650:
        raise UpdateError("SQL chưa trả về chính sách không hoạt động của 7.7.0.")
    request("/rest/v1/gunny_account_admin?select=user_id,offline_days,protected_until,retention_since,pending_extension_days&limit=0")
    request("/rest/v1/gunny_offline_extensions?select=request_key,user_id,days&limit=0")
    request("/rest/v1/profiles?select=id,stones,forge_levels,dungeon_normal_clear&limit=0")
    request("/rest/v1/gunny780_requests?select=request_key,user_id,operation,payload&limit=0")
    request("/rest/v1/gunny780_card_rewards?select=match_id,user_id,pick,card,stones&limit=0")
    request("/rest/v1/match_results?select=id,dungeon_difficulty&limit=0")
    return policy


def restart_policy(container):
    policy = container["HostConfig"].get("RestartPolicy", {})
    name = policy.get("Name") or "no"
    retries = policy.get("MaximumRetryCount", 0)
    return name + (":" + str(retries) if name == "on-failure" and retries else "")


def validate_container(container):
    if (container["Config"].get("Labels") or {}).get("com.docker.compose.project"):
        raise UpdateError("Container này thuộc stack Compose. Tool sẽ không ghi đè stack; cập nhật qua Portainer theo compose.yaml.")
    if environment(container).get("GUNNY_TEST_MODE") == "1":
        raise UpdateError("Container đang dùng tài khoản RAM thử nghiệm; không cập nhật production từ cấu hình này.")
    network = container["HostConfig"].get("NetworkMode", "bridge")
    if network in ("host", "none") or network.startswith("container:"):
        raise UpdateError("Tool cần container bridge có publish cổng HTTP/TCP, giống gunny-online hiện tại.")
    config = container["HostConfig"]
    if config.get("Privileged") or config.get("Devices") or config.get("CapAdd") or config.get("ReadonlyRootfs") or config.get("Tmpfs"):
        raise UpdateError("Có cấu hình quyền thiết bị/filesystem đặc biệt; tool không tự ý bỏ cấu hình này.")
    for mount in container.get("Mounts", []):
        destination = mount["Destination"].rstrip("/")
        if destination in ("/app", "/app/classes", "/app/assets1", "/app/public", "/opt/gunny"):
            raise UpdateError("Bind mount mã chạy sẽ che image mới: " + destination)
        if mount["Type"] not in ("bind", "volume"):
            raise UpdateError("Có mount ngoài cấu hình được hỗ trợ; chưa đổi container.")
    endpoints(container)  # Require both published ports before any change.


def endpoints(container):
    env = environment(container)
    result = []
    for variable, default in [("PORT", "8080"), ("TCP_PORT", "22122")]:
        key = env.get(variable, default) + "/tcp"
        bindings = container["HostConfig"].get("PortBindings", {}).get(key)
        if not bindings:
            raise UpdateError("Container chưa publish cổng " + key)
        binding = bindings[0]
        port = binding.get("HostPort")
        if not port:
            actual = container.get("NetworkSettings", {}).get("Ports", {}).get(key)
            port = actual[0]["HostPort"] if actual else None
        if not port:
            raise UpdateError("Chưa xác định được cổng host cho " + key)
        host = binding.get("HostIp") or "127.0.0.1"
        if host == "0.0.0.0":
            host = "127.0.0.1"
        elif host == "::":
            host = "::1"
        result.append((host, int(port)))
    return result


def read_frame(sock):
    def receive(size):
        result = bytearray()
        while len(result) < size:
            part = sock.recv(size - len(result))
            if not part:
                raise OSError("TCP đóng kết nối")
            result.extend(part)
        return bytes(result)
    import struct
    code, size = struct.unpack(">II", receive(8))
    if code != 200 or size > 131072:
        raise OSError("Phản hồi TCP không hợp lệ")
    return receive(size)


def verify_endpoints(http, tcp, modern=True):
    import struct
    host = "[" + http[0] + "]" if ":" in http[0] else http[0]
    base = "http://" + host + ":" + str(http[1])
    with urllib.request.urlopen(base + "/health", timeout=3) as response:
        health = json.load(response)
    if health.get("ok") is not True:
        raise OSError("HTTP health chưa sẵn sàng")
    with socket.create_connection(tcp, timeout=3) as sock:
        for _ in range(2):
            sock.sendall(struct.pack(">IBIH3BI", 0x474E5954, 0, 65536, 0, 0, 1, 2, 0))
            health = json.loads(read_frame(sock))
            if health.get("tcp") != 1 or health.get("protocol") != 1:
                raise OSError("Chưa nhận được giao thức game TCP")
            if modern and any(health.get(feature) != 1 for feature in ("events", "charge", "actors", "live", "growth", "expansion", "forge", "dungeon")):
                raise OSError("Image chưa có đồng bộ cấp và trạng thái TCP trực tiếp của 7.8.0")
    if modern:
        for device in ("E72-2009", "E6"):
            with urllib.request.urlopen(base + "/download/Gunny-" + device + "-Online-780.jad", timeout=3) as response:
                jad = response.read(8192).decode()
            if "MIDlet-Version: 7.8.0" not in jad or "socket://" not in jad:
                raise OSError("Bộ cài không khớp bản 7.8.0")


def wait_ready(container, modern=True, timeout=40):
    http, tcp = endpoints(container)
    deadline = time.monotonic() + timeout
    while True:
        try:
            verify_endpoints(http, tcp, modern)
            return
        except (OSError, ValueError):
            if time.monotonic() >= deadline:
                raise UpdateError("Container chưa qua kiểm tra HTTP/TCP/bộ cài trong thời gian chờ.") from None
            time.sleep(.5)


def preview_image(docker, image_id, run_id):
    # This preview has no Supabase keys and can bind only loopback through TEST_MODE.
    sockets = [socket.socket(), socket.socket()]
    try:
        for sock in sockets:
            sock.bind(("127.0.0.1", 0))
        ports = [sock.getsockname()[1] for sock in sockets]
    finally:
        for sock in sockets:
            sock.close()
    name = "gunny-preview-" + run_id
    created = docker.call("create", "--name", name, "--network", "host", "--restart", "no",
                          "--label", "io.gunny.preview=" + run_id,
                          "-e", "GUNNY_TEST_MODE=1", "-e", "PORT=" + str(ports[0]),
                          "-e", "TCP_PORT=" + str(ports[1]), image_id)
    try:
        docker.call("start", created)
        deadline = time.monotonic() + 40
        while True:
            try:
                verify_endpoints(("127.0.0.1", ports[0]), ("127.0.0.1", ports[1]))
                break
            except (OSError, ValueError):
                if time.monotonic() >= deadline:
                    raise UpdateError("Image không qua chạy thử; server hiện tại vẫn được giữ nguyên.") from None
                time.sleep(.5)
    finally:
        docker.call("stop", "--time", "2", created, missing_ok=True)
        docker.call("rm", created, missing_ok=True)


def create_arguments(old, name, image_id, env_path, run_id):
    config = old["HostConfig"]
    args = ["create", "--name", name, "--restart", restart_policy(old), "--env-file", str(env_path),
            "--network", config.get("NetworkMode") or "bridge", "--label", "io.gunny.update-id=" + run_id,
            "--label", "io.gunny.version=" + VERSION]
    for label, value in (old["Config"].get("Labels") or {}).items():
        if not label.startswith("io.gunny."):
            args += ["--label", label + "=" + value]
    primary = config.get("NetworkMode") or "bridge"
    if primary not in ("bridge", "default"):
        network = old.get("NetworkSettings", {}).get("Networks", {}).get(primary, {})
        if network.get("IPAMConfig"):
            raise UpdateError("Network có IP tĩnh riêng; cần cập nhật cấu hình mạng thủ công.")
        for alias in network.get("Aliases") or []:
            if alias not in (name, old["Id"], old["Id"][:12]):
                args += ["--network-alias", alias]
    for port, bindings in config.get("PortBindings", {}).items():
        for binding in bindings or []:
            host = binding.get("HostIp", "")
            if ":" in host:
                host = "[" + host + "]"
            host_port = binding.get("HostPort")
            if not host_port:
                raise UpdateError("Cổng publish ngẫu nhiên không được hỗ trợ khi cập nhật.")
            args += ["--publish", (host + ":" if host else "") + host_port + ":" + port]
    for mount in old.get("Mounts", []):
        source = mount.get("Name") if mount["Type"] == "volume" else mount["Source"]
        if any("," in value for value in (source, mount["Destination"])):
            raise UpdateError("Mount có dấu phẩy cần được cập nhật riêng.")
        spec = "type=" + mount["Type"] + ",src=" + source + ",dst=" + mount["Destination"]
        args += ["--mount", spec + (",readonly" if not mount.get("RW", True) else "")]
    for key, flag in [("Memory", "--memory"), ("MemorySwap", "--memory-swap"), ("CpuShares", "--cpu-shares"), ("PidsLimit", "--pids-limit")]:
        if config.get(key):
            args += [flag, str(config[key])]
    if config.get("NanoCpus"):
        args += ["--cpus", str(config["NanoCpus"] / 1_000_000_000)]
    args.append(image_id)
    return args


def rollback(docker, state_path):
    state = json.loads(state_path.read_text())
    old = docker.container(state["old_id"])
    current = docker.container(state["name"], missing_ok=True)
    if current and current["Id"] != old["Id"]:
        if current["Id"] != state.get("new_id") and (current["Config"].get("Labels") or {}).get("io.gunny.update-id") != state["run_id"]:
            raise UpdateError("Tên container đã thuộc một bản khác; không tự ý ghi đè để rollback.")
        docker.call("stop", "--time", "20", current["Id"])
        docker.call("update", "--restart=no", current["Id"])
        docker.call("rename", current["Id"], state["name"] + "-failed-" + state["run_id"])
    if old["Name"].lstrip("/") != state["name"]:
        docker.call("rename", old["Id"], state["name"])
    docker.call("update", "--restart=" + state["old_restart"], old["Id"])
    docker.call("start", old["Id"])
    wait_ready(docker.container(old["Id"]), modern=False)
    state["phase"] = "rolled-back"
    private_json(state_path, state)
    print("Đã quay lại container cũ. Không thay đổi database.", flush=True)


def update(docker, args):
    if not args.sql_confirmed:
        raise UpdateError("Bạn cần tự chạy SQL và xác nhận trước. Dùng menu hoặc thêm --sql-confirmed sau khi đã chạy SQL.")
    old = docker.container(args.container)
    validate_container(old)
    if not old["State"]["Running"]:
        raise UpdateError("Container hiện tại đang dừng; kiểm tra nguyên nhân trước khi cập nhật.")
    policy = migration_status(environment(old))
    print("SQL 7.7.0 + 7.8.0 hợp lệ. Hạn: " + str(policy["offline_days"]) + " ngày; tự xoá: " + ("bật" if policy["enabled"] else "tắt"), flush=True)
    image = docker.image(args.image, missing_ok=True)
    if args.build or image is None:
        docker.build(args.image)
        image = docker.image(args.image)
    if old["Image"] == image["Id"]:
        wait_ready(old)
        print("Container đã chạy đúng image và qua kiểm tra 7.8.0.")
        return
    run_id = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "-" + uuid.uuid4().hex[:6]
    folder = args.state_dir / run_id
    folder.mkdir(parents=True, mode=0o700)
    private_json(folder / "container-before.json", old)
    env = environment(old)
    env["GUNNY_ASSETS"] = "/app/assets1"
    env.pop("GUNNY_TEST_MODE", None)
    env_path = folder / "runtime.env"
    fd = os.open(env_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as out:
        for key, value in env.items():
            if "\n" in value or "\r" in value:
                raise UpdateError("Biến môi trường nhiều dòng cần được cấu hình riêng.")
            out.write(key + "=" + value + "\n")
    command = create_arguments(old, args.container, image["Id"], env_path, run_id)
    networks = old.get("NetworkSettings", {}).get("Networks", {})
    preview_image(docker, image["Id"], run_id)
    print("Image đã qua chạy thử. Sắp đổi container; các trận trong RAM sẽ ngắt.", flush=True)
    state = {"name": args.container, "old_id": old["Id"], "old_restart": restart_policy(old),
             "backup_name": args.container + "-backup-" + run_id, "run_id": run_id,
             "target_image": image["Id"], "phase": "prepared"}
    state_path = folder / "state.json"
    private_json(state_path, state)
    private_json(args.state_dir / "latest.json", {"state_path": str(state_path.resolve())})
    try:
        # Keep the backup stopped even after a Docker daemon reboot.
        docker.call("update", "--restart=no", old["Id"])
        docker.call("stop", "--time", "20", old["Id"])
        docker.call("rename", old["Id"], state["backup_name"])
        state["phase"] = "old-saved"
        private_json(state_path, state)
        state["new_id"] = docker.call(*command)
        state["phase"] = "new-created"
        private_json(state_path, state)
        primary = old["HostConfig"].get("NetworkMode") or "bridge"
        for name, network in networks.items():
            if name != primary and not (primary == "default" and name == "bridge"):
                connect = ["network", "connect"]
                for alias in network.get("Aliases") or []:
                    if alias not in (args.container, old["Id"], old["Id"][:12]):
                        connect += ["--alias", alias]
                docker.call(*connect, name, state["new_id"])
        docker.call("start", state["new_id"])
        wait_ready(docker.container(state["new_id"]))
        state["phase"] = "healthy"
        private_json(state_path, state)
    except (Exception, KeyboardInterrupt) as error:
        print("Cập nhật chưa thành công; đang phục hồi container cũ.", flush=True)
        try:
            rollback(docker, state_path)
        except Exception as recovery:
            raise UpdateError("Cần phục hồi bằng mục Quay lại. Bản ghi: " + str(state_path) + ". Lỗi: " + str(recovery)) from None
        raise UpdateError("Đã phục hồi bản cũ. Lý do: " + str(error)) from None
    print("Cập nhật thành công lên " + VERSION + ". HTTP, TCP và JAD E72/E6 đã qua kiểm tra.")
    print("Container cũ còn giữ tại: " + state["backup_name"])
    print("Cấu hình sao lưu riêng tư: " + str(folder))


def check(docker, args):
    old = docker.container(args.container)
    print("Container: " + args.container + " | image: " + old["Config"]["Image"] + " | " + old["State"]["Status"])
    image = docker.image(args.image, missing_ok=True)
    print("Image " + args.image + ": " + ("đã có" if image else "chưa có; tool sẽ build khi cập nhật"))
    validate_container(old)
    policy = migration_status(environment(old))
    print("SQL 7.7.0 + 7.8.0 đã có: " + str(policy["offline_days"]) + " ngày, tự xoá " + ("bật" if policy["enabled"] else "tắt"))
    print("Có thể chọn Cập nhật. Không có container hay dữ liệu nào vừa bị thay đổi.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", nargs="?", choices=["menu", "check", "update", "rollback"], default="menu")
    parser.add_argument("--container", default="gunny-online")
    parser.add_argument("--image", default="gunny-online:7.8.0")
    parser.add_argument("--state-dir", type=Path, default=ROOT / "deploy/backups/updater")
    parser.add_argument("--sql-confirmed", action="store_true", help="Owner confirms the SQL was run manually; still verify schema")
    parser.add_argument("--build", action="store_true", help="Rebuild even when the image already exists")
    args = parser.parse_args()
    args.state_dir = args.state_dir.resolve()
    if not shutil.which("docker"):
        raise UpdateError("Chưa tìm thấy Docker CLI trên máy này.")
    docker = Docker()
    if args.action == "menu":
        if not sys.stdin.isatty():
            parser.print_help()
            return 0
        print("GUNNY " + VERSION + " · CẬP NHẬT DOCKER")
        print("1. Kiểm tra\n2. Cập nhật lên 7.8.0\n3. Quay lại bản trước\n0. Thoát")
        choice = input("Chọn: ").strip()
        if choice == "0":
            return 0
        args.action = {"1": "check", "2": "update", "3": "rollback"}.get(choice)
        if args.action is None:
            raise UpdateError("Lựa chọn không hợp lệ.")
        if args.action == "update":
            print("Tự chạy các file này trong SQL Editor của project " + PROJECT + ":\n" + str(MIGRATION) + "\n" + str(EXPANSION))
            print("Tool không chạy SQL. Cập nhật sẽ ngắt trận đang chơi; cấu hình và bản cũ sẽ được giữ.")
            if input("Nếu đã chạy SQL và muốn cập nhật, gõ DA CHAY SQL: ").strip() != "DA CHAY SQL":
                print("Chưa cập nhật.")
                return 0
            args.sql_confirmed = True
        elif args.action == "rollback" and input("Quay lại sẽ ngắt trận. Gõ QUAY LAI để tiếp tục: ").strip() != "QUAY LAI":
            print("Chưa quay lại.")
            return 0
    if args.action == "check":
        check(docker, args)
    else:
        with locked(args.state_dir):
            if args.action == "update":
                update(docker, args)
            else:
                latest = args.state_dir / "latest.json"
                if not latest.exists():
                    raise UpdateError("Chưa có bản cập nhật nào của tool để quay lại.")
                rollback(docker, Path(json.loads(latest.read_text())["state_path"]))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (UpdateError, OSError, ValueError) as error:
        print("Lỗi: " + str(error), file=sys.stderr)
        sys.exit(1)
