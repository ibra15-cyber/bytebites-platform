# ByteBites Microservices Platform

A comprehensive food delivery platform built with Spring Boot microservices architecture, featuring JWT authentication, role-based access control, and event-driven communication.

## 🏗️ Architecture Overview

The platform consists of 7 microservices orchestrated through service discovery and API gateway:

- **discovery-server** (Eureka) - Service registry and discovery
- **config-server** - Centralized configuration management
- **api-gateway** - Request routing and JWT validation
- **auth-service** - Authentication and authorization
- **restaurant-service** - Restaurant and menu management
- **order-service** - Order processing and management
- **notification-service** - Event-driven notifications

## 📋 Prerequisites

- Java 11 or higher
- Docker and Docker Compose
- Git
- Postman (for API testing)

## 🚀 Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/bytebites-platform.git
cd bytebites-platform
```

### 2. Environment Setup
Ensure the following ports are available:
- 8761: Eureka Discovery Server
- 8888: Config Server
- 8080: API Gateway
- 8081: Auth Service
- 8082: Restaurant Service
- 8083: Order Service
- 8084: Notification Service
- 9092: Kafka (if using Kafka)
- 5672: RabbitMQ (if using RabbitMQ)

### 3. Service Startup Order ⚡

**CRITICAL**: Services must be started in the correct order due to dependencies:

```bash
# Step 1: Start Infrastructure (Message Broker)
docker-compose up -d kafka  # or rabbitmq
# Wait 30 seconds for Kafka to fully initialize

# Step 2: Start Discovery Server
docker-compose up -d discovery-server
# Wait 30 seconds for Eureka to start

# Step 3: Start Config Server
docker-compose up -d config-server
# Wait 15 seconds for config server to register

# Step 4: Start Auth Service
docker-compose up -d auth-service
# Wait 15 seconds for auth service to register

# Step 5: Start API Gateway
docker-compose up -d api-gateway
# Wait 15 seconds for gateway to register

# Step 6: Start Business Services
docker-compose up -d restaurant-service order-service notification-service
```

### 4. Alternative: Start All Services
```bash
# Start all services (less reliable but faster)
docker-compose up -d

# Monitor logs to ensure proper startup
docker-compose logs -f
```

### 5. Verify Services
Check Eureka Dashboard: http://localhost:8761
All services should be registered and UP.

## 🧪 Testing Each Flow

### Authentication Flow

#### 1. User Registration
```bash
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "CUSTOMER"
}
```

#### 2. User Login
```bash
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "john.doe",
  "password": "password123"
}

# Response includes JWT token
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "john.doe",
  "roles": ["ROLE_CUSTOMER"]
}
```

### Restaurant Management Flow (Restaurant Owner)

#### 1. Create Restaurant Owner Account
```bash
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "username": "restaurant.owner",
  "email": "owner@restaurant.com",
  "password": "password123",
  "role": "RESTAURANT_OWNER"
}
```

#### 2. Create Restaurant
```bash
POST http://localhost:8080/restaurants
Authorization: Bearer [JWT_TOKEN]
Content-Type: application/json

{
  "name": "Mario's Pizza",
  "address": "123 Main St",
  "cuisine": "Italian",
  "description": "Authentic Italian pizza"
}
```

#### 3. Add Menu Items
```bash
POST http://localhost:8080/restaurants/{restaurantId}/menu
Authorization: Bearer [JWT_TOKEN]
Content-Type: application/json

{
  "name": "Margherita Pizza",
  "description": "Classic tomato and mozzarella",
  "price": 12.99,
  "category": "PIZZA"
}
```

### Order Management Flow (Customer)

#### 1. Browse Restaurants
```bash
GET http://localhost:8080/restaurants
Authorization: Bearer [JWT_TOKEN]
```

#### 2. Place Order
```bash
POST http://localhost:8080/orders
Authorization: Bearer [JWT_TOKEN]
Content-Type: application/json

{
  "restaurantId": 1,
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2
    }
  ],
  "deliveryAddress": "456 Oak Ave"
}
```

#### 3. View Order History
```bash
GET http://localhost:8080/orders/my-orders
Authorization: Bearer [JWT_TOKEN]
```

### Admin Flow

#### 1. View All Users (Admin Only)
```bash
GET http://localhost:8080/auth/users
Authorization: Bearer [ADMIN_JWT_TOKEN]
```

## 🔐 JWT Testing with Postman

### 1. Import Collection
Download the [ByteBites Postman Collection](./postman/ByteBites-API-Collection.json)

### 2. Environment Setup
Create a Postman environment with:
- `base_url`: http://localhost:8080
- `jwt_token`: (will be set automatically after login)

### 3. Authentication Setup
1. Create a login request
2. Add to Tests tab:
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    pm.environment.set("jwt_token", response.token);
}
```

