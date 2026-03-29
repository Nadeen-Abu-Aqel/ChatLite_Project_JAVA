package Network;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    public String clientName;
    private String room = "General"; // الغرفة الافتراضية
public int sentCount = 0;
public int receivedCount = 0;
   private ServerUI serverUI;
public int archiveCount = 0;
public String status = "ACTIVE";
public ClientHandler(Socket socket, ServerUI serverUI) {
    this.socket = socket;
    this.serverUI = serverUI;
}
public void sendMessage(String msg) {
    out.println(msg);
}
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String msg;
            while ((msg = in.readLine()) != null) {
                // تسجيل الدخول
              if (msg.startsWith("HELLO")) {
    clientName = msg.split(" ")[1];
status = "ONLINE";   // 🔥 أضف هذا

out.println("200 WELCOME");

serverUI.updateLog(clientName + " joined");
serverUI.refreshUsers();

broadcast("INFO " + clientName + " joined the chat");
}
                
                // إرسال رسالة عادية (تصل فقط لأعضاء نفس الغرفة)
 else if (msg.startsWith("MSG")) {
    String[] parts = msg.split(" ", 3);

    String roomName = parts[1];
    String text = parts[2];

    sentCount++;

    String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

    for (ClientHandler c : ServerUI.clients) {
        if (c.room.equals(roomName)) {
            c.out.println("[" + time + "] " + clientName + ": " + text);
            c.receivedCount++;
        }
    }

    out.println("211 SENT");
    serverUI.refreshStats();
}    
   // إرسال رسالة خاصة
               else if (msg.startsWith("PM")) {
    String[] parts = msg.split(" ", 3);
    sendPrivate(parts[1], parts[2]);
    out.println("212 PRIVATE SENT");   // 🔥 مهم
}
                // طلب قائمة المستخدمين
                else if (msg.equals("USERS")) {
                    out.println("213 " + ServerUI.clients.size());

for (ClientHandler c : ServerUI.clients) {
    if (c.clientName != null)
        out.println("213U " + c.clientName);
}

out.println("213 END");
                }
                else if (msg.startsWith("JOIN")) {

    String newRoom = msg.split(" ")[1];

    String oldRoom = this.room;

    // إشعار الغرفة القديمة
    for (ClientHandler c : ServerUI.clients) {
        if (c.room.equals(oldRoom)) {
            c.out.println("INFO " + clientName + " left the room");
        }
    }

    // تغيير الغرفة
    this.room = newRoom;

    // تأكيد للعميل
out.println("210 JOINED " + newRoom);
    // إشعار الغرفة الجديدة
    for (ClientHandler c : ServerUI.clients) {
        if (c.room.equals(newRoom)) {
            c.out.println("INFO " + clientName + " joined " + newRoom);
        }
    }
}else if (msg.startsWith("LEAVE")) {
    this.room = "General";
    out.println("215 LEFT");
}
               else if (msg.equals("QUIT")) {
    out.println("221 BYE");
    break;
}else if (msg.equals("ROOMS")) {
    out.println("214 General");
    out.println("214 Networks");
    out.println("214 Java");
}
            }
        } catch (Exception e) {
serverUI.updateLog("Connection error with " + clientName);        } finally {
            cleanup();
        }
    }
private void broadcast(String msg) {
    for (ClientHandler c : ServerUI.clients) {
        if (c.room.equals(this.room)) {
            c.out.println(msg);
            c.receivedCount++;
        }
    }

    serverUI.refreshStats(); // 🔥 مهم جداً
}

    private void sendPrivate(String target, String msg) {
        for (ClientHandler c : ServerUI.clients) {
            if (c.clientName != null && c.clientName.equalsIgnoreCase(target)) {
                c.out.println("(Private) From " + clientName + ": " + msg);
                out.println("(Private) To " + target + ": " + msg);
                return;
            }
        }
        out.println("User [" + target + "] not found.");
    }private void cleanup() {
    try { socket.close(); } catch (Exception e) {}

    status = "OFFLINE";   // 🔥 مهم

    if (clientName != null) {
        serverUI.updateLog(clientName + " left");
        broadcast("INFO " + clientName + " left the chat");
    }

    //ServerUI.clients.remove(this);

    serverUI.refreshUsers();
    serverUI.refreshStats();
}
    public String getIP() {
    return socket.getInetAddress().toString();
}
    public void kick() {
    try {
        sendMessage("[SERVER] You have been kicked!");
        socket.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}