API Testing Guide - Attijari Compass
=====================================

BASE URL: http://localhost:8081

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1️⃣ AUTHENTICATION ENDPOINTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔐 REGISTER (Create new account)
─────────────────────────────────
POST /api/auth/register
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "role": "USER"
}

Response (201 Created):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER"
}


🔐 LOGIN (Authenticate)
──────────────────────
POST /api/auth/login
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER"
}

⚠️ Save the token for subsequent requests!


🔄 REFRESH TOKEN (Get new access token)
───────────────────────────────────────
POST /api/auth/refresh-token
Content-Type: application/json

Request Body:
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440001",
  "email": "user@example.com",
  "role": "USER"
}


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2️⃣ USER MANAGEMENT ENDPOINTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

👤 GET CURRENT USER INFO
───────────────────────
GET /api/users/me
Authorization: Bearer {token}

Response (200 OK):
{
  "id": 1,
  "email": "user@example.com",
  "role": "USER",
  "createdAt": "2026-03-22T10:30:00Z"
}


👥 GET ALL USERS (ADMIN ONLY)
──────────────────────────────
GET /api/users
Authorization: Bearer {admin_token}

Response (200 OK):
[
  {
    "id": 1,
    "email": "user@example.com",
    "role": "USER",
    "createdAt": "2026-03-22T10:30:00Z"
  },
  {
    "id": 2,
    "email": "admin@example.com",
    "role": "ADMIN",
    "createdAt": "2026-03-22T09:00:00Z"
  }
]


✏️ UPDATE USER INFO
──────────────────
PUT /api/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

Request Body:
{
  "email": "newemail@example.com",
  "password": "NewPassword456!"
}

Response (200 OK):
{
  "id": 1,
  "email": "newemail@example.com",
  "role": "USER",
  "createdAt": "2026-03-22T10:30:00Z"
}


🗑️ DELETE USER (ADMIN ONLY)
──────────────────────────
DELETE /api/users/{id}
Authorization: Bearer {admin_token}

Response: 204 No Content


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3️⃣ TRANSACTION ENDPOINTS (NEW - FINTECH)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💳 CREATE CARD PAYMENT (Auto-categorized)
───────────────────────────────────────
POST /api/transactions/card-payment
Authorization: Bearer {token}
Content-Type: application/json

Description: 
Register a card payment. The category is automatically determined based on merchant name.

Request Body:
{
  "merchantName": "Carrefour Market",
  "amount": 45.50,
  "date": "2026-03-22",
  "description": "Weekly groceries",
  "cardLast4": "1234",
  "mcc": "5411"
}

Response (201 Created):
{
  "id": 1,
  "description": "Weekly groceries",
  "amount": 45.50,
  "date": "2026-03-22",
  "category": "ALIMENTATION",
  "type": "DEPENSE",
  "userId": 1,
  "merchantName": "Carrefour Market",
  "paymentMethod": "CARD",
  "source": "MANUAL_CARD",
  "cardLast4": "1234",
  "createdAt": "2026-03-22T14:30:00Z"
}

🎯 Example Merchant Names (auto-categorization):
- "Carrefour Market" → ALIMENTATION
- "McDonald's" → RESTAURANT
- "Uber" → TRANSPORT
- "Netflix" → LOISIRS
- "Amazon" → SHOPPING
- "SNCF Train" → TRANSPORT
- "Pharmacie" → SANTE
- "Université" → EDUCATION


📝 CREATE TRANSACTION (Old format - still works)
────────────────────────────────────────────────
POST /api/transactions
Authorization: Bearer {token}
Content-Type: application/json

Request Body:
{
  "description": "Restaurant dinner",
  "amount": 35.00,
  "date": "2026-03-22",
  "category": "RESTAURANT",
  "type": "DEPENSE"
}

Response (201 Created):
{
  "id": 2,
  "description": "Restaurant dinner",
  "amount": 35.00,
  "date": "2026-03-22",
  "category": "RESTAURANT",
  "type": "DEPENSE",
  "userId": 1,
  "paymentMethod": "CARD",
  "source": "MANUAL_ENTRY",
  "createdAt": "2026-03-22T14:45:00Z"
}


📊 GET ALL MY TRANSACTIONS
──────────────────────────
GET /api/transactions
Authorization: Bearer {token}

Response (200 OK):
[
  {
    "id": 1,
    "description": "Weekly groceries",
    "amount": 45.50,
    "date": "2026-03-22",
    "category": "ALIMENTATION",
    "type": "DEPENSE",
    "userId": 1,
    "merchantName": "Carrefour Market",
    "paymentMethod": "CARD",
    "source": "MANUAL_CARD",
    "cardLast4": "1234",
    "createdAt": "2026-03-22T14:30:00Z"
  },
  {
    "id": 2,
    "description": "Restaurant dinner",
    "amount": 35.00,
    "date": "2026-03-22",
    "category": "RESTAURANT",
    "type": "DEPENSE",
    "userId": 1,
    "paymentMethod": "CARD",
    "source": "MANUAL_ENTRY",
    "createdAt": "2026-03-22T14:45:00Z"
  }
]


🔍 GET TRANSACTION BY ID
─────────────────────
GET /api/transactions/{id}
Authorization: Bearer {token}

