package Network;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.*;

public class ServerUI extends JFrame {

    // --- الألوان ---
    private final Color BG_DARK = new Color(13, 5, 25);
    private final Color ACCENT_PURPLE = new Color(110, 38, 158);
    private final Color TEXT_COLOR = new Color(200, 180, 220);
    private final Color PANEL_BG = new Color(18, 10, 31);

    // --- العناصر ---
    private ServerSocket serverSocket;
    private boolean running = false;
    private JTextArea logArea;
    private DefaultTableModel usersTableModel;
    private DefaultTableModel statsModel;
    private JTable usersTable;
    private long startTime;
    private JLabel statusLbl;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private List<String> allLogs = new ArrayList<>();
    public static Vector<ClientHandler> clients = new Vector<>();
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
private int maxLogSizeKB = 64;
private String currentFilter = "All";
    public ServerUI() {
        setTitle("ChatLite Server Console");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));

        initTopStatus();

        JPanel mainGrid = new JPanel(new GridLayout(1, 3, 10, 10));
        mainGrid.setBackground(BG_DARK);
        mainGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        userListModel = new DefaultListModel<>();
        loadUsersFromFile();

        mainGrid.add(createColumn1()); // User Management
        mainGrid.add(createColumn2()); // Active Sessions (تم تكبيرها بحذف midLogs)
        mainGrid.add(createColumn3()); // Statistics & Settings

        add(mainGrid, BorderLayout.CENTER);
        initBottomLogs();

        setVisible(true);
startServer();
    }

    private void initTopStatus() {
        JPanel topStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        topStatusPanel.setBackground(BG_DARK);
        topStatusPanel.setBorder(new LineBorder(ACCENT_PURPLE, 1));
        
        statusLbl = new JLabel("SERVER STATUS: ● ONLINE | Port: 6789 | Uptime: 00:00:00");
        statusLbl.setForeground(new Color(0, 255, 150));
        statusLbl.setFont(new Font("Monospaced", Font.BOLD, 13));
        topStatusPanel.add(statusLbl);
        add(topStatusPanel, BorderLayout.NORTH);

        startTime = System.currentTimeMillis();
        new javax.swing.Timer(1000, e -> {
            long seconds = (System.currentTimeMillis() - startTime) / 1000;
            statusLbl.setText(String.format("SERVER STATUS: ● ONLINE | Port: 6789 | Uptime: %02d:%02d:%02d", 
                seconds / 3600, (seconds % 3600) / 60, seconds % 60));
        }).start();
    }

    private JPanel createColumn1() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(BG_DARK);
        
        JPanel userMgmt = createSection("USER MANAGEMENT");
userMgmt.setLayout(new GridLayout(0, 1, 5, 5));        usernameField = new JTextField();
        passwordField = new JPasswordField();
        styleComponent(usernameField);
        styleComponent(passwordField);
        userMgmt.add(new JLabel("Username:") {{ setForeground(TEXT_COLOR); }});
        userMgmt.add(usernameField);
        userMgmt.add(new JLabel("Password:") {{ setForeground(TEXT_COLOR); }});
        userMgmt.add(passwordField);
        
        JButton createBtn = createStyledButton("Create User");
        createBtn.addActionListener(e -> createUserAction());
        userMgmt.add(createBtn);

JButton loadBtn = createStyledButton("Load Users");
loadBtn.addActionListener(e -> loadUsersFromFile());

