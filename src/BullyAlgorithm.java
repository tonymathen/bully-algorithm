import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.HashMap;

public class BullyAlgorithm implements Runnable {
    static int nodeId = -1;
    static HashMap<Integer,String> nodes= new HashMap<>();
    String mode;
    static int leaderId = -1;
    static int nodeServerPort = 8070;
    static int senderNodeId = -1;
    String messageType;
    static boolean electionInProgress = false;
    static boolean receivedOk = false;

    static boolean isLeader = false;
    static int greaterNodes = 0;
    public BullyAlgorithm(String mode) {
        this.mode = mode;
    }

    public BullyAlgorithm(String mode, String messageType) {
        this.mode = mode;
        this.messageType = messageType;
    }

    public static void initializeNodes() throws UnknownHostException, InterruptedException {
        nodes.put(1,"node1");
        nodes.put(2,"node2");
        nodes.put(3,"node3");
        nodes.put(4,"node4");
        nodes.put(5,"node5");
        InetAddress iAddress = InetAddress.getLocalHost();
        String nodeName = iAddress.getHostName();

        switch (nodeName){
            case "node1":
                Thread.sleep(1000);
                nodeId = 1;
                greaterNodes = countHigherPriorityNodes();
                System.out.println("Node "+ nodeId + " has joined the network");
//                Runnable sender = new BullyAlgorithm("SENDER", "ELECTION");
//                new Thread(sender).start();
                break;
            case "node2":
                nodeId = 2;
                greaterNodes = countHigherPriorityNodes();
                System.out.println("Node "+ nodeId + " has joined the network");
                break;
            case "node3":
                nodeId = 3;
                greaterNodes = countHigherPriorityNodes();
                System.out.println("Node "+ nodeId + " has joined the network");
                break;
            case "node4":
                nodeId = 4;
                greaterNodes = countHigherPriorityNodes();
                System.out.println(greaterNodes);
                System.out.println("Node "+ nodeId + " has joined the network");
                break;
            case "node5":
                nodeId = 5;
                greaterNodes = countHigherPriorityNodes();
//                System.out.println("Inside Case 5");
                System.out.println("Node "+ nodeId + " has joined the network");
                break;
            default:
                System.out.println("Node is not known to the network");
        }
        if(!isLeader) {
            System.out.println("No leader for the network. New Election Starting");
            electionInProgress = true;
            Runnable sender = new BullyAlgorithm("SENDER", "ELECTION");
            new Thread(sender).start();

        }
    }


