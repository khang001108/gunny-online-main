#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
mkdir -p backend/build
javac --release 17 -encoding UTF-8 -d backend/build backend/src/*.java backend/tests/*.java
for check in Loading796Check Ranking794Check Conversion793Check Features793Check Fix791Check Vip790Check VipTcp790Check CardsPresence790Check Update788Check Gift786Check AdminLanCheck Online785Check TribalCheck TribalVideo797Check TribalTcpCheck CraftingChanceCheck WeaponJoinCheck PlayerUiCheck ChunkPrefetchCheck SupportPageCheck MatchReturnRegressionCheck ReturnRoomCheck Expansion780Check Gameplay771Check Charge770Check Tcp768Check TcpGameplayCheck Tcp770Check VirtualAuth770Check CloudStoreCheck AuthContract630Check ServerCheck RoomSettingsCheck Protocol630Check Admin750Check RetentionAdmin770Check; do
  java -Djava.awt.headless=true -cp backend/build "$check"
done
