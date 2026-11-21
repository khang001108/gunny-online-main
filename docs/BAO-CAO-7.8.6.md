# Gunny 7.8.6 — Túi vật phẩm, quà và tải trận

Đã sửa và build source client/server. Chưa deploy Pi, chưa chạy SQL trên Supabase thật và chưa kiểm tra điện thoại vật lý.

## Bộ nhớ khi vào trận

- Giải phóng ảnh Home, Túi, Shop, Tiệm rèn, popup, tooltip, danh sách/phòng chờ, khung loadout và quà trước khi cấp phát ảnh map và mảng RGB.
- Bỏ giữ atlas Kiến/Nữ Vương, ảnh xoay mũi tên và sprite Tà Thần từ trận cũ khi về sảnh hoặc bắt đầu trận.
- Xóa các lớp trang phục online cũ trước khi tải map mới, thay vì giữ chúng tới sau khi map đã tải xong.
- Không tải atlas bộ đồ nguyên người khi nhân vật online đang dùng các lớp thời trang. Dùng chung atlas trang phục của chính người chơi khi trang phục khớp; E72 dùng chung ảnh đứng và ảnh giới thiệu.
- Không sinh ảnh đối thủ offline không dùng tới trong lúc chuẩn bị trận online.

Đây là sửa các nguồn giữ ảnh và đỉnh bộ nhớ đã xác định từ source. Người dùng cho biết thường văng khi vào nhiều trận nhưng chưa cung cấp lỗi Java đầy đủ. Không xác nhận mọi lần văng là OutOfMemoryError hay khẳng định đã hết lỗi trên E72 thật.

## Đá, bùa và cường hóa

Túi hiện riêng từng loại có số lượng: đá cường hóa Lv1–5, bùa may mắn 25%, bùa bảo vệ. Bỏ ô đá tổng/ảnh stone780 cũ. Đá có nhãn cấp và số lượng; OK mở thông tin vật liệu. Tiệm rèn dùng cùng ảnh, số lượng và cấp đá. Tất cả lấy dữ liệu tài khoản thật khi online, RMS riêng khi offline.

Áo (kind2), mũ (kind3), kính (kind6) tăng **13 thủ/cấp**, tối đa **195 ở +15** cho từng món. Tooltip hiển thị ví dụ `Thủ: 5 (+195)`; chiến đấu server, offline và thống kê dùng cùng ForgeRules. Vũ khí và bộ đồ cũ giữ hệ số trước đó. Không xóa cấp cường hóa người chơi.

## Hòm cấp và túi phó bản

- Một hòm cấp hiện tại bắt đầu Lv1. Chỉ mở khi nhân vật đạt cấp đó; nhận hòm cấp kế tiếp, xu bằng 100 × cấp hòm, đá Lv1 50–100. Hòm Lv70 không sinh hòm tiếp.
- Mỗi người thắng **ải cuối** Huyệt Ma Kiến hoặc Bộ Lạc Tà Thần được 1 túi phó bản, mọi độ khó. Không cấp túi mới cho ải trung gian hoặc thất bại. Kết quả lặp lại không cấp thêm túi.
- Mở túi: đá Lv2 20–70; 10% nhận thêm 1 thẻ VIP 1 ngày. Đây là mức cân bằng mặc định mới, không tuyên bố là tỷ lệ Gunny PC.
- Thẻ VIP là vật phẩm riêng trong Túi, OK dùng để kích hoạt 24 giờ. Thẻ tiếp theo cộng nối từ hạn đang có hoặc thời điểm hiện tại nếu đã hết hạn. Túi hiển thị thời gian VIP còn lại. Chưa thêm đặc quyền chiến đấu/EXP cho VIP vì chưa được yêu cầu.
- OK trên hòm/túi/thẻ dùng trực tiếp, nhận popup khung chung với ảnh vật phẩm và số lượng; OK đóng popup.

Server quyết định thưởng và lưu giao dịch. RPC khóa hồ sơ; biên nhận theo user + request key, kiểm tra loại vật phẩm. Gửi lại cùng yêu cầu trả đúng kết quả cũ. Hai giao dịch đồng thời không dùng một túi hai lần. Trigger cấp túi nằm trong giao dịch ghi kết quả ải cuối; không phát quà hồi tố từ các trận cũ.

