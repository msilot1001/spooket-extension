import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Main extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static String serverIP = "localhost";
    private static int port = 8008;
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

        System.out.println("Connecting to " + serverIP + " on port " + port);
        try {
            main.startConnection();
            System.out.println("Connected to " + main.getSocket().getRemoteSocketAddress());
            /*while (true) {
                System.out.println("Say Something");
                Scanner stdIn = new Scanner(System.in);
                fromUser = stdIn.nextLine();
                if (fromUser != null) {
                    System.out.println("Client: " + fromUser);
                    fromServer = main.sendMessage(fromUser);
                    fromUser = null;
                    if(fromServer.equals("bye")) {
                        main.getSocket().close();
                        System.out.println("Connection Terminated");
                        break;
                    }
                    System.out.println("Server: " + fromServer);
                }
            }*/

        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}