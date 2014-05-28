package jetty_web_socket;
import java.io.IOException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jetty.websocket.common.WebSocketSession;

@ClientEndpoint
@ServerEndpoint(value="/events/")
public class EventSocket
{
    private WebSocketSession sess;

	@OnOpen
    public void onWebSocketConnect(Session sess)
    {
        this.sess = (WebSocketSession) sess;
		System.out.println("Socket Connected: " + sess);
    }
    
    @OnMessage
    public void onWebSocketText(String message)
    {
        System.out.println("Received TEXT message: " + message);
        if (sess != null){
        	try {
				sess.getRemote().sendString("Received: "+message+" from "+sess.getRequestURI()+ " at "+ sess.getRemoteAddress());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    @OnClose
    public void onWebSocketClose(CloseReason reason)
    {
        System.out.println("Socket Closed: " + reason);
    }
    
    @OnError
    public void onWebSocketError(Throwable cause)
    {
        cause.printStackTrace(System.err);
    }
}