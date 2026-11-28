# Đạn và nổ thường / Bpow — E72

Đã áp dụng trực tiếp vào repo, gồm source client/backend, 8 atlas E72, 8 atlas E6, bộ cài JAR/JAD và SHA256. Không triển khai lên máy chủ từ xa.

| Vũ khí | Đạn/nổ thường | Đạn/nổ Bpow |
|---|---:|---:|
| Lựu đạn | 101 | 111 |
| Pháo | 161 | 171 |
| Tủ thuốc | 341 | 351 |
| Lu gạch | 381 | 391 |
| Tủ lạnh | 321, TV | 331, tủ lạnh |
| Phi tiêu | 121 | 131 |
| Giỏ đồ | 401 | 411 |
| Sấm sét | 141 | 151 |

VIP Thương Cổ giữ bộ riêng vì chưa xác minh được cặp Common/Special gốc của vũ khí tùy chỉnh này.

## Cách chạy trên E72

`WeaponCombatArt` đọc atlas được đóng gói sẵn. Đạn dùng 16 hướng, giữ animation điện/kéo, hình cầm khi ném và vệt bay phù hợp thường/Bpow. Nổ dùng 25 mốc chuyển động từ timeline gốc, giữ tỉ lệ giữa nổ thường và Bpow. Không xoay ảnh, tạo ảnh hoặc đọc tài nguyên trong đường vẽ đạn/nổ. Chỉ nạp vũ khí có trong trận lúc Loading và trước khi đổi vũ khí; giải phóng khi rời trận. PNG của E72 có cả chiều rộng và chiều cao không quá 1024.

`Battle.Blast.powered` nhận cờ từ viên đạn. Backend truyền cờ trong bit cao byte bán kính hiệu ứng; client tách cờ và bán kính trước khi phát lại. Độ dài snapshot và phần bán kính thực không đổi. Cờ được đặt lại cả khi tái sử dụng đối tượng snapshot. Đồng hồ hiệu ứng phân biệt nổ thường và Bpow tại cùng tọa độ.

Cần dùng cả backend đã sửa và JAR mới để nhận nổ Bpow khi xem online. Chỉ thay JAR trên máy chủ đang chạy backend cũ chưa đủ cho hiệu ứng nổ của người chơi từ xa. Công thức sát thương, bán kính đào đất, quỹ đạo, nhịp vật lý và thời gian sống của vụ nổ giữ nguyên.

## Kiểm tra

- Client biên dịch CLDC 1.1 / MIDP 2.0 và preverify bằng ProGuard 6.2.2; build cả E72/E6.
- `WeaponCombatCheck`: 407 kiểm tra cờ thường/Bpow, sát thương/bán kính, 16 hướng, vệt bay và giới hạn số vụ nổ.
- `UiWeaponCombatCheck`: 3.541 kiểm tra trên mỗi cấu hình 320×240 và 640×480. E72 chạy với `-Xmx32m`; E6 với `-Xmx128m`.
- Vẽ đủ mọi hướng, frame động, các kiểu lật hình cầm, fade vệt bay và frame nổ cho cả 8 vũ khí. Xác nhận đạn và nổ thường/Bpow khác nhau, không giải mã thêm ảnh khi vẽ, cache được giải phóng.
- Stress render 400 frame với 15 viên đạn và 16 vụ nổ đồng thời; E72 khoảng 0,5 ms/frame trên máy chạy MicroEmulator. Đây là phép đo trên máy tính, không phải FPS của E72 thật.
- Round trip giữa backend và MIDP client: hai vụ nổ thường/Bpow đồng thời, đảo cờ khi tái sử dụng snapshot, đọc hết gói và giữ bán kính chính xác.
- Các kiểm tra `Arsenal717ServerCheck`, `HF42FinishCheck`, `Battle754Check`, `HF45BalanceAdminCheck`, `ShotPacket650Check`, `ChunkPrefetchCheck`, `HF37NetworkCheck`, `TcpGameplayCheck`, `BattlePlaybackPacerCheck`, `BattleNetworkPolicyCheck`, `DeltaReusableCheck` đã qua. `TcpGameplayCheck` có một lần lỗi thời điểm lệnh lặp, lần chạy lại đã qua.
- JAR không lỗi CRC; JAD đúng dung lượng; SHA256 khớp; tài nguyên trong JAR khớp atlas theo từng cấu hình. Dữ liệu E72 giữ ZIP_STORED như định dạng bộ cài hiện tại.

## Kiểm tra cũ đang lỗi

Đã chạy lại trên cả thay đổi mới và bản HEAD chưa sửa, nhận cùng lỗi:

- `verify.ps1` dừng ở `TribalDamageHF4Check`: `Stage-specific projectile damage: 6`.
- `UiPerf7916Check`: đoạn kiểm thử cố tình tạo gói thiếu frame bị từ chối tại `OnlineChunks.decodeSegment`; phần render campaign trước đó chạy được.
- `ShotFramesClientCheck` với `ShotFramesFixture`: EOF khi đọc lobby của fixture cũ.

Không sửa các lỗi ngoài phạm vi này. Kiểm tra online riêng của thay đổi dùng phiên thương lượng hiện tại và snapshot thật từ backend.

Ảnh E72 sau khi áp dụng được lưu trong `client/build/weapon-combat-review/`; nguồn và SHA256 tài nguyên nằm ở `client/source-assets/weapon-combat/manifest.json`.
