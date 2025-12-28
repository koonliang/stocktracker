# Stock Tracker

A full-stack web application for tracking stock portfolios and watchlists with real-time pricing data from Yahoo Finance.

## Features

- User authentication with JWT tokens
- Real-time stock portfolio tracking
- Live price updates from Yahoo Finance API
- Automatic calculation of returns and performance metrics
- Dashboard with portfolio overview
- Watchlist management
- Responsive design with TailwindCSS

## Tech Stack

### Frontend
- **Framework**: React 19.2.0
- **Build Tool**: Vite 7.2.4
- **Language**: TypeScript 5.9.3
- **Routing**: React Router DOM 7.11.0
- **HTTP Client**: Axios 1.13.2
- **Styling**: TailwindCSS 3.4.19
- **Code Quality**: ESLint, Prettier

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (development), MySQL (production)
- **Security**: Spring Security with JWT
- **ORM**: Spring Data JPA
- **Caching**: Caffeine
- **API Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven

### External APIs
- **Yahoo Finance** - Real-time stock quotes and pricing data

## Project Structure

```
stocktracker/
├── backend/                 # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stocktracker/
│   │   │   │   ├── client/          # Yahoo Finance API client
│   │   │   │   ├── config/          # Configuration (Security, Cache)
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── repository/      # Database repositories
│   │   │   │   └── service/         # Business logic
│   │   │   └── resources/
│   │   │       ├── application.yml  # Application configuration
│   │   │       └── data.sql         # Seed data
│   │   └── test/
│   └── pom.xml
├── frontend/                # React frontend
│   ├── src/
│   │   ├── components/      # Reusable UI components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── pages/           # Page components
│   │   ├── services/        # API services
│   │   ├── types/           # TypeScript types
│   │   └── utils/           # Utility functions
│   └── package.json
└── tasks/                   # Project task documentation

```

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Maven 3.6+
- npm or yarn

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```

2. Install dependencies and build:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The backend server will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

The frontend application will start on `http://localhost:5173`

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Portfolio
- `GET /api/portfolio` - Get user's portfolio with live prices
- `GET /api/portfolio/refresh` - Force refresh prices (bypasses cache)

### Holdings
- `GET /api/holdings` - Get all user holdings
- `POST /api/holdings` - Add a new holding
- `PUT /api/holdings/{id}` - Update a holding
- `DELETE /api/holdings/{id}` - Delete a holding

## Key Features

### Portfolio Dashboard
- View all holdings with real-time prices
- Calculate total returns (dollar and percentage)
- Display current value vs. cost basis
- Portfolio summary with total value, cost, and returns
- Automatic price caching (2-minute TTL)
- Manual refresh option

### Stock Data Integration
- Integration with Yahoo Finance API for real-time quotes
- Batch price fetching for multiple symbols
- Automatic retry and error handling
- Cached responses to reduce API calls

### Security
- JWT-based authentication
- BCrypt password hashing
- Secured API endpoints
- CORS configuration

## Development Scripts

### Frontend
```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # Run ESLint
npm run lint:fix     # Fix ESLint issues
npm run format       # Format code with Prettier
```

### Backend
```bash
mvn spring-boot:run  # Run the application
mvn test             # Run tests
mvn clean install    # Clean and build
```

## Database Schema

### Users Table
- `id` (Primary Key)
- `email` (Unique)
- `password` (Hashed)
- `created_at`
- `updated_at`

### Holdings Table
- `id` (Primary Key)
- `user_id` (Foreign Key)
- `symbol` (Stock ticker)
- `company_name`
- `shares` (Supports fractional shares)
- `average_cost` (Cost per share)
- `created_at`
- `updated_at`

## Environment Configuration

### Backend
Configure in `backend/src/main/resources/application.yml`:
- Database connection
- JWT secret key
- Yahoo Finance API URL
- Cache settings

### Frontend
Configure in `frontend/vite.config.ts`:
- API base URL
- Proxy settings

## Caching Strategy

The application uses Caffeine cache to optimize Yahoo Finance API calls:
- Portfolio data cached for 2 minutes
- Automatic expiration and refresh
- Manual refresh endpoint available
- Maximum cache size: 1000 entries

## Design System

The UI follows a "Corporate Trust" design system with:
- Professional color palette (Slate, Indigo, Emerald)
- Consistent spacing and typography
- Accessible color contrast
- Responsive breakpoints
- TailwindCSS utility classes

## Future Enhancements

- Sortable table columns
- Search and filter holdings
- Watchlist functionality expansion
- Real-time updates via WebSocket
- Price charts and historical data
- Export to CSV
- Multiple portfolio support
- Market hours indicator
- Performance analytics