import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class BullyAlgorithm implements Runnable {
    static int nodeId = -1;
    static HashMap<Integer,String> nodes= new HashMap<>();
    static boolean isLeader = false;
    static String operation;
    static int leaderId = -1;
    static int nodeServerPort = 5511;

    public BullyAlgorithm(String operation) {
        this.operation = operation;
    }
    public static void initializeNodes() throws UnknownHostException {
        nodes.put(1,"node1");
        nodes.put(2,"node2");
        nodes.put(3,"node3");
        InetAddress iAddress = InetAddress.getLocalHost();
        String nodeName = iAddress.getHostName();

        switch (nodeName){
            case "node1":
                nodeId = 1;
                System.out.println("Node "+ nodeId + " has joined the network");
                break;
            case "node2":
                nodeId = 2;
                System.out.println("Node "+ nodeId + " has joined the network");
                break;
            case "node3":
                nodeId = 3;
                System.out.println("Node "+ nodeId + " has joined the network");
                break;
            default:
                System.out.println("Node is not known to the network");
        }
    }

    @Override
    public void run() {
        switch(operation) {
            case "HEARTBEAT":
                while(true) {
                    try {
                        System.out.println("");
                        if((nodeId != leaderId)) {
                            Thread.sleep(1500*60);
                            String leaderNode = nodes.get(leaderId);
                            Socket socket = new Socket(leaderNode, nodeServerPort);
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            out.writeUTF("HEARTBEAT");
                            out.writeUTF(nodeId + " ");
                            System.out.println("Sent heartbeat to " + leaderNode);

                        }
                    } catch (Exception e) {
                        //Start new election
                    }
                }
        }

    }

    public static void main(String[] args) throws UnknownHostException {
        initializeNodes();
        Runnable heartbeat = new BullyAlgorithm("HEARTBEAT");
        new Thread(heartbeat).start();

        while(true) {}

    }
}