userMgmt.add(createBtn);
userMgmt.add(loadBtn);

        JPanel existingUsers = createSection("Existing Users:");
        userList = new JList<>(userListModel);
        styleComponent(userList);
        existingUsers.add(new JScrollPane(userList), BorderLayout.CENTER);
        
        JPanel userActions = new JPanel(new GridLayout(1, 2, 5, 5));
        userActions.setBackground(PANEL_BG);
        JButton deleteBtn = createStyledButton("Delete Selected");
        JButton resetBtn = createStyledButton("Reset Password");
        
        deleteBtn.addActionListener(e -> deleteUserAction());
        resetBtn.addActionListener(e -> resetPasswordAction());

        userActions.add(deleteBtn);
        userActions.add(resetBtn);
        
        col.add(userMgmt);
        col.add(Box.createVerticalStrut(10));
        col.add(existingUsers);
        col.add(userActions);
        return col;
    }

    private JPanel createColumn2() {
        JPanel col = new JPanel();
        col.setLayout(new BorderLayout(0, 10)); // استخدام BorderLayout لاستغلال المساحة كاملة
        col.setBackground(BG_DARK);

        JPanel activeSessions = createSection("ACTIVE SESSIONS & ROSTER");
        usersTableModel = new DefaultTableModel(new String[]{"Username", "Status", "IP Address"}, 0);
        usersTable = new JTable(usersTableModel);
        styleTable(usersTable);
        activeSessions.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        
        JPanel sessionBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sessionBtns.setBackground(PANEL_BG);
        JButton kickBtn = createStyledButton("Kick User");
        JTextField broadcastField = new JTextField(10);
        JButton broadcastBtn = createStyledButton("Send");
        styleComponent(broadcastField);

        kickBtn.addActionListener(e -> kickUserAction());
        broadcastBtn.addActionListener(e -> broadcastAction(broadcastField));

        sessionBtns.add(kickBtn);
        sessionBtns.add(broadcastField);
        sessionBtns.add(broadcastBtn);
        activeSessions.add(sessionBtns, BorderLayout.SOUTH);

        col.add(activeSessions, BorderLayout.CENTER);
        return col;
        
    }

    private JPanel createColumn3() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(BG_DARK);

        JPanel stats = createSection("MAILBOX STATISTICS");
        statsModel = new DefaultTableModel(new String[]{"User", "In", "Out", "Arch"}, 0);
        JTable statsTable = new JTable(statsModel);
        styleTable(statsTable);
        JButton cleanupBtn = createStyledButton("Force Cleanup Now");
        cleanupBtn.addActionListener(e -> {
            for (ClientHandler c : clients) c.archiveCount = 0;
            updateLog("[CLEANUP DONE]");
            refreshStats();
        });
        stats.add(new JScrollPane(statsTable), BorderLayout.CENTER);
        stats.add(cleanupBtn, BorderLayout.SOUTH);

        JPanel settingsSection = createSection("SYSTEM LOGS SETTINGS");
        JPanel settingsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        settingsPanel.setBackground(PANEL_BG);

        JPanel sizeRow = createInputRowForSettings("Max Size:");
        JComboBox<String> sizeCombo = new JComboBox<>(new String[]{"64 KB", "128 KB", "256 KB"});
        styleComboBox(sizeCombo);
        sizeRow.add(sizeCombo, BorderLayout.CENTER);

        JPanel filterRow = createInputRowForSettings("Filter:");
        JComboBox<String> filterCombo = new JComboBox<>(new String[]{"All", "Info", "Error"});
        styleComboBox(filterCombo);
        filterRow.add(filterCombo, BorderLayout.CENTER);
        
        // ربط الفلتر (تعديل رقم 7)
        filterCombo.addActionListener(e -> filterLogs(filterCombo.getSelectedItem().toString()));

        JButton applyBtn = createStyledButton("Apply Settings");
        JButton clearTopBtn = createStyledButton("Clear Logs");
