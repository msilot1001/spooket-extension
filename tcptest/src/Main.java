import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class Main extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static String serverIP = "192.168.1.213";
    private static int port = 25565;
    private String fromServer;
    private String fromClient;

    public Socket getSocket() {
        return clientSocket;
    }

    public void startConnection() throws IOException {
        clientSocket = new Socket(serverIP, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendMessage(String msg) throws IOException {
        out.println(msg);
        return in.readLine();
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args) {
        Main main = new Main();
        String fromServer;
        String fromUser;

        String type = "CRDD";
        String payload = "1242456";
        String sEnd = "\r\n";

        createPacket(type,payload,sEnd);
//        System.out.println("Connecting to " + serverIP + " on port " + port);
//        try {
//            main.startConnection();
//            System.out.println("Connected to " + main.getSocket().getRemoteSocketAddress());
//            while (true) {
//                System.out.println("Say Something");
//                Scanner stdIn = new Scanner(System.in);
//                fromUser = stdIn.nextLine();
//                if (fromUser != null) {
//                    System.out.println("Client: " + fromUser);
//                    fromServer = main.sendMessage(fromUser);
//                    fromUser = null;
//                    if(fromServer.equals("bye")) {
//                        main.getSocket().close();
//                        System.out.println("Connection Terminated");
//                        break;
//                    }
//                    System.out.println("Server: " + fromServer);
//                }
//            }
//
//        } catch(IOException e) {
//            e.printStackTrace();
//        }
    }
    public static byte[] createPacket(String type, String payload, String sEnd) {
        byte[] buffer = new byte[1024];

        char[] typeArr = type.toCharArray();
        char[] sEndArr = sEnd.toCharArray();

        // type
        buffer[0] = (byte) typeArr[0];
        buffer[1] = (byte) typeArr[1];
        buffer[2] = (byte) typeArr[2];
        buffer[3] = (byte) typeArr[3];

        System.out.println(Arrays.toString(Arrays.copyOfRange(buffer, 0, 4)));

        // payload
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        System.out.println("payloadBytes = " + Arrays.toString(payloadBytes));
        if (payloadBytes.length > 1018) {
            throw new Error("Too big payload!");
        } else System.arraycopy(payloadBytes, 0, buffer, 4, payloadBytes.length);

        // end
        buffer[1022] = (byte)sEndArr[0];
        buffer[1023] = (byte)sEndArr[1];

        System.out.println(Arrays.toString(buffer));

        return buffer;
    }
}