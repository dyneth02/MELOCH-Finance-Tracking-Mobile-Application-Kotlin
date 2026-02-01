# Meloch – Personal Finance Tracker

<p align="center">
  <img src="Screenshots/Screenshot_20250422_153410_com.example.meloch.jpg" width="200" alt="Meloch Splash">
  <img src="Screenshots/Screenshot_20250422_153518_com.example.meloch.jpg" width="200" alt="Dashboard">
  <img src="Screenshots/Screenshot_20250422_153748_com.example.meloch.jpg" width="200" alt="Add Record">
</p>

**Meloch** is a modern, intuitive Android application built with Kotlin, designed to help users take full control of their personal finances. With a clean UI, secure per‑user data isolation, and powerful features like budgeting, transaction tracking, wallet card management, and exportable reports, Meloch turns everyday financial management into a seamless experience.

---

## ✨ Features

### 🧾 **Core Functionalities**
- **User Account Management** – Secure registration/login with email-based data isolation.
- **Transaction Recording** – Add income/expense entries with category, payment method, date, and amount.
- **Budget Planning** – Set monthly budgets per category; track remaining budget with visual progress bars.
- **Wallet Card Management** – Securely store credit/debit card details (masked by default).
- **Dashboard Overview** – Real-time view of total balance, budget status, and category-wise spending.
- **Transaction History** – Chronologically grouped records with filtering by date.
- **Statistics & Charts** – Pie charts and bar graphs for income vs. expense and category breakdowns.

### 🚀 **Bonus Features**
- **Data Backup (JSON)** – Export all financial data to a portable JSON file.
- **PDF Report Generation** – Professional monthly summaries with charts and transaction tables.
- **Smart Notifications** – Alert users when budgets run low or are reset.
- **Review & Feedback** – In-app rating system with notification follow-up.

---

## 📸 Screenshots

| Login & Registration | Dashboard & Budgets | Add Record |
|----------------------|---------------------|------------|
| <img src="Screenshots/Screenshot_20250422_153440_com.example.meloch.jpg" width="200"> | <img src="Screenshots/Screenshot_20250422_153512_com.example.meloch.jpg" width="200"> | <img src="Screenshots/Screenshot_20250422_153748_com.example.meloch.jpg" width="200"> |

| Statistics & Charts | Wallet Cards | Financial Report (PDF) |
|---------------------|--------------|------------------------|
| <img src="Screenshots/Screenshot_20250422_153557_com.example.meloch.jpg" width="200"> | <img src="Screenshots/Screenshot_20250422_153650_com.example.meloch.jpg" width="200"> | <img src="Screenshots/Screenshot_20250422_154438_com.google.android.apps.docs.jpg" width="200"> |

---

## 🛠️ Technical Implementation

### **Architecture & Components**
- **Language:** Kotlin
- **Storage:** `SharedPreferences` for per‑user persistent data (transactions, budgets, balances)
- **Notifications:** Custom `NotificationHelper` with channels for budget alerts and feedback
- **Data Export:**
  - **PDF Report:** Generated with document APIs, includes charts and tables.
  - **JSON Backup:** Structured JSON output for full data portability.
- **UI:** Modern Material Design with color‑coded charts and intuitive navigation.

### **Key Code Highlights**
- `PreferenceManager.kt` – Handles all user‑specific data operations with email‑scoped keys.
- `NotificationHelper.kt` – Manages budget‑low, budget‑limit, and budget‑reset alerts.
- `JSONBackupGenerator.kt` – Creates comprehensive JSON backups of user data.
- `ProfileNotificationHelper.kt` – Sends feedback confirmation notifications.

---

## 📁 Project Structure
