/** Shared release contract. No extra bytes or work in the battle simulation. */
final class GameRelease {
    static final String VERSION="7.9.19";
    static final int EPOCH=43, LEGACY_MAGIC=0x474e5954, WIRE_MAGIC=0x474e5933;
    static final String UPDATE="Phiên bản game đã cũ. Vui lòng cập nhật Gunny Java 7.9.19 (HF43) tại trang tải game của máy chủ trước khi đăng nhập.";
    static int magic(int kind){return kind==0?LEGACY_MAGIC:WIRE_MAGIC;}
    static boolean accepts(int magic){return magic==WIRE_MAGIC;}
    static String healthError(String content){
        if(content.indexOf("\"ok\":true")<0||content.indexOf("\"protocol\":1")<0)return "Địa chỉ không phải máy chủ Gunny tương thích.";
        if(content.indexOf("\"wireEpoch\":"+EPOCH+",")<0||content.indexOf("\"version\":\""+VERSION+"\"")<0)return "Phiên bản game và máy chủ không khớp. Hãy cập nhật game từ trang tải của máy chủ (bản trên máy: "+VERSION+"). Nếu máy chủ chưa nâng cấp, hãy liên hệ quản trị viên.";
        return null;
    }
}