applyBtn.addActionListener(e -> {
    String size = sizeCombo.getSelectedItem().toString();

    if (size.contains("64")) maxLogSizeKB = 64;
    else if (size.contains("128")) maxLogSizeKB = 128;
    else if (size.contains("256")) maxLogSizeKB = 256;

    currentFilter = filterCombo.getSelectedItem().toString();

    updateLog("[SETTINGS APPLIED] Size=" + maxLogSizeKB + "KB, Filter=" + currentFilter);

    applyLogLimit();
    filterLogs(currentFilter);
});        clearTopBtn.addActionListener(e -> { logArea.setText(""); allLogs.clear(); });

        settingsPanel.add(sizeRow);
        settingsPanel.add(filterRow);
        settingsPanel.add(applyBtn);
        settingsPanel.add(clearTopBtn);
        settingsSection.add(settingsPanel, BorderLayout.CENTER);

        col.add(stats);
        col.add(Box.createVerticalStrut(10));
        col.add(settingsSection);
        return col;
    }

    private void initBottomLogs() {
        logArea = new JTextArea(5, 20);
        styleComponent(logArea);
        logArea.setEditable(false);

        JPanel bottomWrapper = createSection("SYSTEM LOGS (Live Stream)");
        bottomWrapper.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel logControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logControls.setBackground(PANEL_BG);

        JButton clearBtn = createStyledButton("Clear Logs");
        clearBtn.addActionListener(e -> { logArea.setText(""); allLogs.clear(); });

        JButton saveBtn = createStyledButton("Save Logs to .txt");
        saveBtn.addActionListener(e -> saveLogsToFile());

        logControls.add(clearBtn);
        logControls.add(saveBtn);
        bottomWrapper.add(logControls, BorderLayout.SOUTH);
        add(bottomWrapper, BorderLayout.SOUTH);
    }

    // --- منطق الأزرار مع تحديث الملفات (تعديلات 3 و 4) ---
    private void createUserAction() {
        String u = usernameField.getText();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) return;
        userListModel.addElement(u);
        try (FileWriter fw = new FileWriter("users.txt", true)) {
            fw.write(u + ":" + p + "\n");
            updateLog("[NEW USER] " + u);
        } catch (Exception ex) { ex.printStackTrace(); }
        usernameField.setText(""); passwordField.setText("");
    }

    private void deleteUserAction() {
        String selected = userList.getSelectedValue();
        if (selected == null) return;
        userListModel.removeElement(selected);
        updateLog("[DELETE USER] " + selected);
        try {
            List<String> lines = Files.readAllLines(Paths.get("users.txt"));
            List<String> updated = new ArrayList<>();
            for (String line : lines) if (!line.startsWith(selected + ":")) updated.add(line);
            Files.write(Paths.get("users.txt"), updated);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void resetPasswordAction() {
        String selected = userList.getSelectedValue();
        if (selected == null) return;
        String newPass = JOptionPane.showInputDialog(this, "New password for " + selected);
        if (newPass == null || newPass.isEmpty()) return;
        try {
            List<String> lines = Files.readAllLines(Paths.get("users.txt"));
            List<String> updated = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith(selected + ":")) updated.add(selected + ":" + newPass);
                else updated.add(line);
            }
            Files.write(Paths.get("users.txt"), updated);
            updateLog("[RESET PASSWORD] " + selected);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void kickUserAction() {
        int row = usersTable.getSelectedRow();
        if (row == -1) return;
        String username = usersTableModel.getValueAt(row, 0).toString();
        for (ClientHandler c : clients) {
            if (c.clientName != null && c.clientName.equals(username)) {
                c.kick(); 
                updateLog("[KICK] " + username);
                break;
            }
        }
    }

    private void broadcastAction(JTextField field) {
        String msg = field.getText();
        if (msg.isEmpty()) return;
        for (ClientHandler c : clients) c.sendMessage("[SERVER]: " + msg);
        updateLog("[BROADCAST] " + msg);
        field.setText("");
    }

    private void saveLogsToFile() {
        try (FileWriter writer = new FileWriter("logs.txt")) {
            writer.write(logArea.getText());
            JOptionPane.showMessageDialog(this, "Saved!");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void filterLogs(String filter) {
        logArea.setText("");
        for (String log : allLogs) {
            if (filter.equals("All") || log.contains(filter)) logArea.append(log + "\n");
        }
    }

    // --- الدوال المساعدة للتصميم ---
    private JPanel createSection(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        TitledBorder b = BorderFactory.createTitledBorder(new LineBorder(ACCENT_PURPLE, 1), title);
        b.setTitleColor(ACCENT_PURPLE);
        p.setBorder(b);
        return p;
    }

    private JPanel createInputRowForSettings(String labelText) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(PANEL_BG);
        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(TEXT_COLOR);
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private void styleComboBox(JComboBox<String> box) {
        box.setBackground(PANEL_BG);
        box.setForeground(TEXT_COLOR);
        box.setBorder(new LineBorder(ACCENT_PURPLE, 1));
    }

    private void styleTable(JTable table) {
        table.setBackground(PANEL_BG);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(ACCENT_PURPLE);
        table.getTableHeader().setBackground(BG_DARK);
        table.getTableHeader().setForeground(ACCENT_PURPLE);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_DARK);
        btn.setForeground(TEXT_COLOR);
        btn.setBorder(new LineBorder(ACCENT_PURPLE, 1));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleComponent(JComponent c) {
        c.setBackground(new Color(10, 5, 20));
        c.setForeground(TEXT_COLOR);
        c.setBorder(new LineBorder(ACCENT_PURPLE, 1));
    }

  public void updateLog(String msg) {
    SwingUtilities.invokeLater(() -> {
        allLogs.add(msg);

        applyLogLimit(); // 🔥 مهم

        if (currentFilter.equals("All") || msg.contains(currentFilter)) {
            logArea.append(msg + "\n");
        }
    });
}

    // تحديث الجداول (تعديلات 5 و 6)
    public void refreshUsers() {
        SwingUtilities.invokeLater(() -> {
            usersTableModel.setRowCount(0);
            for (ClientHandler c : clients) {
                if (c.clientName != null) 
usersTableModel.addRow(new Object[]{
    c.clientName,
    c.status.equals("ONLINE") ? "🟢 ONLINE" : "🔴 OFFLINE",
    c.getIP()
});            }
        });
    }

    public void refreshStats() {
        SwingUtilities.invokeLater(() -> {
            statsModel.setRowCount(0);
            for (ClientHandler c : clients) {
                if (c.clientName != null)
                    statsModel.addRow(new Object[]{c.clientName, c.receivedCount, c.sentCount, c.archiveCount});
            }
        });
    }

   private void loadUsersFromFile() {
    try {
        userListModel.clear(); // 🔥 مهم

        Path path = Paths.get("users.txt");
        if (!Files.exists(path)) Files.createFile(path);

        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            userListModel.addElement(line.split(":")[0]);
        }

        updateLog("[USERS LOADED]"); // 👈 للتأكيد

    } catch (Exception e) {
        e.printStackTrace();
    }
}
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(6789);
                running = true;
                updateLog("[SYSTEM] Server Online on 6789");
                while (running) {
                    Socket socket = serverSocket.accept();
                    ClientHandler client = new ClientHandler(socket, this);
                    clients.add(client);
                    client.start();
                }
            } catch (Exception e) { updateLog("[ERROR] " + e.getMessage()); }
        }).start();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception e) {}
        new ServerUI();
    }
    private void applyLogLimit() {
    int maxLines = maxLogSizeKB; // تقريب بسيط (1KB ≈ 1 line)

    while (allLogs.size() > maxLines) {
        allLogs.remove(0); // احذف أقدم log
    }

    logArea.setText("");
    for (String log : allLogs) {
        logArea.append(log + "\n");
    }
}
   
}