# Đối chiếu video Tà Thần

Nguồn: bốn video trong `D:/gunny/tà thần phó bản/playertest`, tổng khoảng 8 phút 30 giây. Phân tích khung theo giây, kiểm tra riêng các thời điểm thoại, đòn đánh, chuyển dạng và loading. Dùng gói Resource để dựng lại chuyển động từ timeline gốc.

| Video / mốc | Điều nhìn thấy | Áp dụng trong Java 7.9.7 |
|---|---|---|
| Ải 1, 00:02–00:16 | Thẻ Bộ Lạc Tà Thần; điều kiện hạ 20 địch; nền xanh có trụ đá; lính mặt nạ trắng và chiến sĩ tím bên phải | 20 mục tiêu thật, có tiếp viện; map1122; Living115 và Living116; đội người chơi bên trái |
| Ải 1, 00:37–01:04 | Nhóm lính tiến lại áp sát; thoại hài; đâm giáo | Nhóm lính di chuyển/đâm cùng lượt; không tạo viên đạn khi đánh cận chiến |
| Ải 1, 01:39 / 02:07 | Chiến sĩ ở xa phóng lao qua đường cong | Đạn lao riêng có vị trí phóng, vận tốc, va chạm và sát thương |
| Ải 2, 00:02–00:20 | Thẻ số2 hình dây đậu; rừng, hai cột vật tổ hai bên và sàn giữa; Bulan Em giới thiệu | Thẻ loading lấy từ video; map1124; cả hai Bulan dùng đúng Living117, ở hai bên; đội người chơi trên sàn giữa |
| Ải 2, 00:31 | “Ném một đống lớn!” và vật ném dạng trượng | Câu thoại này; vật ném riêng, không dùng ảnh vũ khí người chơi |
| Ải 2, 00:38–00:40 | “Hãy thử Vật Tổ Mệt Mỏi của ta!”; vật tổ ở mép sàn | Vật Tổ Mệt Mỏi bằng Living127; đặt trên sàn, gây giảm thể lực khi còn sống |
| Ải 2, 01:24 | “Chiến sĩ bộ lạc, hạ gục chúng!” và lính tím tiếp viện | Có hoạt ảnh gọi lính; giới hạn lính gọi thêm để không chồng vô hạn |
| Boss vàng, 00:02–00:24 | Thẻ số3; đền thờ rắn vàng; Sulan vàng giới thiệu | Map1126; thẻ phòng/loading số3; Living121; camera giới thiệu và thoại |
| Boss vàng, 00:43–01:00 | “Vật Tổ HP, hãy cho ta sức mạnh.”; hai vật tổ xanh hai bên boss | Living124, tối đa hai vật tổ HP, hồi máu cho Sulan; phá vật tổ sẽ ngừng tác dụng |
| Boss đỏ, 00:48–00:52 | Sulan vàng hết HP, biến mất rồi Sulan đỏ xuất hiện, máu đầy | Chuyển dạng trong cùng trận; dùng Living122; dừng vật lý/nhập trong diễn cảnh; giữ bản đồ và đội người chơi |
| Boss đỏ, 00:55–01:00 | Mượn thân thể; xuất hiện Đậu Hà Lan | Hành động riêng và Living123; không dùng hình bia tập cũ |
| Boss đỏ, 01:24 | Ba cầu lửa, chuyển động tấn công riêng | Ba đạn lửa thật; tách đòn đánh gần và đòn phóng lửa |
| Boss đỏ, 01:57–02:15 | Đậu Hà Lan nói, hiệu ứng xanh Phủ Sức Mạnh, Sulan hết HP và kết thúc | Cửa sổ tăng sức mạnh, hoạt ảnh chết trước kết quả; hạ dạng đỏ kết thúc ải |

Ải công khai chạy theo thứ tự1 → 2 → 3. ID lưu/server3 → 4 → 6 được giữ để hệ thống tiếp ải và thưởng hiện có tiếp tục làm việc; ID5 cũ là nhánh tương thích của Bulan.

## Hình và chuyển động

- Bản đồ lấy cả nền và đất có alpha từ Resource. Bỏ dải đất/nền cứng dựng tạm. PNG hai kích thước và mask server có cùng va chạm tại mọi điểm480×264.
- Living115/116: lính trắng/chiến sĩ tím; Living117: cả hai Bulan; living121/122: hai dạng Sulan; living123: Đậu Hà Lan; living124/125/126/127: HP, Xiềng Xích, Nổ, Mệt Mỏi.
- Tám mẫu thời gian cho mỗi hàng tư thế: đứng, đi, đánh, gọi phép, kỹ năng, trúng đòn, chết, xuất hiện. Các lớp của một nhân vật giữ chung tỷ lệ và điểm neo; chuyển động lấy từ timeline, không xoay một hình đứng để giả hoạt ảnh.
- Vật ném: lao, trượng Bulan, lửa và trượng Sulan. Trượng Sulan dùng bitmap81 tương ứng với tư thế beatA; tượng đá của phép triệu hồi không bị dùng nhầm thành viên đạn.
- Nền loading ghép phần minh họa nhân vật xanh/rồng đỏ từ video vào hai panel màu; hình người chơi, VS, thẻ ải và phần trăm được vẽ động. Đây là bản bố trí lại cho màn Nokia4:3, không phải ảnh chụp nguyên màn hình làm game.
- Nguồn, hash SWF, nhãn và số frame: `client/source-assets/tribal797/manifest.json`. Dùng [JPEXS](https://github.com/jindrapetrik/jpexs-decompiler) để dựng đầy đủ các lớp Flash.

## Phần cần phân biệt với bản Mobi nguyên gốc

Video cho thấy bố cục và hành động nhưng không cung cấp bảng dữ liệu máy chủ. HP, sát thương, phòng thủ, số lượt hồi sinh, chu kỳ gọi vật tổ và thời gian cửa sổ Đậu Hà Lan đang là luật cân chỉnh Java ME. Tọa độ spawn được đặt theo bố cục video và quy đổi về thế giới480×264, không khẳng định là tọa độ máy chủ Mobi.

Các câu nhìn rõ đã chép theo khung video. Một phần câu của Sulan/Đậu Hà Lan ở00:20,01:24,01:57,02:11 của clip boss đỏ bị tên/gió/thông báo che; những câu này hiện dùng bản diễn ý, chưa xác nhận được từng chữ. Phần hồi sinh Bulan, khóa góc của Xiềng Xích và đếm hai lượt của vật tổ nổ tiếp nối luật phó bản trước đó; các video chưa xác nhận đủ chi tiết để coi đó là thông số gốc.
