# Gunny Java 7.9.2

## Đã đổi

- VIP tối đa9. Chi phí nâng cấp: 200/600/700/800/1000/1200/1400/1600/2000 (tổng9.500). Mốc đầu0/200 theo yêu cầu; giữ các mốc cũ tiếp theo đến VIP9. Dùng đúng icon gốc1–9. Bỏ slider, EXP đặt dưới icon, tên nhân vật căn giữa Túi.
- VIP1–3 vòng mẫu1; VIP4–6 mẫu2; VIP7–9 mẫu3. Chỉ một vòng, giải phóng cache vòng bậc cũ, không chồng vòng và bỏ sao chớp. Atlas16 khung xoay từ cùng bitmap để không đổi hình giữa các khung. Bpow giảm216 xuống144, nằm giữa màn. Trên map vẫn chỉ có cánh; vòng giữ ở ảnh lượt và Bpow.
- Trong giao tranh dùng hai nửa cánh cùng một phía lưng, chia lớp trong trước vũ khí/body và lớp ngoài sau vũ khí. Đóng/mở4 khung, phản chiếu theo hướng quay, nghiêng theo5 mức dốc từ-30 đến30. Ảnh cache theo loại/hướng dốc/lớp; không xoay/tạo ảnh lớn mỗi khung vẽ. Đã bỏ đường vẽ cánh phó bản cũ gây trùng.
- Thông tin bạn bè: các ô đồ, nhân vật, icon/EXP VIP, cấp/EXP nhân vật, Công/Thủ/May, ô cánh và thanh HP/TL theo tọa độ bên trái của Túi (dịch sang bảng thông tin). Ô cánh chọn được bằng điều hướng hoặc chạm để xem thông tin. Chỉ xem, không mặc/tháo đồ của bạn.

## Dữ liệu và cập nhật

Cần chạy riêng `supabase/migrations/202611220002_vip9.sql` trước khi cập nhật backend/JAR. Chỉ thay hàm giao dịch VIP; giữ nguyên EXP/thẻ/tiền/cánh đang lưu, kể cả EXP cũ trên9.500 (hiển thị VIP9 MAX). Full VIP từ nay đạtVIP9. Không sửa env/secret và không deploy production trong phiên này.

Thẻ cũ10–12 giữ lại như thẻ EXP+1000/+1100/+1200, không tạo cấpVIP10–12. Full VIP giữ vị trí dữ liệu cũ để không mất vật phẩm. Yêu cầu mua cánh Huyền Thoại đổiVIP12→VIP9, cánh đã mua vẫn giữ.

SQL đã gộp vào GUNNY-ALL.sql/UPDATE-CURRENT.sql cho cài mới hoặc nâng từ bản cũ; người đã chạy SQL VIP chỉ cần file202611220002. Chưa chạy migration trên PostgreSQL/Supabase thật.

## Build/kiểm tra

- Client CLDC/MIDP/preverify và backend Java17: qua.
- Vip790Check được cập nhật9 cấp:62 kiểm tra qua (ngưỡng, EXP dư, Full VIP, mua/đeo/HP và chống cấp/dùng trùng).
- Fix791Check, CardsPresence790Check21 ca, VipTcp790Check20 ca: qua.
- Ui790Check cập nhật9 cấp, Túi/Shop/bạn bè, vòng/Bpow/cánh,6 vòng vào trận/quay lại và khung dọc: qua320×240 heap32MB và640×480 heap128MB trên MicroEmulator.
- Chưa kiểm thử điện thoại Nokia thật hoặc đủ mọi tư thế bắn trên hai máy vật lý. Cánh dùng khung bitmap đóng/mở và nghiêng theo dốc, chưa phải toàn bộ timeline Flash gốc. Cần thử thêm tư thế thực tế trước khi khẳng định khớp tuyệt đối.
- Hash/size bộ cài ở BUILD-7.9.2.json. Build bằng `python client/build.py --tools D:/gunny/GunnyNokia/tools`; kiểm tra backend bằng verify.ps1/verify.sh.

File chính: VipRules.java cả hai phía; VipSystem.java, PhoneCanvas.java, BagScreen.java, OnlineSocial.java; bộ atlas vòng16 khung và wing792-*; migration VIP9; Admin, metadata và bộ cài792. Source asset giữ nguyên nguồn circlelight và wing đã ghi trong client/VIP-ASSETS.json.
