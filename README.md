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