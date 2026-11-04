# Gunny Java 6.5.0 — phát bắn online theo lượt

**Cập nhật cả server và game E72/E6.** Upload toàn bộ gói `gunny-online-main` lên GitHub, đợi Render deploy, kiểm tra `/health` có `"replay":1`, rồi cài game 6.5.0. Không cần SQL mới nếu đã chạy `202611040001_gunny_rewards.sql`.

- Máy người bắn lấy lực, thả đạn, va chạm và phá đất bằng cùng vật lý với offline. Không chờ mạng để vẽ đường đạn hoặc hố nổ.
- Server kiểm tra thao tác và xử lý phát bắn ở nền. Trong lúc xử lý, máy còn lại chờ; giao thức mới không gửi các vị trí đạn dở dang để kéo đạn theo mạng.
- Khi xử lý xong, server gửi trạng thái trước phát bắn cùng kết quả cuối. Máy nhận phát lại cả phát bắn bằng vật lý offline: đạn chạm đất và đất nổ cùng khung hình. Các gói lặp không đặt lại đạn.
- Kết quả HP, hố đất và phần thưởng vẫn do server quyết định. Client đối chiếu trạng thái cuối sau khi hiệu ứng hoàn tất; không được tự cộng thưởng.
- Chỉ sau khi các máy nhận và phát lại xong, server mới mở lượt kế tiếp và bắt đầu đủ thời gian lượt. Nếu quá 30 giây chưa hoàn tất xử lý/nhận dữ liệu, game hiện “Đợi xử lý dữ liệu...”.
- POW, nhiều đạn, x3 và bay dùng cùng luồng hoàn tất phát bắn. Giữ camera, phím Q W E R T / B và sửa đăng nhập từ 6.4.0.

Giao thức được thương lượng bằng `/health` và header `X-Gunny-Replay: 1`. Server giữ đường giao thức cũ cho các bộ cài trước; cần cả hai người dùng bản mới để có phát lại đầy đủ. Không tự deploy hay thay đổi cấu hình Supabase.
