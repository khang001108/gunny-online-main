GUNNY JAVA ONLINE 7.9.18 - PATCH TOI UU RAM / FPS
=================================================
Nguon: gunny-online-main-main(4).zip (ban repo nguoi dung gui).
Chi chua file thay doi, duong dan tu thu muc goc repo. Khong thay asset, JAR/JAD,
backend, SQL, dia chi may chu, giao thuc TCP, vat ly, toc do tick 30 Hz, quy tac game.

CACH CAP NHAT
1. Sao luu repo hoac commit code hien tai.
2. Giai nen ZIP vao GOC repo (noi co client/, backend/, public/), dong y ghi de.
3. Dong KEmulator / FreeJ2ME neu dang mo JAR.
4. Chay client\Build.bat tren Windows (script build dang co cua repo).
   Hoac: python client/build.py --tools D:/gunny/GunnyNokia/tools
   Can ecj.jar, proguard.jar, cldcapi11.jar, midpapi20.jar.
5. Lay 2 JAR/JAD moi tu public/downloads/ va cai lai tren may thu.
   Neu phat hanh qua web, commit/push ca JAR/JAD sau khi build thanh cong.
   Khong can doi backend hoac Supabase cho patch nay.

CACH SUA
- PhoneCanvas: gom giai phong terrain, pixel/scratch, tile, NPC, loading, POW,
  cache xoay/nhan vat mo va VIP khi roi tran; khong giu cac cache tran cu o phong cho.
  Tai su dung buffer minimap va dial; cache tach dong toast; giam chuoi tam.
- VipSystem: bo cache atlas canh theo goc khong con nhan vat song su dung;
  giai phong anh tinh chi dung luc loading. Khong giam kich thuoc hay so frame canh.
- OnlineClient / OnlineResult: goi cleanup khi roi tran, ve phong, logout, mat
  ket noi, loi room; chi doi key chan dung khi du lieu doi; khong tao preview
  loading thu hai khong duoc drawVersus su dung.
- SnapshotDelta / OnlineChunks: dung lai mang byte trong segment da validate;
  bo reference buffer sau khi phat xong. API read() cu van tra ban sao doc lap.
- RoomSpeech / LobbyChat / TribalArt: cache ngat dong chat, thoai, bo chuoi tam
  trong luc render; xoa message cu ngoai pham vi historyCount.

KIEM TRA DA CHAY
- Java syntax parse 67 file client/src va 36 file client/tests: PASS.
- Delta785Check: PASS 1002 ca round-trip/validation.
- DeltaReusableCheck: PASS 2000 frame, 20 lan cap phat mang decoder (fixture
  co doi size moi 100 frame). Khong phai ket qua FPS tren Nokia.
- Kiem tra ZIP va file thay the.
- Chua build CLDC/preverify hay chay emulator/Nokia vi repo khong kem cac
  thu vien build va MicroEmulator. ReturnRoomClientCheck da them assert
  cleanup terrain/nhan vat/canh nhung chua chay duoc o moi truong nay.

TEST SAU KHI BUILD
- Bat FPS trong Cai dat. Test 1v1, 3v3 va Pho ban Tà Than/Sulan.
- Cho nhieu VIP/canh khac nhau, di chuyen qua doc, dung POW, chat co dau.
- Choi 20-30 phut, vao/roi phong 10 lan. So sanh FPS ban dau va cuoi,
  kiem tra loi OutOfMemoryError, do tre khi vao phong va luot ke tiep.
- Test logout/mat mang/ket noi lai va nhan thuong, quay lai phong.
