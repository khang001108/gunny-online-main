import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** In-process adapter for the shared game handler: no loopback HTTP request. */
final class TcpExchange extends HttpExchange {
 final Headers request=new Headers(),response=new Headers();
 final ByteArrayOutputStream output=new ByteArrayOutputStream();
 final InputStream input;final InetSocketAddress remote,local;int status=200;
 TcpExchange(Socket socket,byte[] body){input=new ByteArrayInputStream(body);remote=(InetSocketAddress)socket.getRemoteSocketAddress();local=(InetSocketAddress)socket.getLocalSocketAddress();}
 public Headers getRequestHeaders(){return request;}public Headers getResponseHeaders(){return response;}
 public URI getRequestURI(){return URI.create("/api");}public String getRequestMethod(){return "POST";}
 public HttpContext getHttpContext(){return null;}public void close(){}
 public InputStream getRequestBody(){return input;}public OutputStream getResponseBody(){return output;}
 public void sendResponseHeaders(int code,long length){status=code;}public InetSocketAddress getRemoteAddress(){return remote;}
 public int getResponseCode(){return status;}public InetSocketAddress getLocalAddress(){return local;}
 public String getProtocol(){return "GUNNY/TCP";}public Object getAttribute(String name){return null;}
 public void setAttribute(String name,Object value){}public void setStreams(InputStream in,OutputStream out){throw new UnsupportedOperationException();}
 public HttpPrincipal getPrincipal(){return null;}
}
