# ☀️ Solaris API

Enterprise inventory and stock management backend built with Spring Boot, PostgreSQL and JWT Authentication.

Solaris is a modern backend-focused SaaS architecture project designed to simulate real-world enterprise inventory workflows, stock tracking and business management systems.

---

## 🚀 Features

- JWT Authentication & Authorization
- Spring Security Integration
- Product Management
- Category Management
- Inventory & Stock Tracking
- Stock Movement History
- Dashboard Summary Metrics
- Search & Filtering
- Global Exception Handling
- RESTful API Architecture
- Swagger/OpenAPI Documentation

---

## 🛠️ Tech Stack

### Backend
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate

### Database
- PostgreSQL

### Authentication
- JWT (JSON Web Tokens)
- BCrypt Password Encryption

### Documentation
- Swagger / OpenAPI

### Tools
- Maven
- Git
- GitHub
- Postman

---

## 🏗️ Architecture

Solaris follows a layered enterprise architecture:

```text
Controller → Service → Repository → Database
```

Main modules:
- Authentication
- Products
- Categories
- Stock Movements
- Dashboard Analytics

---

## ⚙️ Getting Started

### Clone repository

```bash
git clone https://github.com/LuccaCbs/solaris-api.git
```

### Configure PostgreSQL

Create a PostgreSQL database:

```sql
CREATE DATABASE solaris_db;
```

### Configure application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/solaris_db
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### Run application

```bash
./mvnw spring-boot:run
```

---

## 📚 API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI Docs:

```text
http://localhost:8080/v3/api-docs
```

---

## 🧾 Fiscal invoicing (TusFacturas / AFIP)

Solaris can emit Factura B/C through [TusFacturasAPP](https://www.tusfacturas.app/api-factura-electronica-afip.html) as an AFIP intermediary.

### Environment variables

Copy `.env.example` and set at least the database and JWT values. TusFacturas endpoint override (optional):

```properties
TUSFACTURAS_BASE_URL=https://www.tusfacturas.app/app/api/v2/facturacion/nuevo
```

TusFacturas does **not** expose a separate sandbox URL. Development uses the **Plan API DEV** (free 30-day trial) against the same production endpoint; comprobantes are ficticios and CAE comes back empty.

### Organization credentials (Admin > Fiscal settings)

1. Set **CUIT**, **razón social**, **condición IVA** and **punto de venta** (must match the PDV configured in TusFacturas).
2. Choose provider **TUSFACTURAS**.
3. Paste credentials as JSON in the API key field (never commit real values):

```json
{"apikey":"YOUR_APIKEY","apitoken":"YOUR_APITOKEN","usertoken":"YOUR_USERTOKEN"}
```

If provider is TUSFACTURAS but credentials are missing or incomplete, Solaris falls back to **MOCK** (fake CAE).

### Obtaining TusFacturas Plan API DEV credentials

1. Register at [quiero-probar-api-factura-electronica](https://www.tusfacturas.app/quiero-probar-api-factura-electronica.html) (Plan API DEV, 30 days, up to 1,500 test invoices).
2. In TusFacturas: **Menú > Mi espacio de trabajo > Puntos de venta** — create a test PDV (e.g. 679) with your CUIT.
3. Copy **apikey**, **apitoken** and **usertoken** from the PDV credentials panel into the Admin fiscal JSON above.

### Test flow

1. Configure organization fiscal data and TusFacturas credentials.
2. Create a sale and emit invoice (Consumidor Final or with customer).
3. In Plan API DEV: expect `error: "N"` but **empty CAE** — comprobante is ficticio.
4. In production (CUIT linked to AFIP): expect real **CAE**, **vencimiento_cae** and **comprobante_pdf_url** (PDF URL is temporary; download and store it).

API docs: [developers.tusfacturas.app](https://developers.tusfacturas.app/como-empiezo)

---

## 📌 Roadmap

- [x] JWT Authentication
- [x] Product Management
- [x] Categories
- [x] Stock Movements
- [x] Dashboard Metrics
- [x] Swagger/OpenAPI
- [ ] Role-based permissions
- [ ] Audit Logs
- [ ] Docker Support
- [ ] React Frontend
- [ ] Deployment
- [ ] CI/CD Pipeline

---

## 👨‍💻 Author

**Lucca Vergara**

- GitHub: https://github.com/LuccaCbs
- LinkedIn: https://linkedin.com/in/lucca-vergara/

---