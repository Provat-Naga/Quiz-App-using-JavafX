# 🧠 Quiz App using JavaFX

A **Java-based Quiz Application** that allows users to take quizzes across various topics. It includes user authentication, progress tracking, leaderboards, and secure data storage — all wrapped in a responsive JavaFX UI.

---

## 📋 Project Description

This quiz application offers:

- 📚 **Multiple-choice quizzes** on various subjects  
- 📊 **Performance tracking** and feedback after each quiz  
- 🔒 **User authentication** with secure password hashing (bcrypt + salt)  
- 🧑‍💼 **User account management** using a local SQLite database  
- 🏆 **Leaderboard** showcasing top scorers and rankings  
- 🕒 **Timer-based quizzes** (optional/extension feature)  
- 🎨 A clean and intuitive interface using **JavaFX + CSS**

Robust exception handling and input validation ensure a reliable and secure user experience.

---

## 🛠️ Technology Stack

| Layer       | Technology                       |
|-------------|-----------------------------------|
| Frontend    | JavaFX (FXML), CSS                |
| Backend     | Java (JDK SE Version 24 64bit)    |
| Database    | SQLite (via DB Browser)           |
| Build Tool  | Maven                             |
| IDE         | IntelliJ IDEA (Community Edition 2024.3.5) |
| Libraries   | JDBC, ControlFX, JBCrypt, JavaFX Controls |

---

## 💻 System Requirements

- **OS**: Windows 10 Pro  
- **RAM**: 8 GB  
- **Processor**: Intel i5 2400 (2nd Gen)  
- **Java Version**: JDK SE 24 (64-bit)  
- **Maven Version**: Compatible with Java 24  
- **SQLite Browser**: [DB Browser for SQLite](https://sqlitebrowser.org/)  

---

## 🚀 How to Launch the App

1. **Clone the Repository** or download the source files.
2. Open the project using **IntelliJ IDEA**.
3. Navigate to the main class:
   ```
   QuizApp/src/main/java/javafx_app/quizapp/StartApp.java
   ```
4. Right-click on `StartApp.java` and choose **Run**.

> ⚠️ Ensure your Maven dependencies are correctly installed and JavaFX is configured.

---

## 📆 Features

- ✅ User login/signup
- ✅ Password hashing with JBCrypt
- ✅ Create/read/update/delete (CRUD) quiz questions
- ✅ Real-time score calculation
- ✅ Leaderboard ranking system
- ✅ Timer-based quiz mode
- ✅ Error handling & user input validation

---

## 🔐 Security

- **Passwords are never stored as plain text**.
- Uses **bcrypt hashing + salting** via the JBCrypt library.
- Input validation and SQL protection using **prepared statements**.

---

## 📸 UI Screenshots *(Optional)*

You can insert screenshots here to showcase:
- Login page
- Quiz UI
- Score summary
- Leaderboard

---

## 👥 Authors

- Your Name – [GitHub Profile](https://github.com/Provat-Naga)  
