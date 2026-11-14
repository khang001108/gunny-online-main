"""Exercise deployment failure recovery without changing Docker or Supabase."""
import argparse
import copy
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from contextlib import redirect_stdout
from unittest.mock import patch, MagicMock

SPEC = importlib.util.spec_from_file_location("gunny_updater", Path(__file__).resolve().parents[2] / "deploy/update-gunny.py")
updater = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(updater)


def original():
    return {"Id": "old-id", "Name": "/gunny-online", "Image": "old-image", "Mounts": [],
            "State": {"Running": True, "Status": "running"},
            "Config": {"Image": "gunny-online:7.6.9", "Labels": None,
                       "Env": ["SUPABASE_URL=https://mnquwasxacifkktmckdv.supabase.co", "SUPABASE_SECRET_KEY=fixture-private-key", "PORT=8080", "TCP_PORT=22122", "GUNNY_ASSETS=/opt/gunny/assets1"]},
            "HostConfig": {"NetworkMode": "bridge", "RestartPolicy": {"Name": "always"},
                           "PortBindings": {"8080/tcp": [{"HostIp": "127.0.0.1", "HostPort": "8080"}], "22122/tcp": [{"HostIp": "", "HostPort": "22122"}]}},
            "NetworkSettings": {"Networks": {"bridge": {}}, "Ports": {}}}


class FakeDocker:
    def __init__(self):
        self.containers = {"old-id": original()}
        self.calls = []
        self.fail_create = False

    def container(self, name, missing_ok=False):
        for container in self.containers.values():
            if name in (container["Id"], container["Name"].lstrip("/")):
                return copy.deepcopy(container)
        if missing_ok:
            return None
        raise updater.UpdateError("missing container")

    def image(self, name, missing_ok=False):
        return {"Id": "new-image"}

    def build(self, name):
        raise AssertionError("Should reuse the prepared image")

    def call(self, *args, **kwargs):
        self.calls.append(args)
        operation = args[0]
        if operation == "create":
            if self.fail_create:
                raise updater.UpdateError("fixture create failed")
            container = original()
            container.update(Id="new-id", Name="/" + args[args.index("--name") + 1], Image=args[-1])
            container["State"] = {"Running": False, "Status": "created"}
            container["Config"]["Image"] = args[-1]
            container["Config"]["Env"] = Path(args[args.index("--env-file") + 1]).read_text().splitlines()
            labels = {}
            for index, value in enumerate(args[:-1]):
                if value == "--label":
                    key, label = args[index + 1].split("=", 1)
                    labels[key] = label
            container["Config"]["Labels"] = labels
            self.containers["new-id"] = container
            return "new-id"
        if operation == "update":
            container = self.containers[args[-1]]
            container["HostConfig"]["RestartPolicy"]["Name"] = args[1].split("=", 1)[1]
        elif operation == "stop":
            self.containers[args[-1]]["State"].update(Running=False, Status="exited")
        elif operation == "start":
            self.containers[args[-1]]["State"].update(Running=True, Status="running")
        elif operation == "rename":
            self.containers[args[1]]["Name"] = "/" + args[2]
        else:
            raise AssertionError(args)
        return ""


class UpdaterChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.args = argparse.Namespace(container="gunny-online", image="gunny-online:7.8.0", state_dir=Path(self.temp.name), sql_confirmed=True, build=False)
        self.docker = FakeDocker()
        self.policy = patch.object(updater, "migration_status", return_value={"enabled": False, "offline_days": 90})
        self.preview = patch.object(updater, "preview_image")
        self.ready = patch.object(updater, "wait_ready")
        self.policy.start()
        self.preview.start()
        self.ready.start()
        self.addCleanup(patch.stopall)
        self.output = io.StringIO()

    def deploy(self):
        with redirect_stdout(self.output):
            updater.update(self.docker, self.args)

    def latest(self):
        return Path(json.loads((self.args.state_dir / "latest.json").read_text())["state_path"])

    def test_explicit_sql_confirmation_is_required(self):
        self.args.sql_confirmed = False
        with self.assertRaises(updater.UpdateError):
            self.deploy()
        self.assertEqual(self.docker.calls, [])

    def test_missing_migration_cannot_stop_production(self):
        updater.migration_status.side_effect = updater.UpdateError("not migrated")
        with self.assertRaises(updater.UpdateError):
            self.deploy()
        self.assertEqual(self.docker.calls, [])
        self.assertTrue(self.docker.container("gunny-online")["State"]["Running"])

    def test_bad_image_preview_cannot_stop_production(self):
        updater.preview_image.side_effect = updater.UpdateError("bad image")
        with self.assertRaises(updater.UpdateError):
            self.deploy()
        self.assertEqual(self.docker.calls, [])

    def test_update_keeps_ports_secrets_and_a_stopped_backup(self):
        self.deploy()
        new = self.docker.container("gunny-online")
        self.assertEqual(new["Id"], "new-id")
        self.assertTrue(new["State"]["Running"])
        self.assertEqual(updater.environment(new)["GUNNY_ASSETS"], "/app/assets1")
        self.assertEqual(updater.environment(new)["SUPABASE_SECRET_KEY"], "fixture-private-key")
        command = next(call for call in self.docker.calls if call[0] == "create")
        self.assertIn("127.0.0.1:8080:8080/tcp", command)
        self.assertIn("22122:22122/tcp", command)
        old = self.docker.container("old-id")
        self.assertFalse(old["State"]["Running"])
        self.assertEqual(updater.restart_policy(old), "no")
        self.assertEqual(json.loads(self.latest().read_text())["phase"], "healthy")
        self.assertEqual(self.latest().stat().st_mode & 0o777, 0o600)
        self.assertNotIn("fixture-private-key", self.output.getvalue())

    def test_unhealthy_new_container_automatically_restores_old(self):
        updater.wait_ready.side_effect = [updater.UpdateError("unhealthy"), None]
        with self.assertRaises(updater.UpdateError):
            self.deploy()
        old = self.docker.container("gunny-online")
        self.assertEqual(old["Id"], "old-id")
        self.assertTrue(old["State"]["Running"])
        self.assertEqual(updater.restart_policy(old), "always")
        self.assertFalse(self.docker.container("new-id")["State"]["Running"])
        self.assertEqual(updater.restart_policy(self.docker.container("new-id")), "no")

    def test_create_failure_restores_original_name(self):
        self.docker.fail_create = True
        with self.assertRaises(updater.UpdateError):
            self.deploy()
        self.assertEqual(self.docker.container("gunny-online")["Id"], "old-id")
        self.assertTrue(self.docker.container("old-id")["State"]["Running"])

    def test_manual_rollback_is_repeatable(self):
        self.deploy()
        with redirect_stdout(self.output):
            updater.rollback(self.docker, self.latest())
            updater.rollback(self.docker, self.latest())
        self.assertEqual(self.docker.container("gunny-online")["Id"], "old-id")
        self.assertEqual(json.loads(self.latest().read_text())["phase"], "rolled-back")

    def test_rollback_will_not_replace_an_unrelated_container(self):
        self.deploy()
        self.docker.containers["new-id"]["Id"] = "unrelated"
        self.docker.containers["new-id"]["Config"]["Labels"] = {}
        with self.assertRaises(updater.UpdateError):
            updater.rollback(self.docker, self.latest())
        self.assertTrue(self.docker.container("gunny-online")["State"]["Running"])

    def test_compose_containers_are_not_taken_over(self):
        self.docker.containers["old-id"]["Config"]["Labels"] = {"com.docker.compose.project": "existing-stack"}
        with self.assertRaises(updater.UpdateError):
            self.deploy()
        self.assertEqual(self.docker.calls, [])


