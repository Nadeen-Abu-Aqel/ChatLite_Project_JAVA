package Network;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;

public class UltraChatUI extends JFrame {

    private JTextArea chatArea;
    private JTextField messageField;
    private JTextArea notificationArea;
    private DefaultListModel<String> userModel;
    private JList<String> userList;
    private JList<String> roomList;

    // إدارة نوافذ البرايفت المستقلة لضمان عدم تكرار فتح نافذة لنفس الشخص
    private HashMap<String, PrivateChatWindow> openPrivateWindows = new HashMap<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String currentRoom = "General";

    // سمة الألوان الداكنة (Dark Theme)
    Color bgDark = new Color(15, 10, 25);
    Color panelDark = new Color(25, 15, 40);
    Color purpleBorder = new Color(90, 50, 150);
    Color accentColor = new Color(110, 60, 180);

    public UltraChatUI() {
        initUI();
        connect();
    }

    private void initUI() {
        setTitle("Ultra Chat System - Professional Edition");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(bgDark);
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- العمود الأيسر (الغرف، المستخدمين، الحالة) ---
        JPanel leftColumn = new JPanel(new GridLayout(3, 1, 0, 15));
        leftColumn.setOpaque(false);
        leftColumn.setPreferredSize(new Dimension(220, 0));
        leftColumn.add(createRoomsPanel());
        leftColumn.add(createUsersPanel());
        leftColumn.add(createStatusPanel());

        // --- العمود الأوسط (عرض الرسائل، حقل الإدخال) ---
        JPanel centerColumn = new JPanel(new BorderLayout(0, 15));
        centerColumn.setOpaque(false);
        centerColumn.add(createChatDisplayPanel(), BorderLayout.CENTER);
        centerColumn.add(createChatInputPanel(), BorderLayout.SOUTH);

        // --- العمود الأيمن (الإشعارات، مراقب النظام) ---
        JPanel rightColumn = new JPanel(new GridLayout(2, 1, 0, 15));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(280, 0));
        rightColumn.add(createNotificationPanel());
        rightColumn.add(createSystemMonitorPanel());

        add(leftColumn, BorderLayout.WEST);
        add(centerColumn, BorderLayout.CENTER);
        add(rightColumn, BorderLayout.EAST);

