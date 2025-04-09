# E-commerce Management System

A modern Android e-commerce application built with the latest technologies and best practices for 2024.

## Features

- **Authentication**: Complete user authentication system with email/password and OAuth providers
- **Product Management**: Browse, search, and filter products with pagination
- **Cart & Checkout**: Add products to cart and proceed to checkout
- **User Profiles**: User profile management with preferences
- **Order History**: Track order history and status
- **Real-time Updates**: Get real-time updates for product availability and prices

## Technical Specifications

### Architecture

This application follows Clean Architecture principles with MVVM pattern:

- **Presentation Layer**: Jetpack Compose UI with Material 3
- **Domain Layer**: Use cases and business logic
- **Data Layer**: Repositories, data sources, and models

### Tech Stack

- **Kotlin 1.9.25**: Modern Kotlin with coroutines and flow
- **Jetpack Compose 1.7.0**: Declarative UI toolkit
- **Material 3 1.3.0**: Latest Material Design components
- **Hilt 2.54**: Dependency injection
- **Supabase 2.4.0**: Backend-as-a-Service for authentication, database, and storage
- **Kotlinx Serialization 1.6.3**: JSON serialization/deserialization
- **Ktor 2.4.1**: HTTP client for API communication
- **Room 2.6.5**: Local database for offline capabilities
- **Coil 2.6.2**: Image loading library

### Supabase Integration

The application integrates with Supabase for:

- **Authentication**: User management with email/password and OAuth providers
- **PostgreSQL Database**: Product catalog, user profiles, orders
- **Storage**: Product images and user avatars
- **Real-time**: Live updates for product changes

## Getting Started

### Prerequisites

- Android Studio 2024 or newer
- JDK 17 or newer
- Supabase account

### Setup

1. Clone the repository
```
git clone "https://github.com/aditya13504/E-Commerce-Management-System.git"
```

2. Open in Android Studio

3. Create a `supabase.properties` file in the project root with your Supabase credentials:
```
SUPABASE_URL=https://your-supabase-url.supabase.co
SUPABASE_KEY=your-supabase-key
```

4. Build and run the application

## Architecture Components

### Core Components

- **SupabaseClientManager**: Manages the Supabase client and authentication state
- **ProductRepository**: Handles product data operations
- **AuthRepository**: Manages user authentication
- **ProductViewModel**: Business logic for product features
- **AuthViewModel**: Business logic for authentication

### Latest 2025 Features

- **Enhanced Error Handling**: Result type for comprehensive error handling
- **Optimistic Updates**: UI updates before server confirmation
- **Advanced Caching**: Smart caching policies for frequently accessed data
- **Full-text Search**: PostgreSQL-powered full-text search for products
- **Atomic Stock Updates**: Concurrent stock management using PostgreSQL functions
- **OAuth Integration**: Multiple authentication providers
- **User Profiles**: Extended user profile management
- **Real-time Monitoring**: Monitor changes to products and orders in real-time

## Security Features

- **Secure Token Storage**: Encrypted storage for authentication tokens
- **Token Refresh**: Automatic token refresh before expiration
- **Input Validation**: Client and server-side validation
- **API Security**: Best practices for API security

## Performance Optimization

- **Pagination**: Efficient loading of large datasets
- **Image Optimization**: Lazy loading and caching of images
- **Batch Processing**: Optimized database queries for batch operations
- **Reactive UI**: Responsive UI updates using Flow and StateFlow

## Contributing

Always welcome for contributions.

## License

MIT License
