import java.io.*;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.*;
import org.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import org.bouncycastle.crypto.tls.*;
import gunny.tls.compat.security.SecureRandom;

/** TLS 1.2 over MIDP sockets; never delegates HTTPS to the old handset TLS stack. */
final class EmbeddedTls implements HttpConnection {
    final String url,host,file;final int port;
    String method=GET,message="";int response=-1;boolean sent,closed;
    Hashtable requestHeaders=new Hashtable(),headers=new Hashtable();Vector headerKeys=new Vector();
    ByteArrayOutputStream requestBody=new ByteArrayOutputStream();byte[] responseBody;
    volatile SocketConnection socket;volatile TlsClientProtocol tls;
    private static SecureRandom random;
    EmbeddedTls(String address)throws IOException{
        if(!address.startsWith("https://"))throw new IOException("TLS yêu cầu HTTPS.");
        url=address;int slash=address.indexOf('/',8);String authority=slash<0?address.substring(8):address.substring(8,slash);
        int colon=authority.indexOf(':');host=(colon<0?authority:authority.substring(0,colon)).toLowerCase();
        try{port=colon<0?443:Integer.parseInt(authority.substring(colon+1));}catch(NumberFormatException e){throw new IOException("Cổng máy chủ không hợp lệ.");}
        if(host.length()==0||port<1||port>65535||authority.indexOf('@')>=0)throw new IOException("Địa chỉ máy chủ không hợp lệ.");
        for(int i=0;i<host.length();i++){char ch=host.charAt(i);if(!(ch>='a'&&ch<='z'||ch>='0'&&ch<='9'||ch=='-'||ch=='.'))throw new IOException("Tên máy chủ không hợp lệ.");}
        file=slash<0?"/":address.substring(slash);if(file.indexOf('\r')>=0||file.indexOf('\n')>=0||file.indexOf('#')>=0||file.indexOf(' ')>=0)throw new IOException("Đường dẫn máy chủ không hợp lệ.");
    }
    static HttpConnection open(String address)throws IOException{
        if(address.startsWith("https://"))return new EmbeddedTls(address);
        // Only the existing client endpoint validator allows loopback HTTP tests.
        if(address.startsWith("http://127.0.0.1:")||address.startsWith("http://localhost:"))return (HttpConnection)Connector.open(address,Connector.READ_WRITE,true);
        throw new IOException("Game chỉ gửi tài khoản qua HTTPS.");
    }
    static synchronized SecureRandom rng(){
        if(random==null){SecureRandom seeded=SecureRandom.getInstance("SHA256PRNG");seeded.setSeed(new ThreadedSeedGenerator().generateSeed(32,false));random=seeded;}
        return random;
    }
    void send()throws IOException{
        if(closed)throw new IOException("Kết nối đã đóng.");if(sent)return;sent=true;
        try{
            socket=(SocketConnection)Connector.open("socket://"+host+":"+port,Connector.READ_WRITE,true);
            if(closed)throw new IOException("Kết nối đã hủy.");
            tls=new TlsClientProtocol(socket.openInputStream(),socket.openOutputStream(),rng());
            tls.connect(new AbstractTlsClient(null){
                public ProtocolVersion getMinimumVersion(){return ProtocolVersion.TLSv12;}
                public ProtocolVersion getClientVersion(){return ProtocolVersion.TLSv12;}
                public int[] getCipherSuites(){return new int[]{CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256};}
                public TlsKeyExchange getKeyExchange(){return new TlsECDHEKeyExchange(KeyExchangeAlgorithm.ECDHE_RSA,supportedSignatureAlgorithms,namedCurves,clientECPointFormats,serverECPointFormats);}
                public TlsCompression getCompression(){return new TlsNullCompression();}
                public TlsCipher getCipher()throws IOException{return new TlsAEADCipher(context,new org.bouncycastle.crypto.modes.GCMBlockCipher(new org.bouncycastle.crypto.engines.AESEngine()),new org.bouncycastle.crypto.modes.GCMBlockCipher(new org.bouncycastle.crypto.engines.AESEngine()),16,16);}
                public Hashtable getClientExtensions()throws IOException{
                    Hashtable extensions=TlsExtensionsUtils.ensureExtensionsInitialised(super.getClientExtensions());Vector names=new Vector();names.addElement(new ServerName(NameType.host_name,host));TlsExtensionsUtils.addServerNameExtension(extensions,new ServerNameList(names));return extensions;
                }
                public TlsAuthentication getAuthentication(){return new TlsAuthentication(){
                    public void notifyServerCertificate(Certificate certificate)throws IOException{TlsTrust.verify(certificate,host);}
                    public TlsCredentials getClientCredentials(CertificateRequest request){return null;}
                };}
            });
            if(closed)throw new IOException("Kết nối đã hủy.");
            OutputStream output=tls.getOutputStream();byte[] body=requestBody.toByteArray();
            String head=method+" "+file+" HTTP/1.1\r\nHost: "+host+(port==443?"":":"+port)+"\r\nConnection: close\r\nAccept-Encoding: identity\r\n";
            java.util.Enumeration keys=requestHeaders.keys();while(keys.hasMoreElements()){String key=(String)keys.nextElement();if(!key.equals("content-length"))head+=key+": "+requestHeaders.get(key)+"\r\n";}
            if(method.equals(POST))head+="Content-Length: "+body.length+"\r\n";
            output.write((head+"\r\n").getBytes("UTF-8"));if(body.length>0)output.write(body);output.flush();
            InputStream input=tls.getInputStream();String status=line(input,4096);int first=status.indexOf(' '),second=status.indexOf(' ',first+1);
            if(!status.startsWith("HTTP/1.")||first<0)throw new IOException("Phản hồi HTTP không đúng.");
            try{response=Integer.parseInt(status.substring(first+1,second<0?status.length():second));}catch(Exception e){throw new IOException("Mã HTTP không đúng.");}
            message=second<0?"":status.substring(second+1);int size=0;
            while(true){String header=line(input,4096);size+=header.length();if(size>16384)throw new IOException("HTTP header quá lớn.");if(header.length()==0)break;int split=header.indexOf(':');if(split<=0)throw new IOException("HTTP header không đúng.");String key=header.substring(0,split).toLowerCase(),value=header.substring(split+1).trim();if(headers.containsKey(key)&&(key.equals("content-length")||key.equals("transfer-encoding")))throw new IOException("HTTP framing bị lặp.");headers.put(key,value);headerKeys.addElement(key);}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();String transfer=(String)headers.get("transfer-encoding"),length=(String)headers.get("content-length");
            if(transfer!=null){if(length!=null||!transfer.toLowerCase().equals("chunked"))throw new IOException("HTTP transfer không hỗ trợ.");
                while(true){String chunk=line(input,128);int semi=chunk.indexOf(';');if(semi>=0)chunk=chunk.substring(0,semi);int count;
                    try{count=Integer.parseInt(chunk.trim(),16);}catch(Exception e){throw new IOException("HTTP chunk không đúng.");}
                    if(count<0||count>131072-bytes.size())throw new IOException("Phản hồi quá lớn.");if(count==0){int trailer=0;while(true){String tail=line(input,4096);trailer+=tail.length();if(trailer>8192)throw new IOException("HTTP trailer quá lớn.");if(tail.length()==0)break;}break;}
                    copy(input,bytes,count);if(line(input,2).length()!=0)throw new IOException("HTTP chunk thiếu dấu kết thúc.");}
            }else if(length!=null){int count;try{count=Integer.parseInt(length);}catch(Exception e){throw new IOException("HTTP length không đúng.");}if(count<0||count>131072)throw new IOException("Phản hồi quá lớn.");copy(input,bytes,count);}
            else{byte[] buffer=new byte[2048];int n;while((n=input.read(buffer))!=-1){if(bytes.size()+n>131072)throw new IOException("Phản hồi quá lớn.");bytes.write(buffer,0,n);}}
            responseBody=bytes.toByteArray();
        }finally{release();}
    }
    static void copy(InputStream input,ByteArrayOutputStream output,int count)throws IOException{byte[] buffer=new byte[2048];while(count>0){int n=input.read(buffer,0,Math.min(count,buffer.length));if(n<0)throw new EOFException("Phản hồi HTTP bị cắt.");output.write(buffer,0,n);count-=n;}}
    static String line(InputStream input,int max)throws IOException{StringBuffer value=new StringBuffer();int ch;while((ch=input.read())!=-1){if(ch=='\r'){if(input.read()!='\n')throw new IOException("HTTP thiếu CRLF.");return value.toString();}if(ch=='\n'||value.length()>=max)throw new IOException("HTTP line không đúng.");value.append((char)ch);}throw new EOFException("Không nhận đủ phản hồi HTTP.");}
    void release(){SocketConnection active=socket;socket=null;if(active!=null)try{active.close();}catch(Exception ignored){}TlsClientProtocol secure=tls;tls=null;if(secure!=null)try{secure.close();}catch(Exception ignored){}}
    public void close(){closed=true;release();}
    public InputStream openInputStream()throws IOException{send();return new ByteArrayInputStream(responseBody);}
    public DataInputStream openDataInputStream()throws IOException{return new DataInputStream(openInputStream());}
    public OutputStream openOutputStream()throws IOException{if(sent||closed)throw new IOException("Đã gửi yêu cầu.");return requestBody;}
    public DataOutputStream openDataOutputStream()throws IOException{return new DataOutputStream(openOutputStream());}
    public int getResponseCode()throws IOException{send();return response;}
    public String getResponseMessage()throws IOException{send();return message;}
    public String getURL(){return url;}public String getProtocol(){return "https";}public String getHost(){return host;}public String getFile(){return file;}public String getRef(){return null;}public String getQuery(){int q=file.indexOf('?');return q<0?null:file.substring(q+1);}public int getPort(){return port;}
    public String getRequestMethod(){return method;}public void setRequestMethod(String value)throws IOException{if(sent||!(value.equals(GET)||value.equals(POST)))throw new IOException("HTTP method không hỗ trợ.");method=value;}
    public String getRequestProperty(String key){return (String)requestHeaders.get(key.toLowerCase());}
    public void setRequestProperty(String key,String value)throws IOException{if(sent||key.indexOf('\r')>=0||key.indexOf('\n')>=0||key.indexOf(':')>=0||value.indexOf('\r')>=0||value.indexOf('\n')>=0)throw new IOException("HTTP header không hợp lệ.");key=key.toLowerCase();if(key.equals("host")||key.equals("connection")||key.equals("transfer-encoding"))throw new IOException("Không cho đổi HTTP framing.");requestHeaders.put(key,value);}
    public String getHeaderField(String key)throws IOException{send();return (String)headers.get(key.toLowerCase());}
    public String getHeaderField(int n)throws IOException{send();return n<0||n>=headerKeys.size()?null:(String)headers.get(headerKeys.elementAt(n));}
    public String getHeaderFieldKey(int n)throws IOException{send();return n<0||n>=headerKeys.size()?null:(String)headerKeys.elementAt(n);}
    public int getHeaderFieldInt(String key,int fallback)throws IOException{try{return Integer.parseInt(getHeaderField(key));}catch(NumberFormatException e){return fallback;}}
    public long getHeaderFieldDate(String key,long fallback){return fallback;}public long getExpiration(){return 0;}public long getDate(){return 0;}public long getLastModified(){return 0;}
    public String getType(){return (String)headers.get("content-type");}public String getEncoding(){return (String)headers.get("content-encoding");}public long getLength(){return responseBody==null?-1:responseBody.length;}
}