class MigrationGateChecks(unittest.TestCase):
    def test_gate_checks_pending_credit_and_durable_receipt_schema(self):
        calls = []

        class ReadOnlyOpener:
            def open(self, request, timeout):
                calls.append((request.full_url, request.get_method(), request.data))
                value = {"enabled": False, "offline_days": 90} if request.data else []
                return io.BytesIO(json.dumps(value).encode())

        with patch.object(updater.urllib.request, "build_opener", return_value=ReadOnlyOpener()):
            policy = updater.migration_status(updater.environment(original()))
        self.assertEqual(policy["offline_days"], 90)
        self.assertIn("pending_extension_days", calls[1][0])
        self.assertIn("gunny_offline_extensions", calls[2][0])
        self.assertIn("forge_levels", calls[3][0])
        self.assertIn("gunny780_requests", calls[4][0])
        self.assertIn("gunny780_card_rewards", calls[5][0])
        self.assertIn("dungeon_difficulty", calls[6][0])
        self.assertEqual([call[1] for call in calls], ["POST"] + ["GET"] * 6)
        self.assertEqual(json.loads(calls[0][2]), {"p_op": "retention-policy", "p_data": {}})

    def test_old_draft_without_receipts_is_not_ready(self):
        class OldSchema:
            def open(self, request, timeout):
                if "gunny_offline_extensions" in request.full_url:
                    raise updater.urllib.error.HTTPError(request.full_url, 404, "Missing table", {}, None)
                value = {"enabled": False, "offline_days": 90} if request.data else []
                return io.BytesIO(json.dumps(value).encode())

        with patch.object(updater.urllib.request, "build_opener", return_value=OldSchema()):
            with self.assertRaises(updater.UpdateError):
                updater.migration_status(updater.environment(original()))


class LiveImageGateChecks(unittest.TestCase):
    def verify(self, health, version="7.8.0"):
        def reply(url, **kwargs):
            if url.endswith("/health"):
                return io.BytesIO(json.dumps({"ok": True}).encode())
            self.assertIn("-Online-780.jad", url)
            return io.BytesIO(("MIDlet-Version: " + version + "\nGunny-Server-URL: socket://127.0.0.1:22122\n").encode())
        with patch.object(updater.urllib.request, "urlopen", side_effect=reply), patch.object(updater.socket, "create_connection", return_value=MagicMock()), patch.object(updater, "read_frame", return_value=json.dumps(health).encode()):
            updater.verify_endpoints(("127.0.0.1",8080),("127.0.0.1",22122))

    def test_older_replay_image_is_rejected(self):
        good = dict(tcp=1, protocol=1, events=1, charge=1, actors=1, live=1, growth=1, expansion=1, forge=1, dungeon=1)
        for field in ("actors", "live", "growth", "expansion", "forge", "dungeon"):
            with self.subTest(field=field), self.assertRaises(OSError):
                self.verify({k: v for k, v in good.items() if k != field})

    def test_native_installer_must_match_new_server(self):
        good = dict(tcp=1, protocol=1, events=1, charge=1, actors=1, live=1, growth=1, expansion=1, forge=1, dungeon=1)
        with self.assertRaises(OSError):
            self.verify(good, "7.7.0")
        self.verify(good)


if __name__ == "__main__":
    unittest.main(verbosity=2)
