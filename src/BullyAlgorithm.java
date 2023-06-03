import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class BullyAlgorithm implements Runnable {
    static int nodeId = -1;
    static HashMap<Integer,String> nodes= new HashMap<>();
    String operation;
    static int leaderId = 3;
    static int nodeServerPort = 8070;

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
    public void run(){
        System.out.println("Inside run for "+nodeId);
        System.out.println("Operation for "+nodeId + " is "+ operation);
        if(operation.equals("RECEIVER")){
            ServerSocket serverSocket = null;
            try{
                serverSocket = new ServerSocket(nodeServerPort);
                while(true){
                    Socket socket = serverSocket.accept();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String option=in.readUTF();
                    if(option.equals("HEARTBEAT")){
                        int sender=Integer.parseInt(in.readUTF());
                        System.out.println("HEARTBEAT received from "+nodes.get(sender));
                    }
                    socket.close();
                }
            }
            catch(Exception e){
                e.printStackTrace();

            }
        }

        else if(operation.equals("HEARTBEAT")){
            while(true){
                try{
                    System.out.print("");

                        Thread.sleep(2000);
                        String peerNode=nodes.get(leaderId);
                        System.out.println("Destination Server "+ peerNode + " Port "+nodeServerPort);
                        Socket socket = new Socket(peerNode, nodeServerPort);
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("HEARTBEAT");
                        out.writeUTF(nodeId+"");

                        System.out.println("Sent HEARTBEAT to : "+peerNode);
                }

                catch(Exception e){
                    System.out.printf("Unable to connect .. retrying");

                }
            }
        }

        else if(operation.equals("SENDER")) {
            // Start Election
            // Send OK for Election Request
            // Send Co ordination message

        }
    }

    public static void startElection() {
        for (int peerNodeId: nodes.keySet()) {
            if (peerNodeId > nodeId) {
                String peerNode = nodes.get(peerNodeId);

                try {
                    Socket socket = new Socket(peerNode, nodeServerPort);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    //Send Election Request


                } catch(Exception e) {
                    //PeerNode has failed
                }
            }
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        initializeNodes();
//        System.out.println("Before Run");
            System.out.println("Receiver for "+nodeId + " starting");
            Runnable receiver = new BullyAlgorithm("RECEIVER");
            new Thread(receiver).start();

            System.out.println("Heartbeat from "+nodeId + " starting");
            Runnable heartbeat = new BullyAlgorithm("HEARTBEAT");
            new Thread(heartbeat).start();

        while(true) {}

    }
}