Example: GET /api/transactions/1

Response (200 OK):
{
  "id": 1,
  "description": "Weekly groceries",
  "amount": 45.50,
  "date": "2026-03-22",
  "category": "ALIMENTATION",
  "type": "DEPENSE",
  "userId": 1,
  "merchantName": "Carrefour Market",
  "paymentMethod": "CARD",
  "source": "MANUAL_CARD",
  "cardLast4": "1234",
  "createdAt": "2026-03-22T14:30:00Z"
}


🏷️ CORRECT TRANSACTION CATEGORY (Manual Adjustment)
─────────────────────────────────────────────────
PATCH /api/transactions/{id}/category
Authorization: Bearer {token}
Content-Type: application/json

Description:
If the auto-categorization got it wrong, you can manually correct it.

Request Body:
{
  "category": "SHOPPING"
}

Response (200 OK):
{
  "id": 1,
  "description": "Weekly groceries",
  "amount": 45.50,
  "date": "2026-03-22",
  "category": "SHOPPING",
  "type": "DEPENSE",
  "userId": 1,
  "merchantName": "Carrefour Market",
  "paymentMethod": "CARD",
  "source": "MANUAL_CARD",
  "cardLast4": "1234",
  "createdAt": "2026-03-22T14:30:00Z"
}


✏️ UPDATE TRANSACTION (Old format)
──────────────────────────────────
PUT /api/transactions/{id}
Authorization: Bearer {token}
Content-Type: application/json

Request Body:
{
  "description": "Updated description",
  "amount": 50.00,
  "date": "2026-03-23",
  "category": "ALIMENTATION",
  "type": "DEPENSE"
}

Response (200 OK):
{
  "id": 1,
  "description": "Updated description",
  "amount": 50.00,
  "date": "2026-03-23",
  "category": "ALIMENTATION",
  "type": "DEPENSE",
  "userId": 1,
  "paymentMethod": "CARD",
  "source": "MANUAL_ENTRY",
  "createdAt": "2026-03-22T14:30:00Z"
}


🗑️ DELETE TRANSACTION
──────────────────
DELETE /api/transactions/{id}
Authorization: Bearer {token}

Response: 204 No Content


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4️⃣ DASHBOARD ENDPOINTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📈 GET DASHBOARD (Financial Overview)
────────────────────────────────────
GET /api/dashboard
Authorization: Bearer {token}

Response (200 OK):
{
  "totalIncome": 2500.00,
  "totalExpenses": 1250.50,
  "balance": 1249.50,
  "savingsRate": 49.98,
  "categoryBreakdown": {
    "ALIMENTATION": 450.50,
    "RESTAURANT": 200.00,
    "TRANSPORT": 150.00,
    "LOISIRS": 250.00,
    "SHOPPING": 200.00
  },
  "thisMonthIncome": 2500.00,
  "thisMonthExpenses": 1250.50
}


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5️⃣ ERROR RESPONSES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

❌ 400 Bad Request (Validation Error)
──────────────────────────────────
{
  "timestamp": "2026-03-22T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Amount must be positive"
}


❌ 401 Unauthorized (Missing/Invalid Token)
──────────────────────────────────────────
{
  "timestamp": "2026-03-22T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized"
}


❌ 403 Forbidden (No Permission)
──────────────────────────────
{
  "timestamp": "2026-03-22T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Forbidden"
}


❌ 404 Not Found
────────────────
{
  "timestamp": "2026-03-22T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Transaction not found with id: 999"
}


❌ 409 Conflict (Email Already Exists)
──────────────────────────────────
{
  "timestamp": "2026-03-22T14:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Email already in use: user@example.com"
}


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
6️⃣ TRANSACTION CATEGORIES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Available Categories for Manual Selection:
- ALIMENTATION      (Grocery stores, markets)
- RESTAURANT        (Dining, cafes)
- TRANSPORT         (Uber, taxi, fuel, transit)
- LOGEMENT          (Rent, housing)
- SANTE             (Pharmacy, medical)
- LOISIRS           (Entertainment, streaming)
- SHOPPING          (Retail, clothes)
- EDUCATION         (School, courses)
- SALAIRE           (Salary, income)
- EPARGNE           (Savings)
- FACTURES          (Bills, utilities)
- AUTRE             (Other)


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
7️⃣ SWAGGER UI
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Interactive API Documentation:
👉 http://localhost:8081/swagger-ui/index.html

API Docs (JSON):
👉 http://localhost:8081/v3/api-docs


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
8️⃣ TESTING SEQUENCE (Step by Step)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1️⃣ Register a new user
   POST /api/auth/register
   Save the token from response

2️⃣ Get current user info
   GET /api/users/me
   (Use the token from step 1)

3️⃣ Create a card payment
   POST /api/transactions/card-payment
   (Watch auto-categorization happen!)

4️⃣ Get all transactions
   GET /api/transactions
   (See your transaction with auto-category)

5️⃣ Manually correct the category if needed
   PATCH /api/transactions/1/category
   (Send a different category)

6️⃣ View dashboard
   GET /api/dashboard
   (See financial overview)

7️⃣ Delete the transaction
   DELETE /api/transactions/1


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ API is ready to use!

