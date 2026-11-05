package com.auction.server.network;

import com.auction.shared.network.NetworkRequest;
import com.auction.shared.network.NetworkResponse;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientNetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(ClientNetworkManager.class);

    private final String host;
    private final int port;
    private final ClientResponseDispatcher dispatcher;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private volatile boolean running;
    private ExecutorService listenerThread;

    public ClientNetworkManager(String host, int port, ClientResponseDispatcher dispatcher) {
        this.host = host;
        this.port = port;
        this.dispatcher = dispatcher;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        openStreams();
        running = true;

        listenerThread = Executors.newSingleThreadExecutor();
        listenerThread.submit(this::listen);
        logger.info("Connected to server at {}:{}", host, port);
    }

    private void openStreams() throws IOException {
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
    }

    private void listen() {
        try {
            while (running) {
                Object response = input.readObject();
                if (response instanceof NetworkResponse) {
                    dispatcher.dispatch((NetworkResponse) response);
                }
            }
        } catch (EOFException e) {
            logger.info("Server disconnected.");
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                logger.warn("Connection to server lost: {}", e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    public synchronized void sendRequest(NetworkRequest request) {
        try {
            if (output != null && !socket.isClosed()) {
                output.writeObject(request);
                output.flush();
                output.reset();
            }
        } catch (IOException e) {
            logger.warn("Failed to send request to server: {}", e.getMessage());
        }
    }

    public void disconnect() {
        if (!running) {
            return;
        }
        running = false;
        closeQuietly(input);
        closeQuietly(output);
        closeSocket();
        if (listenerThread != null && !listenerThread.isShutdown()) {
            listenerThread.shutdownNow();
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            logger.debug("Ignored error closing resource: {}", e.getMessage());
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.debug("Ignored error closing socket: {}", e.getMessage());
        }
    }
}
