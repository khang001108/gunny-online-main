# 7.9.18 HF13 - Room loadout
Incremental patch on top of HF12. Extract into repository root. Build both clients and rebuild backend. No SQL migration.
- Room-level navigation focuses entire equipped and available groups. OK enters; directions select item; left softkey shows native item tooltip; right softkey exits/cancels.
- Add to first empty slot. If full, choose a slot to replace. Equipped group OK removes item. Duplicate equipment disallowed.
- TCP supports empty slot ID 6 and advertises loadoutHF13=1; empty slots cannot be used or consume energy. Older peers receive compatible snapshot encoding but should update to see exact loadouts.
- Shared room panel applies to custom PvP, team, dungeon; quick queue has compact panel before loading. Queue can still start automatically.
- Backend Java compilation, LoadoutHF13Check (980), Items710ServerCheck and TribalTcpCheck (137) passed. Java ME compile and real emulator rendering not performed.
