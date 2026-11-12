#!/usr/bin/env python3
"""Use Gunny's LAN address on this Ubuntu host; preview unless --apply is given."""
import argparse
import ipaddress
import os
from pathlib import Path
import shutil

HOSTNAME = "gunnyjava.xubi.org"


def update(text, address):
    lines = []
    for line in text.splitlines():
        record, mark, comment = line.partition("#")
        fields = record.split()
        if len(fields) >= 2 and HOSTNAME in fields[1:]:
            aliases = [name for name in fields[1:] if name != HOSTNAME]
            if aliases:
                lines.append(fields[0] + "\t" + " ".join(aliases) + (" #" + comment if mark else ""))
            elif mark:
                lines.append("#" + comment)
        else:
            lines.append(line)
    # The managed line is regenerated so repeated runs keep one mapping.
    lines = [line for line in lines if line != "# Gunny LAN: avoid public-IP NAT loopback on this Ubuntu host"]
    while lines and not lines[-1]:
        lines.pop()
    lines += ["", "# Gunny LAN: avoid public-IP NAT loopback on this Ubuntu host",
              f"{address}\t{HOSTNAME}"]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--address", default="192.168.1.28")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    address = ipaddress.ip_address(args.address)
    if address.version != 4 or not address.is_private or address.is_loopback or address.is_unspecified or address.is_link_local or address.is_multicast:
        parser.error("Use the private LAN IPv4 address of the game server")
    if args.apply and os.geteuid() != 0:
        parser.error("Applying requires sudo; run sudo python3 deploy/fix-ubuntu-dns.py --apply")
    targets = [Path("/etc/hosts")]
    # This Ubuntu installation lets cloud-init regenerate /etc/hosts from this template.
    template = Path("/etc/cloud/templates/hosts.debian.tmpl")
    if template.exists():
        targets.append(template)
    changes = []
    for path in targets:
        original = path.read_text()
        revised = update(original, str(address))
        if original != revised:
            changes.append((path, revised))
        print(f"{path}: {'update' if original != revised else 'already configured'} {HOSTNAME} -> {address}")
    if not args.apply:
        print("Preview only. Apply with sudo python3 deploy/fix-ubuntu-dns.py --apply")
        return
    # Back up every target before changing any of them. Keep permissions and inode.
    for path, _ in changes:
        backup = path.with_name(path.name + ".gunny-backup")
        if not backup.exists():
            shutil.copy2(path, backup)
            print(f"Backup: {backup}")
    for path, revised in changes:
        path.write_text(revised)
    print("Applied. The public DNS record and server containers are unchanged.")
    print("Verify: python3 deploy/check-network.py --tcp-host gunnyjava.xubi.org --web-url https://gunnyjava.xubi.org")


if __name__ == "__main__":
    main()
