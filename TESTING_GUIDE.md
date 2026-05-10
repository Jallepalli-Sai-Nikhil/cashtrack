# 🚀 CashTrack Enterprise Testing Guide

This guide provides a step-by-step workflow to test the entire multi-module gRPC system.

**Server Address:** `grpc://localhost:9095` (Plaintext)

---

## 🛠️ Global Setup (Metadata)
For all requests **except Auth**, you must add a Header in Insomnia:
- **Key:** `Authorization`
- **Value:** `Bearer <your_token>`

---

## 🏦 1. Role: BANK_ADMIN (The Manager)
**Goal:** Setup the system, create accounts, and manage hardware.

1.  **Register:** Call `AuthService/Register` with roles `["BANK_ADMIN"]`.
2.  **Login:** Call `AuthService/Login` to get your **Admin Token**.
3.  **Create ATM:** Call `MachineService/RegisterATM` (Returns `atmId`).
4.  **Create Customer Account:** Call `AccountService/CreateAccount` (Returns `accountId`).
5.  **Load Cash:** Call `MachineService/LoadCash` using the `atmId`.
6.  **Link Card:** Call `AccountService/LinkATMCard` using the `accountId` and a dummy card number (e.g., `1234-5678`).

---

## 👤 2. Role: CUSTOMER (The User)
**Goal:** Use the ATM, check balance, and move money.

1.  **Register:** Call `AuthService/Register` with roles `["CUSTOMER"]`.
2.  **Login:** Call `AuthService/Login` to get your **Customer Token**.
3.  **Check Balance:** Call `BalanceService/GetBalance` using your `accountId`.
4.  **Start Session:** Call `SessionService/InitiateSession` with your `atmId` and `cardNumber`.
5.  **Validate PIN:** Call `SessionService/ValidatePIN` (Returns a session token).
6.  **Transfer:** Call `TransferService/InitiateTransfer` to move money to another account.

---

## 🤖 3. Role: ATM_MACHINE (The Hardware)
**Goal:** Handle physical cash movements and system alerts.

1.  **Register:** Call `AuthService/Register` with roles `["ATM_MACHINE"]`.
2.  **Login:** Call `AuthService/Login` to get your **Machine Token**.
3.  **Dispense Cash:** Call `WithdrawalService/DispenseCash` (Simulates physical bills coming out).
4.  **Accept Cash:** Call `DepositService/AcceptCash` (Simulates bill validator).
5.  **Report Failure:** Call `MachineService/ReportATMFailure` if a jam is detected.

---

## ⚖️ 4. Role: AUDITOR (The Inspector)
**Goal:** Review system health and detect fraud.

1.  **Register:** Call `AuthService/Register` with roles `["AUDITOR"]`.
2.  **Login:** Call `AuthService/Login` to get your **Auditor Token**.
3.  **Detect Fraud:** Call `FraudService/DetectFraudulentTransaction`.
4.  **Reconcile:** Call `ReconciliationService/ReconcileTransactions` for today's date.
5.  **Get Analytics:** Call `AnalyticsService/GetTransactionAnalytics`.

---

## 🔄 5. General Maintenance (Any Role)
1.  **Validate:** Use `AuthService/ValidateToken` to check if your current token is still good.
2.  **Refresh:** Use `AuthService/RefreshToken` if your token is about to expire.
3.  **Logout:** Use `AuthService/Logout` to end your session.

---

## 💡 Troubleshooting
- **Permission Denied?** You are using the wrong token for that service. (e.g., trying to Create Account with an ATM token).
- **Unauthenticated?** Your token has expired or the `Bearer ` prefix is missing in the Header.
- **Connection Refused?** Ensure the server is running on port `9095`.
