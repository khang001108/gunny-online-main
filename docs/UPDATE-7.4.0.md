# Presentation 7.4.0

Room cards use fixed server seat order on both clients, including six-seat team rooms. Shared start/cancel artwork and queue box with elapsed seconds and H bot control. Offline search waits for H to begin a bot game and cancels after 60 seconds. Online bot voting remains authoritative and requires all allied human votes.

Startup asks about audio, shows the fixed transparent Gunny logo against black, then fades the logo out from bottom to top. Settings icon on lobby/room footer, or keyboard soft shortcut, controls persisted background music and visual bullet trails/explosions. Physical projectiles remain visible and collision/damage are unchanged. MP3 tracks are derived from the original Resource sound files, compressed mono for MIDP playback. Menu, hall, six maps, win and loss have separate tracks. Physical-handset audio compatibility must still be tested.

No SQL migration. Update both server and JAR; deployment not performed.