        setLocationRelativeTo(null);
    }

    // ================= أقسام واجهة المستخدم (UI Components) =================

    private JPanel createSystemMonitorPanel() {
        JPanel p = createStyledPanel("SYSTEM MONITOR");
        JPanel inner = new JPanel(new GridLayout(5, 1, 5, 5));
        inner.setOpaque(false);

        inner.add(createStatusLabel("📡 Status: Connected", Color.GREEN));
        inner.add(createStatusLabel("⏱ Latency: 24ms", Color.CYAN));
        inner.add(createStatusLabel("🔒 Encryption: AES-256", Color.WHITE));
        inner.add(createStatusLabel("⚙ Protocol: TCP/V6", Color.LIGHT_GRAY));

        JButton closeAllBtn = createStyledButton("Close All Privates");
        closeAllBtn.addActionListener(e -> {
            openPrivateWindows.values().forEach(Window -> Window.dispose());
            openPrivateWindows.clear();
        });

        p.add(inner, BorderLayout.CENTER);
        p.add(closeAllBtn, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createUsersPanel() {
        JPanel p = createStyledPanel("ONLINE USERS");
        userModel = new DefaultListModel<>();
        userList = createStyledList(userModel);
        userList.setForeground(new Color(50, 255, 50)); 

        // إضافة مستمع للنقر المزدوج لفتح نافذة البرايفت
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selected = userList.getSelectedValue();
                    if (selected != null) {
                        openPrivateWindow(selected.replace("● ", "").trim());
                    }
                }
            }
        });

        JButton refreshBtn = createStyledButton("Refresh List");
        refreshBtn.addActionListener(e -> { userModel.clear(); out.println("USERS"); });
        p.add(new JScrollPane(userList), BorderLayout.CENTER);
        p.add(refreshBtn, BorderLayout.SOUTH);
        return p;
    }

    // ================= وظائف الشبكة والصوت (Network & Audio) =================

    private void playNotificationSound() {
        // تشغيل صوت التنبيه الخاص بالنظام
        Runnable soundTask = (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.asterisk");
        if (soundTask != null) {
            soundTask.run();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void openPrivateWindow(String userName) {
        if (!openPrivateWindows.containsKey(userName)) {
            PrivateChatWindow win = new PrivateChatWindow(userName, out);
            openPrivateWindows.put(userName, win);
        } else {
            openPrivateWindows.get(userName).toFront();
        }
    }

    private void connect() {
        try {
socket = new Socket("10.158.205.250", 6789);
in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String name = JOptionPane.showInputDialog(this, "Enter name:");
            out.println("HELLO " + (name == null ? "Guest" : name));

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        if (msg.startsWith("213U ")) {
                            String user = msg.substring(5);
                            userModel.addElement("● " + user);
                            notificationArea.append("🟢 Online: " + user + "\n");
                            playNotificationSound();
                        } else if (msg.contains("(Private)")) {
                            chatArea.append("🔒 " + msg + "\n");
                            notificationArea.append("📩 New Private Message\n");
                            playNotificationSound();
                        } else if (msg.contains("joined") || msg.contains("left")) {
                            notificationArea.append("ℹ " + msg + "\n");
                            playNotificationSound();
                        } else {
                            chatArea.append(msg + "\n");
                        }
                    }
                } catch (Exception e) {}
            }).start();
        } catch (Exception e) {
            notificationArea.append("❌ Connection Error!\n");
        }
    }

    // ================= التنسيقات (Styles & Helpers) =================

    private JPanel createStyledPanel(String title) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(panelDark);
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(purpleBorder), title);
        border.setTitleColor(purpleBorder);
        p.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(5, 5, 5, 5)));
        return p;
    }

    private JList<String> createStyledList(DefaultListModel<String> model) {
        JList<String> list = new JList<>(model);
        list.setBackground(panelDark); list.setForeground(Color.WHITE);
        list.setSelectionBackground(accentColor);
        return list;
    }

    private JButton createStyledButton(String text) {
        JButton b = new JButton(text); b.setBackground(accentColor);
        b.setForeground(Color.WHITE); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel createStatusLabel(String text, Color c) {
        JLabel l = new JLabel(text); l.setForeground(c);
        l.setBorder(new EmptyBorder(2, 5, 2, 5));
        return l;
    }
private JPanel createRoomsPanel() {
    JPanel p = createStyledPanel("CHAT ROOMS");

    DefaultListModel<String> m = new DefaultListModel<>();
    m.addElement("General");
    m.addElement("Networks");
    m.addElement("Java");

    roomList = createStyledList(m);

    roomList.addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) {

String selectedRoom = roomList.getSelectedValue();

currentRoom = selectedRoom;   // 🔥🔥 هذا أهم سطر ناقص

out.println("JOIN " + selectedRoom);

chatArea.setText("");
            // إرسال اسم الغرفة بدون إيموجي
         
        }
    });

    p.add(new JScrollPane(roomList));
    return p;
}

    private JPanel createNotificationPanel() {
        JPanel p = createStyledPanel("NOTIFICATIONS");
        notificationArea = new JTextArea();
        notificationArea.setBackground(new Color(10, 5, 20));
        notificationArea.setForeground(new Color(180, 150, 255));
        notificationArea.setEditable(false);
        p.add(new JScrollPane(notificationArea));
        return p;
    }

    private JPanel createChatDisplayPanel() {
        JPanel p = createStyledPanel("CHAT MESSAGES");
        chatArea = new JTextArea(); chatArea.setBackground(new Color(20, 15, 30));
        chatArea.setForeground(Color.WHITE); chatArea.setEditable(false);
        p.add(new JScrollPane(chatArea));
        return p;
    }

    private JPanel createChatInputPanel() {
        JPanel p = createStyledPanel("CHAT INPUT");
        messageField = new JTextField(); messageField.setBackground(new Color(40, 30, 60));
        messageField.setForeground(Color.WHITE); messageField.setCaretColor(Color.WHITE);
        JButton btn = createStyledButton("Send Message");
        btn.addActionListener(e -> {   
          if (currentRoom != null && !messageField.getText().trim().isEmpty()){
out.println("MSG " + currentRoom + " " + messageField.getText());
        messageField.setText("");  }});
        
        p.add(messageField, BorderLayout.CENTER); p.add(btn, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(0, 80));
        return p;
    }

    private JPanel createStatusPanel() {
        JPanel p = createStyledPanel("USER STATUS");
        JPanel c = new JPanel(new GridLayout(3, 1)); c.setOpaque(false);
        c.add(createStatusLabel("● Active", Color.GREEN));
        c.add(createStatusLabel("● Busy", Color.ORANGE));
        c.add(createStatusLabel("● Away", Color.GRAY));
        p.add(c);
        return p;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UltraChatUI().setVisible(true));
    }
}

// ================= نافذة الدردشة الخاصة (Private Window Class) =================

class PrivateChatWindow extends JFrame {
    JTextArea area;
    JTextField input;
    PrintWriter out;
    String target;

    public PrivateChatWindow(String target, PrintWriter out) {
        this.target = target;
        this.out = out;

        setTitle("Secure Line: " + target);
        setSize(350, 450);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(25, 15, 40));

        area = new JTextArea();
        area.setEditable(false);
        area.setBackground(new Color(15, 10, 25));
        area.setForeground(Color.CYAN);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        input = new JTextField();
        input.setBackground(new Color(40, 30, 60));
        input.setForeground(Color.WHITE);
        
        JButton send = new JButton("Send");
        send.setBackground(new Color(110, 60, 180));
        send.setForeground(Color.WHITE);

        send.addActionListener(e -> {
            String msg = input.getText().trim();
            if (!msg.isEmpty()) {
                out.println("PM " + target + " " + msg);
                area.append("Me: " + msg + "\n");
                input.setText("");
            }
        });

        add(new JScrollPane(area), BorderLayout.CENTER);
        JPanel btm = new JPanel(new BorderLayout(5, 5));
        btm.setOpaque(false); btm.add(input, BorderLayout.CENTER); btm.add(send, BorderLayout.EAST);
        btm.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(btm, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}