### 4. Using JWT in Requests
For all protected endpoints:
- Authorization tab → Bearer Token
- Token: `{{jwt_token}}`

### 5. Test Scenarios
- **Customer Journey**: Register → Login → Browse → Order
- **Restaurant Owner Journey**: Register → Login → Create Restaurant → Manage Menu → View Orders
- **Admin Journey**: Login → Manage Users → View All Data

## 📊 Service Startup Order

### Critical Dependencies
```
Message Broker (Kafka/RabbitMQ)
    ↓
Discovery Server (Eureka)
    ↓
Config Server
    ↓
Auth Service
    ↓
API Gateway
    ↓
Business Services (Restaurant, Order, Notification)
```

### Health Check Commands
```bash
# Check service health
curl http://localhost:8761/actuator/health  # Discovery Server
curl http://localhost:8888/actuator/health  # Config Server
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Restaurant Service
curl http://localhost:8083/actuator/health  # Order Service
curl http://localhost:8084/actuator/health  # Notification Service
```

## 🔗 Swagger UI Links

- **API Gateway**: http://localhost:8080/swagger-ui.html
- **Auth Service**: http://localhost:8081/swagger-ui.html
- **Restaurant Service**: http://localhost:8082/swagger-ui.html
- **Order Service**: http://localhost:8083/swagger-ui.html
- **Notification Service**: http://localhost:8084/swagger-ui.html

## 🎯 Role-Based Access Control (RBAC)

### Customer (ROLE_CUSTOMER)
- ✅ Browse restaurants and menus
- ✅ Place orders
- ✅ View own order history
- ❌ Create/modify restaurants
- ❌ View other users' orders

### Restaurant Owner (ROLE_RESTAURANT_OWNER)
- ✅ Create and manage own restaurants
- ✅ Manage own restaurant menus
- ✅ View orders for own restaurants
- ❌ View other restaurants' data
- ❌ Access user management

### Admin (ROLE_ADMIN)
- ✅ Full access to all endpoints
- ✅ User management
- ✅ View all restaurants and orders
- ✅ System administration

## 🔄 Event-Driven Architecture

### Message Flow
1. **Order Placed** → `OrderPlacedEvent` published to Kafka/RabbitMQ
2. **Notification Service** → Consumes event, sends notification
3. **Restaurant Service** → Consumes event, updates order status

### Event Structure
```json
{
  "eventType": "ORDER_PLACED",
  "orderId": 123,
  "customerId": 456,
  "restaurantId": 789,
  "timestamp": "2024-01-15T10:30:00Z",
  "totalAmount": 25.99
}
```

## 🐛 Troubleshooting

### Common Issues

#### Services Not Registering with Eureka
- Check if Discovery Server is running first
- Verify network connectivity between containers
- Check logs: `docker-compose logs discovery-server`

#### JWT Token Invalid
- Ensure auth-service is running and healthy
- Check token expiration (default: 24 hours)
- Verify JWT secret configuration

#### Database Connection Issues
- Ensure H2/PostgreSQL is properly configured
- Check database URL in config files
- Verify database container is running

#### Message Broker Connection Issues
- Ensure Kafka/RabbitMQ is running before starting services
- Check broker health: `docker-compose logs kafka`
- Verify connection configuration

### Useful Commands
```bash
# View all running services
docker-compose ps

# Restart specific service
docker-compose restart [service-name]

# View service logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down

# Remove all containers and volumes
docker-compose down -v
```

## 📈 Performance Considerations

- **Service Discovery**: Services register every 30 seconds
- **JWT Expiration**: Default 24 hours (configurable)
- **Database**: H2 in-memory for development, PostgreSQL for production
- **Message Broker**: Kafka for high throughput, RabbitMQ for reliability

## 🎬 Demo Script

### 1. System Overview (2 minutes)
- Show Eureka dashboard with all services
- Demonstrate service discovery
- Show Swagger UI links

### 2. Authentication Flow (3 minutes)
- Register different user types
- Login and obtain JWT tokens
- Show token structure and claims

### 3. Business Logic Demo (5 minutes)
- Restaurant owner creates restaurant and menu
- Customer browses and places order
- Show RBAC in action

### 4. Event-Driven Architecture (3 minutes)
- Place order and show event flow
- Demonstrate notification service
- Show restaurant service update

### 5. Monitoring and Admin (2 minutes)
- Admin user management
- System health checks
- Performance metrics

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review service logs
3. Verify service startup order
4. Check Eureka dashboard for service health

---

**Built with ❤️ using Spring Boot Microservices Architecture**