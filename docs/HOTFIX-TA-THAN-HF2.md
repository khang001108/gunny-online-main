# Tà Thần 7.9.18 HF2 (tích lũy HF1)

- Backend: danh tính và trạng thái ô AI dựa trực tiếp vào selectedBots; mời AI không cần Session người chơi. Tạo đơn vị sống, có HP/thể lực/trang bị, không gắn quyền điều khiển người chơi. Ô trống có HP/maxHP bằng 0.
- Lobby TCP: xuất tên bot chính xác từ ID đã mời, present=true, bot=true; không giả mạo tên ở client.
- Client: vẽ khung xem trước phần thưởng trước RoomLoadout (toolbar và tooltip), không che tooltip.
- Health HTTP/TCP: trường `tribalBotFix: 2`; log khởi động `Gunny 7.9.18 HF2 (tribalBotFix=2)`.
- Kiểm thử: TribalBotWireCheck 150 kiểm tra qua TCP (mời hai bot, tên, HP, vị trí, loading, AI thực hiện hoặc kết thúc lượt ở cả 3 ải); TribalHotfix7918Check 885; TribalTcpCheck 118; DungeonRetreat7918Check pass.

## Cài đặt
Giải nén đè thư mục gốc repository, chạy client/Build.bat khi đã đóng KEmulator, push lên Git, rebuild image backend từ commit mới và tạo lại container. Kiểm tra /health có `"tribalBotFix":2`. Tạo phòng mới rồi mời 2 AI. Không cần thay SQL. Chưa kiểm thử render trực tiếp KEmulator hoặc build Java ME trong môi trường này.
