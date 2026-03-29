# Ultra Chat System 💬

Ultra Chat System is a client-server chat application developed using Java.  
The system allows multiple users to communicate in real-time through a centralized server with support for chat rooms, private messaging, and user management.

## 🚀 Features

- 🔹 Multi-client chat using TCP Socket Programming  
- 🔹 Real-time messaging between users  
- 🔹 Support for multiple chat rooms (General, Networks, Java)  
- 🔹 Private messaging between users  
- 🔹 Server-side user management (create, delete, reset password)  
- 🔹 Live monitoring of connected users and their statuses  
- 🔹 Message statistics (sent, received, archived)  
- 🔹 System logs with filtering and saving options  
- 🔹 Modern GUI built with Java Swing  
- 🔹 Cross-network communication using ZeroTier virtual network  

## 🖥️ Technologies Used

- Java (Core Java, Swing)
- TCP Sockets
- Multithreading
- File Handling
- ZeroTier (for virtual networking)

## ⚙️ How It Works

- The server handles all client connections and manages communication between users.
- Each client connects to the server using its IP address and port.
- Messages are routed based on chat rooms or private messaging requests.
- The server tracks user activity, statistics, and system logs.

## 🌐 Networking

This project supports communication between different devices using:
- Localhost (same machine)
- ZeroTier (for different networks)

## 📂 Project Structure

- `ServerUI.java` → Server interface and management  
- `ClientHandler.java` → Handles each connected client  
- `UltraChatUI.java` → Client user interface  
- `users.txt` → Stores registered users  

## 🧪 How to Run

1. Run the server:

2. Run the client:
   
3. Enter server IP and start chatting 🎉

## 📸 Screenshots
<img width="1348" height="728" alt="Screenshot 2026-03-29 183156" src="https://github.com/user-attachments/assets/aa647cbe-1220-4ae1-98c1-b5a335567441" />
<img width="1360" height="878" alt="Screenshot 2026-03-29 141923" src="https://github.com/user-attachments/assets/73ec7055-a967-44ee-9d21-2301d985109d" />


## 👩‍💻 Author

Developed as part of a networking course project.
