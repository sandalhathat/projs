import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Peer {
    private String username;
    private BufferedReader bufferedReader;
    private ServerThread serverThread;
    private Set<SocketInfo> peers = new HashSet<SocketInfo>();
    private boolean leader = false;
    private SocketInfo leaderSocket;

    public Peer(BufferedReader bufReader, String username, ServerThread serverThread) {
        this.username = username;
        this.bufferedReader = bufReader;
        this.serverThread = serverThread;
    }

    public void setLeader(boolean leader, SocketInfo leaderSocket) {
        this.leader = leader;
        this.leaderSocket = leaderSocket;
    }

    public boolean isLeader(){
        return leader;
    }

    public void addPeer(SocketInfo si) {
        peers.add(si);
    }

    public String getPeers() {
        String s = "";
        for (SocketInfo p: peers) {
            s = s + p.getHost() + ":" + p.getPort() + " ";
        }
        return s;
    }

    public void updateListenToPeers(String list) throws Exception {
        String[] peerList = list.split(" ");
        for(String p : peerList) {
            String[] hostPort = p.split(":");

            if ((hostPort[0].equals("localhost") || hostPort[0].equals(serverThread.getHost())) && Integer.valueOf(hostPort[1]) == serverThread.getPort()){
                continue;
            }
            SocketInfo s = new SocketInfo(hostPort[0], Integer.valueOf(hostPort[1]));
            peers.add(s);
        }
    }

    public void askForInput() throws Exception {
		try {
			
			System.out.println("> You can now start chatting (exit to exit)");
			while(true) {
				String message = bufferedReader.readLine();
				if (message.equals("exit")) {
					System.out.println("bye, see you next time");
					break;
				} else {
					pushMessage("{'type': 'message', 'username': '"+ username +"','message':'" + message + "'}");
				}	
			}
			System.exit(0);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public void commLeader(String message) {
		try {
			BufferedReader reader = null; 
				Socket socket = null;
				try {
					socket = new Socket(leaderSocket.getHost(), leaderSocket.getPort());
					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					
				} catch (Exception c) {
					if (socket != null) {
						socket.close();
					} else {
						System.out.println("Could not connect to " + leaderSocket.getHost() + ":" + leaderSocket.getPort());
					}
					return; // returning since we cannot connect or something goes wrong the rest will not work. 
				}

				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println(message);

				JSONObject json = new JSONObject(reader.readLine());
				System.out.println("     Received from server " + json);
				String list = json.getString("list");
				updateListenToPeers(list); // when we get a list of all other peers that the leader knows we update them

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    public void pushMessage(String message) {
		try {
			System.out.println("     Trying to send to peers: " + peers.size());

			Set<SocketInfo> toRemove = new HashSet<SocketInfo>();
			BufferedReader reader = null; 
			int counter = 0;
			for (SocketInfo s : peers) {
				Socket socket = null;
				try {
					socket = new Socket(s.getHost(), s.getPort());
					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				} catch (Exception c) {
					if (socket != null) {
						socket.close();
					} else {
						System.out.println("  Could not connect to " + s.getHost() + ":" + s.getPort());
						System.out.println("  Removing that socketInfo from list");
						toRemove.add(s);
						continue;
					}
					System.out.println("     Issue: " + c);
				}

				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println(message);
				counter++;
				socket.close();
		     }
		    for (SocketInfo s: toRemove){
		    	peers.remove(s);
		    }

		    System.out.println("     Message was sent to " + counter + " peers");

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    public static void main (String[] args) throws Exception {

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String username = args[0];
		System.out.println("Hello " + username + " and welcome! Your port will be " + args[1]);

		int size = args.length;
		System.out.println(size);
		if (size == 4) {
			System.out.println("Started peer");
        } else {
            System.out.println("Expected: <name(String)> <peer(String)> <leader(String)> <isLeader(bool-String)>");
            System.exit(0);
        }

        System.out.println(args[0] + " " + args[1]);
        ServerThread serverThread = new ServerThread(args[1]);
        Peer peer = new Peer(bufferedReader, username, serverThread);

        String[] hostPort = args[2].split(":");
        SocketInfo s = new SocketInfo(hostPort[0], Integer.valueOf(hostPort[1]));
        System.out.println(args[3]);
        if (args[3].equals("true")){
			System.out.println("Is leader");
			peer.setLeader(true, s);
		} else {
			System.out.println("Pawn");

			// add leader to list 
			peer.addPeer(s);
			peer.setLeader(false, s);

			// send message to leader that we want to join
			peer.commLeader("{'type': 'join', 'username': '"+ username +"','ip':'" + serverThread.getHost() + "','port':'" + serverThread.getPort() + "'}");

		}
		serverThread.setPeer(peer);
		serverThread.start();
		peer.askForInput();

	}

}
