import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            System.out.println("Connecting to server...");
            Socket socket = new Socket("localhost", 8888);
            System.out.println("Connected to server successfully!");

            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Scanner scanner = new Scanner(System.in);

            // Read welcome message from server
            String welcomeMsg = in.readUTF();
            System.out.println("[SERVER]: " + welcomeMsg);

            // Loop to send messages continuously
            while (true) {
                System.out.print("Enter message (or 'exit' to quit): ");
                String msg = scanner.nextLine();

                out.writeUTF(msg);
                out.flush();

                if (msg.equalsIgnoreCase("exit")) {
                    System.out.println("Disconnecting from server...");
                    break;
                }

                // Read server response
                String response = in.readUTF();
                System.out.println("[SERVER RESPONSE]: " + response);
            }

            socket.close();
            scanner.close();
        } catch (Exception e) {
            System.out.println("Connection closed or failed to connect to server.");
        }
    }
}