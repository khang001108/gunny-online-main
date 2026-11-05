# Long polling 7.3.0

Server advertises longpoll:1. New JAR opens a separate binary POST /wait connection carrying magic, room ID and opaque state cursor. It waits up to 12 seconds and returns a new cursor when state changes. This is a wake hint; authoritative state still comes from /api sync and existing chunks, sequence/ack and terrain revision remain intact. Movement/commands and local render-completion acknowledgments bypass the idle wait. A 10-second sync heartbeat remains.

Wait handlers use a separate bounded executor (32 connections), one outstanding wait per authenticated session. They release room locks before sleeping. Overload/errors fall back to existing polling after three wait failures. Old JARs continue using /api without /wait. No new database migration.

This reduces empty idle requests; active animations may still generate frequent updates. No performance or two-connection compatibility claim for physical Nokia hardware until handset tests. Update server and matching JAR; deployment has not been performed.
