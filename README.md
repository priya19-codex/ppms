# Paddy Procurement Management System (PPMS)
> **“Smart Booking. Less Waiting. Better Procurement.”**
> *Tamil Nadu Civil Supplies Corporation (TNCSC) · Department of Food & Consumer Protection*

---

## 🌾 Overview

The **Paddy Procurement Management System (PPMS)** is an end-to-end, responsive web application designed for farmers and agricultural administration in Tamil Nadu. It modernizes the traditional paddy procurement process by allowing farmers to book delivery slots in advance, eliminate 12–24 hour direct purchase centre (DPC) vehicle queues, plan transportation, and receive digital tokens with QR verification.

Procurement-centre staff manage real-time queues and track vehicle unloading stages, while state administrators oversee farmer limits, approve extra quota requests, analyze centre utilization with visual charts, and export government reports.

---

## 🚀 Key Features

### 1. Farmer Portal
- **Registration & Auto-Quota:** Enter Farmer ID, land acres, and Aadhaar (masked as `XXXX XXXX 1234`). A provisional quota of **20 bags per acre** is automatically assigned.
- **Direct Purchase Centres:** Search and filter centres across Tamil Nadu (Thanjavur, Tiruvarur, Nagapattinam) with live operating hours, queue count, and available spaces.
- **7-Day Interactive Slot Calendar:** Visual status indicators:
  - 🟢 **AVAILABLE** (Green / Selectable)
  - 🟡 **LIMITED** (Yellow / Few slots left)
  - 🔴 **FULL** (Red / Capacity reached)
  - ⚪ **CLOSED** (Gray / Past hour)
- **Booking & Concurrency Protection:** Atomic database reservation guards (`pessimistic lock` + conditional decrement) guarantee that slots can **never be overbooked**. Bookings validate remaining quota.
- **Official Digital Token & QR Code:** Generates a unique Booking ID (e.g., `PB20261006010001`), daily Token Number (e.g., `A001`), and high-resolution QR code. Includes **Download PNG Token Card** and **Print / Save PDF**.
- **Procurement Workflow Tracker:** Real-time visual progress through the 5 official states:
  $$\text{CONFIRMED} \longrightarrow \text{ARRIVED} \longrightarrow \text{WAITING} \longrightarrow \text{UNLOADING} \longrightarrow \text{COMPLETED}$$
- **Self-Service Cancellation:** Farmers can cancel confirmed bookings up to 2 hours before the slot. Quota is restored instantly.
- **Notification & Reminder Centre:** Real-time in-app alerts on status changes and automated 24-hour / 15-minute reminders.
- **Profile & Extra Quota Requests:** View quota usage bar or submit applications for additional bags with yield justification.

### 2. Staff Portal
- **Centre-Scoped Security:** Staff are restricted to operations at their assigned procurement centre.
- **Today's Live Queue:** Live vehicle queue showing token, booking ID, farmer, vehicle number, type, quantity, and slot time.
- **Strict State Machine:** Step-by-step queue progression:
  - `CONFIRMED` ➔ `ARRIVED`
  - `ARRIVED` ➔ `WAITING` or `UNLOADING`
  - `WAITING` ➔ `UNLOADING`
  - `UNLOADING` ➔ `COMPLETED`
  - *Each transition automatically generates an instant notification for the farmer.*
- **Slot Capacity Management:** Create new slots, adjust capacities, and toggle open/closed state.

### 3. Administrator Portal
- **Executive Analytics Dashboard:** Visual KPI cards and interactive Chart.js graphs:
  - Daily bookings & completed procurement volume
  - Queue status distribution (doughnut chart)
  - Centre-wise volume comparison
  - Centre slot utilization % (next 7 days)
- **Farmer Management & Quota Revisions:** Search all farmers, toggle active/inactive status, adjust approved quotas, and review/approve/reject extra quantity requests with one click.
- **Procurement Centre & Staff Management:** Add or edit Direct Purchase Centres, set daily capacities, and create staff login credentials.
- **All Bookings Audit:** Statewide filter by date, centre, status, or farmer search.
- **Government Reports & CSV Export:**
  - Daily Procurement Summary
  - Centre-wise Aggregation
  - Completed Procurements Detailed Audit
  - Slot Utilization Analysis
  - Farmer Volume Summary
  - Single-click **CSV Download** and **Print-ready PDF** format.

