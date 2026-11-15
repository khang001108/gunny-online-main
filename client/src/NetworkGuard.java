import javax.microedition.io.Connection;

/** Also bounds Connector.open: some Nokia/emulator stacks block before returning a connection. */
final class NetworkGuard implements Runnable {
    final OnlineClient net;boolean started;
    NetworkGuard(OnlineClient n){net=n;}
    synchronized void start(){if(!started){started=true;new Thread(this).start();}}
    static void closeLater(final Connection connection){if(connection!=null)new Thread(new Runnable(){public void run(){try{connection.close();}catch(Exception ignored){}}}).start();}
    public void run(){while(net.alive){long began=net.transportSince;int limit=net.operation.equals("wake")||net.operation.equals("health")?net.wake.limitMs+2000:net.logged||net.operation.equals("register")?60000:30000;if(net.busy&&began>0&&System.currentTimeMillis()-began>limit)net.abortNetwork(began,"Kết nối quá thời gian chờ. Kiểm tra mạng rồi thử lại.");try{Thread.sleep(200);}catch(Exception ignored){}}}
}
