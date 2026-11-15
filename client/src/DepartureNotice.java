
/** A server-issued choice; the client cannot declare itself the winner. */
final class DepartureNotice {
    final OnlineClient net;int shown,version,alertVersion,room;boolean paused;
    DepartureNotice(OnlineClient n){net=n;}
    void update(int event,boolean waiting,boolean eligible,String name){if(room!=net.roomId){room=net.roomId;shown=0;paused=false;}version=event;if(paused&&!waiting)net.processingSince=0;paused=waiting;if(!waiting||event==shown)return;shown=alertVersion=event;net.repairGround=net.inBattle;synchronized(net){for(int i=0;i<net.queueCount;i++){net.queueKind[i]=7;net.queueArg[i]=0;}}net.chunks.reset();net.replay.reset();net.predicting=false;net.c.held=net.c.fireKey=0;if(eligible)net.c.popup.open("Người chơi rời trận",name+" đã thoát. Tiếp tục với máy hoặc nhận chiến thắng.",new String[]{"Tiếp tục với máy","Nhận chiến thắng"},2);else net.c.popup.notice("Người chơi rời trận",name+" đã thoát. Chờ đối phương chọn tiếp tục hoặc kết thúc.");}

}
