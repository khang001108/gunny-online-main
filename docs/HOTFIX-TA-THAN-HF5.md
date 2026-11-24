# Gunny 7.9.18 HF5 — Tà Thần

Cumulative patch on HF4. Deploy both server and freshly built client. /health: `tribalHF5: 1`. No SQL change.

## Damage
- Base NPC damage 1080/1380/1800 (easy/normal/hard), 2× HF4. Elite spear no longer divides damage by 3: each spear uses base damage. Bulan projectiles and sword, tribal melee, Sulan golden spear/stomp, red fireballs, explosive totems and burn all use the increased calibration. Pea protection and distance falloff remain. PvP, allied AI and Ant Cave unchanged.
- Red fireball base 1404/1794/2340, red stomp base 2160/2760/3600, burn 180/330/480 per affected turn (before pea protection).

## Command sequence and synchronization
- Room exit/encounter return rebases discarded local queued commands to the last server ACK; updates the local sequence when the ACK advances.
- Sequence gap is an explicit protocol error (code 4, UTF message, int ACK). Server rejects later commands; client discards ambiguous pending input, uses the returned ACK, clears predicted animation and resumes authoritative synchronization without logout. No skipped commands are replayed or blindly applied.
- A stalled local prediction (30 seconds, queue drained, no active playback) requests replay/snapshot synchronization; terminal authoritative frames override stalled local animations after 5 seconds.
- Client-side changes require a new JAR.

## Checks
Java 17 backend compiled. SequenceHF5Check (23), damage (21), HF3 (100), hotfix (885), Sulan opening (148), adds (152), wire bot (150), campaign TCP (118), TribalCheck (87), video (34), waves, Bulan and Sulan7915 passed. Client JAR build and KEmulator playback not executed because Java ME build tools are unavailable.
