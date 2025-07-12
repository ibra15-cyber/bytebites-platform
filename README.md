
# ByteBites Microservices Platform

A comprehensive food delivery platform built with Spring Boot microservices architecture, featuring JWT authentication, role-based access control, and event-driven communication via RabbitMQ.

## 🏗️ Architecture Overview

The platform consists of 7 microservices orchestrated through service discovery and API gateway, with a shared common module for DTOs and security components:

- **discovery-server** (Eureka) - Service registry and discovery
- **config-server** - Centralized configuration management
- **api-gateway** - Request routing, JWT validation, and header extraction
- **auth-service** - Authentication and authorization
- **restaurant-service** - Restaurant and menu management
- **order-service** - Order processing and management
- **notification-service** - Event-driven email notifications
- **common-module** - Shared DTOs, JWT filters, and security configurations

## 🔧 Key Architecture Changes

### Shared Common Module
- **Common DTOs**: Standardized data transfer objects across services
- **Centralized JWT Header Filter**: Consistent token extraction and validation
- **Security Configuration**: Shared security configurations with custom entry points and access denied handlers
- **No ROLE_ Prefix**: Custom security implementation without Spring's default ROLE_ prefix

### Authentication Flow
1. **User Registration**: API Gateway → Auth Service (user account creation)
2. **User Login**: API Gateway → Auth Service (JWT token generation)
3. **Token Validation**: API Gateway validates JWT and extracts user headers
4. **Header Propagation**: Gateway forwards user context headers to downstream services
5. **Service Authorization**: Downstream services use headers for access control

### Event-Driven Architecture (RabbitMQ Only)
- **Order Placement**: Triggers order events sent to RabbitMQ exchange
- **Restaurant Service**: Consumes order events and processes orders
- **Notification Service**: Consumes events, fetches restaurant details, sends emails

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
- 5672: RabbitMQ Management
- 15672: RabbitMQ Web UI

### 3. Service Startup Order ⚡

Start your microservices in the correct order due to dependencies:

```bash
# Step 1: Start Infrastructure (RabbitMQ)
docker-compose up -d rabbitmq
# Wait 30 seconds for RabbitMQ to fully initialize

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
- **Eureka Dashboard**: http://localhost:8761
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- All services should be registered and UP.

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
  "roles": ["CUSTOMER"]
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

## 🔐 JWT and Security Architecture

### JWT Header Extraction Flow
1. **API Gateway**: Validates JWT token and extracts user information
2. **Header Propagation**: Gateway adds user context headers:
    - `X-User-Id`: User ID
    - `X-User-Role`: User role (without ROLE_ prefix)
    - `X-Username`: Username
3. **Downstream Services**: Use common security filter to extract headers and authorize requests

### Custom Security Configuration
- **No ROLE_ Prefix**: Custom role handling without Spring's default prefix
- **Centralized Security**: Shared security configurations across services
- **Custom Entry Points**: Unified authentication entry point handling
- **Access Denied Handlers**: Consistent access denied responses

### Role-Based Access Control

#### Customer (CUSTOMER)
- ✅ Browse restaurants and menus
- ✅ Place orders
- ✅ View own order history
- ❌ Create/modify restaurants
- ❌ View other users' orders

#### Restaurant Owner (RESTAURANT_OWNER)
- ✅ Create and manage own restaurants
- ✅ Manage own restaurant menus
- ✅ View orders for own restaurants
- ❌ View other restaurants' data
- ❌ Access user management

#### Admin (ADMIN)
- ✅ Full access to all endpoints
- ✅ User management
- ✅ View all restaurants and orders
- ✅ System administration

## 🔄 Event-Driven Architecture (RabbitMQ)

### Order Placement Event Flow
1. **Order Service**: Places order and publishes `OrderPlacedEvent` to RabbitMQ exchange
2. **Restaurant Service**: Consumes order event and processes/prepares order
3. **Notification Service**: Consumes order event, fetches restaurant details from Restaurant Service, sends email notification

### RabbitMQ Configuration
- **Exchange**: `order.exchange` (Topic Exchange)
- **Routing Keys**:
    - `order.placed` - Order placement events
    - `order.updated` - Order status updates
- **Queues**:
    - `restaurant.order.queue` - Restaurant service queue
    - `notification.order.queue` - Notification service queue

### Event Structure
```json
{
  "eventType": "ORDER_PLACED",
  "orderId": 123,
  "customerId": 456,
  "restaurantId": 789,
  "timestamp": "2024-01-15T10:30:00Z",
  "totalAmount": 25.99,
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2,
      "price": 12.99
    }
  ],
  "deliveryAddress": "456 Oak Ave"
}
```

## 📊 Service Dependencies

### Critical Dependencies
```
RabbitMQ
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