    @Override
    public void run(){
//        System.out.println("Inside run for "+nodeId);
//        System.out.println("Operation for "+nodeId + " is "+ mode);
        if(mode.equals("RECEIVER")){
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

                    else if (option.equals("ELECTION")) {
                        senderNodeId = Integer.parseInt(in.readUTF());
                        //Start own election if node has higher priority than sender node
                        if(senderNodeId < nodeId){
                            Runnable sender = new BullyAlgorithm("SENDER","OK");
                            new Thread(sender).start();
                        }

                        if(!electionInProgress){
                            electionInProgress = true;
                            Runnable newElection = new BullyAlgorithm("SENDER", "ELECTION");
                            new Thread(newElection).start();
                            System.out.println(nodes.get(nodeId) + " started its election");

                            Runnable timer = new BullyAlgorithm("TIMER");
                            new Thread(timer).start();
                            System.out.println("Timer thread started");

                        }

                    }
                    else if (option.equals("OK")){
                        receivedOk = true;
                        int senderId = Integer.parseInt(in.readUTF());
                        System.out.println("Received OK from " +nodes.get(senderId));

                    }
                    else if(option.equals("COORDINATE")){
                        leaderId = Integer.parseInt(in.readUTF());
                        isLeader = true;
                        receivedOk = true;
                        electionInProgress = false;
                        System.out.println(nodes.get(nodeId) + " has acknowledged leader as " +nodes.get(leaderId));
                        System.out.println("Value of received "+receivedOk);
                    }
                    socket.close();
                }
            }
            catch(Exception e){
                e.printStackTrace();

            }
        }

        else if(mode.equals("HEARTBEAT")){
            while(true){
                try{
                    System.out.print("");

                        Thread.sleep(2000);
                        String leaderNode=nodes.get(leaderId);
                        System.out.println("Destination Server "+ leaderNode + " Port "+nodeServerPort);
                        Socket socket = new Socket(leaderNode, nodeServerPort);
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("HEARTBEAT");
                        out.writeUTF(nodeId+"");

                        System.out.println("Sent HEARTBEAT to : "+leaderNode);
                }

                catch(Exception e){
                    System.out.printf("Leader has failed!");
                    System.out.println(nodes.get(nodeId) + " starting election");
                    isLeader = false;

                    Runnable newElection = new BullyAlgorithm("SENDER", "ELECTION");
                    new Thread(newElection).start();


                }
            }
        }

        else if(mode.equals("SENDER")) {
            // Start Election
            if(messageType.equals("ELECTION")){
                startElection();
            }
            // Send OK for Election Request
            else if(messageType.equals("OK")){
                sendOk();
            }
            // Send Co ordination message
            else if(messageType.equals("COORDINATE")){
                sendCoordinatorMessage();
            }


        }

        else if(mode.equals("TIMER")){
            try{
                Thread.sleep(7000);
                if(!receivedOk){
                    leaderId = nodeId;
                    isLeader = true;
                    electionInProgress = false;
                    System.out.println("I am the new Leader");
                    //Start Coordination

                    Runnable coordinator = new BullyAlgorithm("SENDER", "COORDINATE");
                    new Thread(coordinator).start();
                }

            }catch(Exception e){
                //Timer interrupted
            }

        }

    }

    public static int countHigherPriorityNodes(){
        int countGreaterNodes = 0;
        for(int peerNodeId:nodes.keySet()){
            if(peerNodeId > nodeId){
                countGreaterNodes++;
            }
        }
        return countGreaterNodes;
    }
    public static void startElection() {
        int failedNodes = 0;
        for (int peerNodeId: nodes.keySet()) {
            //Find all peer nodes having a greater ID
            if (peerNodeId > nodeId) {
                String peerNode = nodes.get(peerNodeId);

                try {
                    Socket socket = new Socket(peerNode, nodeServerPort);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("ELECTION");
                    out.writeUTF(nodeId + "");
                    System.out.println("Sent Election Request to : "+peerNode);


                } catch(Exception e) {
                    //PeerNode has failed
                    failedNodes++;

                }
            }
        }
        if(failedNodes == greaterNodes){
            //Start timer and wait before electing itself as leader
            System.out.println("Greater Nodes have Failed");
            System.out.println("Timer started for declaring "+nodes.get(nodeId) + " as leader");
            electionInProgress = true;
            receivedOk = false;
            Runnable electionTimer = new BullyAlgorithm("TIMER");
            new Thread(electionTimer).start();
        }
    }

    public static void sendOk(){
        try{
            String peerNode = nodes.get(senderNodeId);
            Socket socket = new Socket(peerNode, nodeServerPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("OK");
            out.writeUTF(nodeId + "");
            System.out.println("Sent OK to " +peerNode);
        } catch (Exception e){
            // Peer Node failed
        }
    }

    public static void sendCoordinatorMessage() {
        System.out.println("Inside coordinator LeaderId value" +leaderId);
        for (int peerNodeId : nodes.keySet()) {
            if(peerNodeId != nodeId){
                String peerNode = nodes.get(peerNodeId);
                try{
                    Socket socket = new Socket(peerNode, nodeServerPort);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("COORDINATE");
                    out.writeUTF(leaderId+"");

                    System.out.println("Sent coordinator message to "+peerNode);
                }
                catch(Exception e){
                    //Peer Node Failed
                }

            }
        }
    }


    public static void main(String[] args) throws UnknownHostException, InterruptedException {
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
