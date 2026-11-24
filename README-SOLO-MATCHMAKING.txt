GUNNY 7.9.18 — SOLO MATCHMAKING

Apply after the earlier lag and Ronin-Lap patches. This patch supersedes the Solo Match Layout Fix. The overlapping client files are based on the previous patches, including the lag optimization and Ronin-Lap UI.

1. Extract into repository root, overwrite files.
2. Commit/push changes and rebuild/redeploy backend on Pi 5 (Docker / Xubi Server Manager), keeping environment and persistent data intact.
3. Run client/Build.bat to rebuild JAR and JAD; reinstall updated client. Update both server and client; cancelquick is a new command.
4. No SQL migration, no server URL or port changes.

Solo queue is hidden from room list; internal room IDs are kept only for existing TCP protocol and battle state. Both solo entrants and explicitly queued strict PvP rooms with exactly one human and no bots are eligible. Match is 1v1, one server-selected random map, 12-second turn. Both players go back to lobby after rewards. No automatic or manual bots in solo; regular room bot votes unchanged. Timeout at 60s stops eligibility, client cancels ticket and returns to lobby. Late cancellation cannot leave a matched battle. A normal room that matches a solo entrant becomes a solo battle, not a permanent room.

Verified: Java 17 backend compile; 30 new real TCP assertions; Matchmaking680Check, WaitBot690Check, WeaponJoinCheck, MatchReturnRegressionCheck, Departure631Check all passed. Parsed all 67 client source files for Java syntax. Full MIDP/CLDC build and emulator visual test not run (toolchain absent).
