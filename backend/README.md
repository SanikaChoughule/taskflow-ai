# TaskFlow AI - MySQL Backend Server

Node.js Express backend that connects to your **MySQL database** and provides **multi-user task and schedule isolation** for TaskFlow AI.

---

## 1. Quick Setup Guide

### Step 1: Install Dependencies
```bash
npm install
```

### Step 2: Set Up MySQL Database
Run the SQL schema script provided in the project root:
```bash
mysql -u root -p < ../mysql_schema.sql
```
*(Or open `mysql_schema.sql` in phpMyAdmin/Workbench and execute it)*

### Step 3: Configure Environment
Edit `.env` with your MySQL credentials:
```env
PORT=3000
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=rohan1234
DB_NAME=taskflow_db
```

### Step 4: Start the Server
```bash
npm start
```
You should see:
```
TaskFlow Backend running at http://0.0.0.0:3000
Accepting connections from mobile devices on local Wi-Fi!
```

---

## 2. Connecting the Android App

In `local.properties`:
```properties
BACKEND_BASE_URL=http://192.168.0.106:3000/api/v1/
```
