import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.*;

public class ServerTask extends Thread {
    private BufferedReader bufferedReader;
    private Peer peer = null;
    private PrintWriter out = null;
    private Socket socket = null;

    public ServerTask(Socket socket, Peer peer) throws IOException {
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        this.peer = peer;
        this.socket = socket;
    }

    public void run() {
        while (true) {
            try {
                JSONObject json = new JSONObject(bufferedReader.readLine());

                if (json.getString("type").equals("join")){
                    System.out.println("     " + json); // just to show the json

                    System.out.println("     " + json.getString("username") + " wants to join the network");
                    peer.updateListenToPeers(json.getString("ip") + ":" + json.getInt("port"));
                    out.println(("{'type': 'join', 'list': '"+ peer.getPeers() +"'}"));

                    if (peer.isLeader()){
                        peer.pushMessage(json.toString());
                    }

                } else {
                    System.out.println("[" + json.getString("username")+"]: " + json.getString("message"));
                }


            } catch (Exception e) {
                interrupt();
                break;
            }
        }
    }
}