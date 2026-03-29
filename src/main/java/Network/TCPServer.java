package Network;

import java.net.*;
import java.util.*;

public class TCPServer {

    public static Vector<ClientHandler> clients = new Vector<>();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(6789);
        System.out.println("--- Server Engine Started on Port 6789 ---");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("New client detected: " + socket.getInetAddress());

            ClientHandler client = new ClientHandler(socket, null);
            clients.add(client);
            client.start();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}