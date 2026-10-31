package com.auction.server.network;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            // Open port 8888 on com.auction.server.network.Server to accept incoming connections
            ServerSocket serverSocket = new ServerSocket(8888);
            System.out.println("[SERVER] Multithreaded com.auction.server.network.Server is running on port 8888...");

            int clientCount = 0;

            // Structure 1: Infinite loop to continuously accept incoming clients
            while (true) {
                // Block thread until a new client connects
                Socket socket = serverSocket.accept();
                clientCount++;
                System.out.println("[SERVER] Client #" + clientCount + " connected from IP: " + socket.getInetAddress());

                // Structure 2: Spawn a separate thread for each connected client
                ClientHandler handler = new ClientHandler(socket, clientCount);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// Structure 3: Class handling dedicated communication for a single client
class ClientHandler implements Runnable {
    private Socket socket;
    private int clientId;

    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            // Send welcome message and client ID upon successful connection
            out.writeUTF("Welcome! You are Client #" + clientId);
            out.flush();

            // Continuous loop to listen for client messages
            while (true) {
                String message = in.readUTF();

                // Break loop if client sends disconnect keyword
                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("[SERVER] Client #" + clientId + " requested to disconnect.");
                    break;
                }

                System.out.println("[CLIENT #" + clientId + "]: " + message);

                // Echo message back to client
                out.writeUTF("com.auction.server.network.Server received: " + message);
                out.flush();
            }
        } catch (EOFException e) {
            System.out.println("[SERVER] Client #" + clientId + " disconnected abruptly.");
        } catch (Exception e) {
            System.out.println("[SERVER] Error handling Client #" + clientId + ": " + e.getMessage());
        } finally {
            // Structure 4: Mandatory resource cleanup to prevent memory leaks
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("[SERVER] Closed socket for Client #" + clientId);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}