package jetty_web_socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectorSpike {
	static String word = "abacate";
	
	public static void main(String[] args) throws Exception {
		URL ur = new URL("http://127.0.0.1:8080");
		URLConnection conn = ur.openConnection();
		conn.setDoOutput(true);
		OutputStream out = conn.getOutputStream();
		InputStream in = conn.getInputStream();
		for (int i = 0; i < 1000; i++){
			System.out.println(in.read());
		}
		System.out.println("The End");
	}
}