---

## 🛠️ Technology Stack

- **Backend:** Java 21, Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Security, Spring Validation)
- **Database:** MySQL 8 (via MySQL Connector/J with connection pooling and transactions)
- **Security:** HTTP Basic + BCrypt password hashing, role-based access control (`FARMER`, `STAFF`, `ADMIN`)
- **Frontend:** Vanilla HTML5, modern CSS3 (Agricultural Green & Gold government palette), JavaScript (ES6+ SPA architecture, no Node.js required)
- **Visuals & Charts:** Chart.js, QRCode.js, Canvas PNG receipt generator

---

## 🔑 Demo Accounts

Use the **1-Click Demo Login** buttons on the homepage or enter these credentials:

| Role | Portal URL | Mobile Number | Password | Notes |
|---|---|---|---|---|
| **Administrator** | `/#/admin-login` | `9000000001` | `Admin@123` | Full statewide access, approvals, reports |
| **Staff (Thanjavur)** | `/#/staff-login` | `9000000002` | `Staff@123` | Thanjavur Delta Procurement Centre |
| **Staff (Tiruvarur)** | `/#/staff-login` | `9000000003` | `Staff@123` | Tiruvarur Paddy Procurement Centre |
| **Farmer (Priya Selvam)** | `/#/login` | `9876500001` | `Farmer@123` | 5 acres (100 bags limit), active bookings |
| **Farmer (Ravi Chandran)** | `/#/login` | `9876500002` | `Farmer@123` | 3 acres (60 bags limit) |
| **Farmer (Kumar Rajan)** | `/#/login` | `9876500003` | `Farmer@123` | 4 acres (80 bags limit) |
| **Farmer (Suresh Babu)** | `/#/login` | `9876500004` | `Farmer@123` | 2 acres, extra quantity request |
| **Farmer (Lakshmi Devi)** | `/#/login` | `9876500005` | `Farmer@123` | 6 acres (120 bags limit) |

---

## 🖥️ Running Locally

### Prerequisites
- JDK 17+ installed (`java -version`)
- Apache Maven 3.9+ installed (`mvn -v`)
- MySQL 8 running on port 3306

### Execution
1. Navigate to the project backend directory:
   ```powershell
   cd C:\Users\PRIYADHARSHINI\.gemini\antigravity\scratch\ppms\backend
   ```
2. Run Spring Boot:
   ```powershell
   mvn spring-boot:run
   ```
3. Open your browser and visit:
   ```
   http://localhost:8080
   ```
*All seed data (centres, slots, bookings, farmers, sample queue) is automatically initialized on startup.*

---

## 📂 Project Structure

```
ppms/
├── README.md
├── database/
│   └── schema.sql                 # Reference database schema
├── docs/
│   └── API.md                     # REST API reference documentation
└── backend/
    ├── pom.xml                    # Maven build configuration
    └── src/
        └── main/
            ├── java/com/ppms/
            │   ├── PpmsApplication.java   # Spring Boot entry point (@EnableScheduling)
            │   ├── Model.java             # JPA entities (User, Farmer, Centre, Slot, Booking, Notification, QuantityRequest)
            │   ├── Repos.java             # Spring Data repositories with pessimistic locks
            │   ├── CoreService.java       # Booking concurrency, quota logic, reminder scheduler
            │   ├── Api.java               # REST controllers (Auth, Centres, Slots, Bookings, Staff, Admin)
            │   ├── SecurityConfig.java    # Spring Security role filters and BCrypt encoder
            │   ├── Errors.java            # Global Exception Handler
            │   └── DataSeeder.java        # Initial demo records seeder
            └── resources/
                ├── application.properties # Server port & MySQL configuration
                └── static/
                    ├── index.html         # Single Page Application container
                    ├── style.css          # Agricultural / Government portal CSS
                    └── app.js             # Client router, reactive controllers, QR & Chart logic
```
