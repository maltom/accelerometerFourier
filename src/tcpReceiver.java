import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class tcpReceiver
{
private Socket tcpSocket;
private BufferedReader incomingMessage;
private int port = 2134;
private String ip = "192.168.1.11";
public void startConnection() throws IOException {
    tcpSocket = new Socket(ip,port);
    incomingMessage = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
}
public void stopConnection() throws IOException {
    incomingMessage.close();
    tcpSocket.close();
}
}
