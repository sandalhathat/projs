import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {
    private ServerSocket serverSocket;
    private SocketInfo socket;
    private Peer peer = null;

    public ServerThread(String peer) throws IOException {
        String[] hostPort = peer.split(":");
        int port = Integer.valueOf(hostPort[1]);
        String host = hostPort[0];
        System.out.println("    host: " + host);
        socket = new SocketInfo(host, port);
        serverSocket = new ServerSocket(port);
        System.out.println("    Listening on: " + host + ":" + port);
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public String getHost() {
        return socket.getHost();
    }

    public int getPort() {
        return socket.getPort();
    }

    public void run() {
        try {
            while (true) {
                Socket sock = serverSocket.accept();
                new ServerTask(sock, peer).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}