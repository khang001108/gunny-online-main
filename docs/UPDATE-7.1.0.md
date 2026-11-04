# Cập nhật Gunny Java 7.1.0

Cài game mới trên tất cả máy và cập nhật toàn bộ gói server. Không có SQL mới; giữ các migration và dữ liệu đã chạy. `/health` cần có `items:2`.

## Bố cục và điều khiển

Buff Q/W/E/R/T đứng dọc bên phải như trước. Ba đạo cụ Z/X/C nằm ngang bên phải, phía trên thanh lực. F đổi vũ khí, B bật POW, V bỏ lượt; phím mềm trái bật/tắt xem camera. Khi chuyển lượt, camera kéo dần đến nhân vật đang tới lượt. Camera vẫn theo đạn khi đang bay.

Phòng có sáu đạo cụ để chọn vào ba ô Z/X/C. Mỗi đạo cụ đã chọn **chỉ được dùng một lần trong cả trận**, không tự hồi lại theo lượt. Trận mới có lại. Đổi bộ ba chỉ được thực hiện trước trận và sẽ hủy sẵn sàng/tìm trận. Chọn trùng sẽ đổi chỗ, không nhân đôi đạo cụ.

## Đạo cụ

| Đạo cụ | Thể lực | Hiệu lực |
| --- | ---: | --- |
| Tàng hình | 50 | Bật ngay, kéo dài hai lượt của người được áp dụng, tính cả lượt đang dùng. Bản thân và đồng đội thấy nhân vật mờ; đối thủ không thấy hình, tên hoặc chấm nhỏ trên bản đồ. |
| Đạn băng | 150 | Cầm và lấy đà ném một viên băng riêng, thay đạn vũ khí. Trúng mục tiêu thì khóa hai lượt; mục tiêu nằm trong tảng băng. Băng không gây sát thương hoặc phá đất. Khi bị tấn công tiếp, băng vỡ và sát thương của đòn phá băng giảm một nửa. |
| Hồi máu nhóm | 170 | Hồi tối đa 300 HP cho từng đồng minh còn sống, bao gồm bản thân. |
| Tàng hình nhóm | 150 | Tàng hình hai lượt của từng đồng minh còn sống; cách hiển thị giống tàng hình cá nhân. |
| Dẫn đường | 120 | Trong lượt đang dùng, đạn vũ khí tự uốn hướng khi tới gần đối thủ (phạm vi 100 pixel của thế giới game). Giảm 50% sát thương vũ khí. Đạn vẫn va chạm và nổ vào đất. Không phải đường dự đoán ngắm bắn. |
| Kích nộ | 120 | Tăng 50 điểm nộ, tối đa 100. Khi đủ 100 vẫn phải nhấn B để dùng POW. |

Không đủ thể lực hoặc hiệu ứng không thể áp dụng thì không tiêu đạo cụ. Đạo cụ không dùng được trong lúc lấy lực hoặc có đạn đang bay. Đạn băng không ghép cùng buff tăng sát thương, nhiều đợt/nhiều tia, máy bay, dẫn đường hay POW.

Mức thể lực, hồi 300 HP nhóm, giảm 50% sát thương dẫn đường và tăng 50 nộ lấy từ `Tank.Request/TemplateAlllist.xml` của source gốc; thời gian tàng hình lấy hai lượt theo yêu cầu mới, thay cho ba lượt trong ảnh minh họa.

## Buff sẵn có

- Q: thêm hai đợt bắn, giữ luật và thể lực cũ.
- W: ba tia đạn, giữ luật và thể lực cũ.
- E: thêm 50% sát thương, giữ luật và thể lực cũ.
- R: hồi 500 HP, tốn 150 thể lực, tối đa **5 lần trong một trận**.
- T: máy bay, tốn 150 thể lực, hồi chiêu hai lượt kế tiếp của chính người dùng. Dùng lại từ lượt thứ ba sau đó. Góc bay từ 0 đến 90 độ, không bị giới hạn theo vũ khí đang cầm.
- B: POW vẫn tiêu hết nộ khi bật, giữ cách dùng cũ.

## Asset và đồng bộ

Viên băng từ `Resource/image/bomb/blastout/blastout1.swf`, symbol `bullet1` (76). Tảng băng từ SWF PC `79831fd6d794240bdc5c.swf`, symbol `asset.gameFrostEffectAsset` (1383); hiệu ứng va chạm băng từ `asset.game.propanimation.freeze` (1223). Động tác lấy đà/ném dùng bộ khung nhân vật gốc. Icon nhóm hồi máu, nhóm tàng hình, dẫn đường và kích nộ dùng icon gốc, không cắt từ ảnh tham khảo.

Game và server cùng dùng `Battle.java`. Server kiểm tra thể lực, đạo cụ đã dùng, hồi chiêu máy bay, giới hạn hồi máu, va chạm, đóng/vỡ băng, tàng hình và dẫn đường. Các trạng thái này nằm trong đoạn phát lại đã xử lý; lượt sau vẫn đợi các máy xem xong. Bản 7.0.0 cần nâng lên 7.1.0 để tránh nhầm sáu loại đạo cụ đã đổi.

Đã thử luật game và giao thức localhost, kiểm tra ảnh E72/E6 trên MicroEmulator. Chưa thử trên Nokia vật lý hoặc Render/Supabase thật. Phần Quan chiến đã được thay bằng khung chọn bản đồ.

## Khung phòng và popup Mời

Khung bên phải dùng ba ô Trong trận, danh sách sáu đạo cụ và khung bản đồ ở dưới thay Quan chiến. Bấm khung bản đồ hoặc chọn bằng phím hướng rồi OK để mở chọn map. Chỉ chủ phòng đổi bản đồ Online trước trận. Chọn Ngẫu nhiên vẫn hiện Ngẫu nhiên dù server đã chuẩn bị một bản đồ cụ thể.

Ảnh ngẫu nhiên tiếng Việt “ALL – Bản đồ ngẫu nhiên” là ảnh game gốc `image/map/0/samll_map.png` (295×107) lấy từ cache tài nguyên PC, không cắt ảnh tham khảo. Khung có chữ Thông tin phòng, biểu tượng và góc bánh răng gốc; số giây hiển thị đúng thiết lập hiện tại.

Mời là popup nổi trên nền phòng. Ba tab là **Liên hệ / Bạn bè / Phòng**; Liên hệ chứa tám AI. Mỗi dòng có hình người xanh gốc `asset.core.sex_M`, tên và nút Mời xanh gốc `asset.core.littleGreenBtn`. Không hiện cấp, Guild hoặc nút mời miễn phí. Giữ nút Danh sách tạo mới, phân trang và điều hướng bằng bốn phím. Offline dùng cùng popup để chọn AI; Online mời người chơi thật theo danh sách máy chủ. Quản lý bạn bè và yêu cầu kết bạn vẫn nằm ở cửa sổ Bạn bè riêng.