### Inter-Service Communication
- **Order Service → Restaurant Service**: Fetches restaurant details for order validation
- **Notification Service → Restaurant Service**: Fetches restaurant details for email notifications
- **All Services → Auth Service**: Token validation (via API Gateway)

## 🔗 Swagger UI Links

- **API Gateway**: http://localhost:8080/swagger-ui.html
- **Auth Service**: http://localhost:8081/swagger-ui.html
- **Restaurant Service**: http://localhost:8082/swagger-ui.html
- **Order Service**: http://localhost:8083/swagger-ui.html
- **Notification Service**: http://localhost:8084/swagger-ui.html

## 🐛 Troubleshooting

### Common Issues

#### Services Not Registering with Eureka
- Check if Discovery Server is running first
- Verify network connectivity between containers
- Check logs: `docker-compose logs discovery-server`

#### JWT Token Invalid
- Ensure auth-service is running and healthy
- Check token expiration (default: 24 hours)
- Verify JWT secret configuration in common module

#### Header Propagation Issues
- Verify API Gateway is extracting JWT claims correctly
- Check that downstream services are using common security filter
- Verify header names match between gateway and services

#### RabbitMQ Connection Issues
- Ensure RabbitMQ is running before starting services
- Check broker health: `docker-compose logs rabbitmq`
- Verify connection configuration and exchange/queue setup

#### Database Connection Issues
- Ensure H2/PostgreSQL is properly configured
- Check database URL in config files
- Verify database container is running

### Useful Commands
```bash
# View all running services
docker-compose ps

# Restart specific service
docker-compose restart [service-name]

# View service logs
docker-compose logs -f [service-name]

# Check RabbitMQ queues
curl -u guest:guest http://localhost:15672/api/queues

# Stop all services
docker-compose down

# Remove all containers and volumes
docker-compose down -v
```

## 📈 Performance Considerations

- **Service Discovery**: Services register every 30 seconds
- **JWT Expiration**: Default 24 hours (configurable)
- **Database**: H2 in-memory for development, PostgreSQL for production
- **Message Broker**: RabbitMQ for reliable message delivery
- **Common Module**: Shared components reduce code duplication and ensure consistency

## 🎬 Demo Script

### 1. System Overview (2 minutes)
- Show Eureka dashboard with all services
- Demonstrate service discovery
- Show RabbitMQ management interface

### 2. Authentication Flow (3 minutes)
- Register different user types
- Login and obtain JWT tokens
- Show token structure and claims
- Demonstrate header propagation

### 3. Business Logic Demo (5 minutes)
- Restaurant owner creates restaurant and menu
- Customer browses and places order
- Show RBAC in action with custom roles (no ROLE_ prefix)

### 4. Event-Driven Architecture (3 minutes)
- Place order and show RabbitMQ event flow
- Demonstrate notification service fetching restaurant details
- Show restaurant service processing order

### 5. Monitoring and Admin (2 minutes)
- Admin user management
- System health checks
- RabbitMQ queue monitoring

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review service logs
3. Verify service startup order
4. Check Eureka dashboard for service health
5. Monitor RabbitMQ queues and exchanges

---

**Built with ❤️ using Spring Boot Microservices Architecture with RabbitMQ**