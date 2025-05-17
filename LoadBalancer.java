// Simple Load Balancer in Java

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class LoadBalancer {
    private static final int PORT = 8080; // Port the load balancer listens on
    private static final List<String> BACKEND_SERVERS = Arrays.asList("localhost:9001", "localhost:9002");
    private static int currentIndex = 0; // For round-robin algorithm

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Load Balancer is listening on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept(); // Accept incoming client connection
            new Thread(() -> handleClient(clientSocket)).start(); // Handle each connection in a new thread
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            String backend = getNextBackend(); // Pick backend using round-robin
            String[] parts = backend.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            Socket backendSocket = new Socket(host, port); // Connect to backend

            // Forward client request to backend
            Thread forwardClient = new Thread(() -> forwardData(clientSocket, backendSocket));
            Thread forwardBackend = new Thread(() -> forwardData(backendSocket, clientSocket));

            forwardClient.start();
            forwardBackend.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void forwardData(Socket inputSocket, Socket outputSocket) {
        try {
            InputStream in = inputSocket.getInputStream();
            OutputStream out = outputSocket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            // Connection might close
        } finally {
            try {
                inputSocket.close();
                outputSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static synchronized String getNextBackend() {
        String backend = BACKEND_SERVERS.get(currentIndex);
        currentIndex = (currentIndex + 1) % BACKEND_SERVERS.size(); // Round-robin index update
        return backend;
    }
}
