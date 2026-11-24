# Gunny 7.9.18 – Tà Thần HF4

Cumulative patch: contains HF1, HF2 and HF3, with the HF3 wave-skip behavior replaced.

- Stage 1: refill before choosing the enemy actor. After the last living party member's turn, a wiped enemy wave gets its normal melee group turn, followed by the warrior's three-spears turn. This restores ordinary active-actor camera tracking. Six ordinary troops, one warrior, and 20 troop kills remain unchanged.
- All three Tribal encounters: enemy base damage 180/230/300 becomes 540/690/900. The same base applies to spears, melee, Bulan, bomb and Sulan skills. Sulan's separate burn and pea-targeted fury damage also triple. Existing modifiers, falloff, pea protection, player damage, Ant Cave and PvP remain unchanged.
- Expedition waiting-room exit: stage 2 of Ant Cave or later Tribal stages ask whether to leave and lose campaign progress. Cancel leaves the room untouched. Confirm uses the existing leave operation; successful response always opens the expedition list even if the previous browser or match-over flag is stale. First-stage exit does not need a progress warning. Battle retreat continues to use dungeonretreat.
- Backend health: `tribalBotFix: 2`, `tribalHF3: 1`, `tribalHF4: 1`.

Validation: Java 17 backend compilation and targeted TribalDamageHF4Check, TribalHF3Check, TribalWavesCheck, Sulan7912Check, Sulan7915Check, TribalCheck, TribalBotWireCheck, TribalHotfix7918Check, TribalTcpCheck, SulanAdds7913Check, BulanSequenceCheck and DungeonRetreat7918Check passed. Added MIDP room-exit assertions to UiRetreat7918Check; client UI/JAR has not been executed in this environment (MIDP build toolchain unavailable).

Install: extract into repository root, rebuild client JAR and server container, deploy both. No SQL changes.