## Cập nhật

Chạy **một file** `supabase/UPDATE-7.7.0-TO-7.8.6.sql` cho DB đã có 7.7.0 trở lên, kể cả đã chạy 7.8.5. File gồm tất cả cập nhật trước và phần quà mới; không cần chạy migration lẻ. `GUNNY-ALL.sql` cho cài mới. Chỉ thêm trường/bảng/trigger/RPC, không xóa dữ liệu tài khoản.

Sau SQL, cập nhật backend và public từ gói Pi, build/redeploy đúng Git commit, rồi cài JAR/JAD 7.8.6 trên các máy. Giữ nguyên file môi trường đang dùng. `/health` sẽ có `version: 7.8.6` và `gifts: 1`; admin có header X-Gunny-Version để đối chiếu đúng bản đang chạy. Không tự deploy production trong tác vụ này.

## Build

Server: JDK17+, biên dịch `backend/src/*.java`; JAR sản xuất có sẵn trong gói. Chạy `bash deploy/pi5/run-786.sh` với môi trường hiện có nếu chạy JAR; Docker dùng Dockerfile tại gốc.

Client: `python client/build.py --tools <thư mục ECJ/ProGuard/CLDC/MIDP>` với Java trong PATH; dùng asset từ JAR 7.8.6 có sẵn làm nguồn. Trên máy phát triển còn có `D:/gunny/GunnyNokia/build-786.ps1` và generator crafting_assets.py để xuất asset gốc. JAR được preverify CLDC1.1/MIDP2.0, giữ tên MIDlet/vendor để giữ RMS cũ.

## Kiểm tra

- Suite verify.ps1 qua sau khi cập nhật kiểm tra trang tải sang bộ cài hiện hành và bỏ tham chiếu ảnh test đã nghỉ dùng từ 7.8.5.
- 142 kiểm tra hòm theo cấp, từ chối cấp thấp, túi Lv2, đồng thời 12 yêu cầu cùng khóa, chống thưởng trùng, VIP cộng thời hạn, thưởng ải cuối và công thức thủ 13 × 15.
- 205 kiểm tra expansion, 86 luật Tà Thần, 162 vòng đời TCP tổ đội qua 4 ải, cường hóa xác suất, cheat và admin LAN qua.
- JAR thật trên giả lập 320×240 / 640×480: OK sử dụng vật phẩm từ Túi, parse hồ sơ/thưởng qua TCP, popup, offline RMS tách riêng, hiển thị thủ +195, 8 chu kỳ màn Túi/Home → trận. Kiểm tra cache ảnh màn trước được nhả và atlas own custom được chia sẻ.
- Phát lại khung bắn thực 6/13 đơn vị trên hai độ phân giải, hố đất/nổ/kết thúc lượt đúng; hồi quy hoạt ảnh Kiến và trở về phòng.
- Kiểm tra CRC, JAD size/version, bytecode CLDC và manifest gói Pi. SQL chưa thực thi trên PostgreSQL/Supabase thật; kiểm thử giao dịch dùng CloudStore.Memory, không phải xác nhận vận hành production.

Không có asset placeholder mới: hòm bạc, hộp quà đỏ, thẻ VIP lấy từ Resource/image/unfrightprop (package4, winbox, vipcard), thu về 23px theo scale màn hình.

## Sửa chọn bùa và thông tin Shop

Tiệm rèn có viền vàng cho ô bùa đang trỏ hoặc đã bật, di chuyển từ nút Cường hóa lên Liên tục → Bảo vệ → May mắn → Đá. OK bật/tắt bùa; L/P và thao tác chạm cũng cập nhật ô đang chọn. Mềm trái ở các ô vật liệu xem đúng thông tin. Shop Đạo cụ: mềm trái xem thông tin cả 5 cấp đá, bùa may mắn và bảo vệ, không mở mua hay tiêu hao; OK giữ chức năng mua. ForgeMaterialUiCheck kiểm tra navigation, hai trạng thái bùa, cả 7 trang thông tin và xác nhận mua trên 320×240/640×480. Đã cập nhật lại JAR/JAD và gói Pi 7.8.6; phần này không cần SQL mới.
