# Cập nhật Gunny Java 7.1.3

Cập nhật **cả game và server** để thẻ phòng nhận đúng dữ liệu. `/health` cần có `cards:1` (dữ liệu thẻ danh sách phòng) và `items:2`. Không cần SQL mới.

## Thẻ phòng

Thẻ dùng đúng asset PC từ thư viện `a1e3ccec8d2963f1885d.swf`: khung thẻ bitmap 37 (`asset.ddtroomlist.item.bg1`), nhãn Tự do bitmap 34, Khiêu chiến bitmap 33 và ổ khóa bitmap 29. Bản đồ được ghép bên dưới lớp khung trong suốt để giữ mép chéo và vệt mờ gốc. Không vẽ lại thẻ hoặc cắt thẻ từ ảnh tham khảo.

- **Tự do:** phòng tạo thường; các phòng đấu đơn nhanh cũng dùng kiểu Tự do.
- **Khiêu chiến:** phòng Tổ đội 3v3.
- Dòng dưới hiển thị Số phòng. Tên do chủ phòng tự nhập vẫn giữ ở bên trong phòng, không thay các nhãn loại phòng trên thẻ.
- Phần bên phải hiện ảnh ALL – Bản đồ ngẫu nhiên gốc nếu đang chọn Ngẫu nhiên; hoặc ảnh đúng bản đồ đã chọn. Không vẽ thêm một lớp tên map chồng lên ảnh ngẫu nhiên.
- Phòng có mật mã hiện ổ khóa. Phòng đầy hoặc khóa ô chờ không tự được gán biểu tượng mật mã.

## Cách đếm số chỗ

Thẻ phòng thường phản ánh các ô đồng minh bên cạnh chủ phòng, chính là nhóm mà người bấm thẻ sẽ được mời/vào bằng chế độ tự chọn đội:

- Chủ phòng một mình, đóng ô bên cạnh: **1/1**.
- Chủ phòng một mình, mở trống ô bên cạnh: **1/2**.
- Ô bên cạnh có người hoặc AI: **2/2**.

Các ô đối thủ dành cho ghép trận không làm phòng thường hiện sai thành 1/4. Tổ đội đếm tất cả các ô đang mở trong phòng 3v3: ví dụ 1/6; đóng bớt ô thì mẫu số giảm. Người chơi hoặc AI đang đứng trong ô đều được tính đã có chỗ. Người đã rời phòng không được đếm từ ảnh nhân vật cũ.

Các thay đổi mở/đóng ô, người vào/ra, thêm/bớt AI và đổi map được server trả lại khi danh sách tự làm mới. Bộ lọc phân loại theo kiểu phòng thật, không suy đoán Tổ đội chỉ từ số ô.

## Vào phòng

Thẻ đang chờ và còn chỗ có thể mở giao diện nhập mật mã khi cần. Phòng có mật mã không bị chặn nhầm là phòng đầy. Phòng đã bắt đầu hoặc hết ô đồng minh trống vẫn bị chặn trước khi vào; server tiếp tục kiểm tra mật mã và ô trống khi nhận yêu cầu.

Giao thức danh sách cũ vẫn được giữ cho các bản game không gửi header `X-Gunny-Cards: 1`; các thông tin loại phòng, map và số ô chỉ thêm cho bản nhận tính năng này. Luật trận và dữ liệu nhận thưởng không thay đổi.

Đã kiểm tra trên MicroEmulator ở 320×240 và 640×480, qua HTTP localhost: 1/1, 1/2, 2/2 với AI, 1/6 Tổ đội, ảnh map, bộ lọc, mở lại ô chờ, vào phòng có mật mã và gói danh sách tương thích cũ. Chưa thử trên Nokia vật lý hoặc server Render/Supabase thật.
