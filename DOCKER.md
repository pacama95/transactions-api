# Docker Build and Run Instructions

This project provides two Docker configurations for different deployment scenarios:

1. **Native Image** (`Dockerfile`) - GraalVM native compilation for smaller size and faster startup
2. **JVM Image** (`Dockerfile.jvm`) - Standard JVM runtime for broader compatibility

## Prerequisites

- Docker installed and running
- PostgreSQL database accessible (either local or via Docker)

## Native Image Build

### Build:
```bash
docker build -f Dockerfile -t transactions-api:native .
```

### Run:
```bash
docker run --rm -p 8081:8080 \
  -e PGHOST=host.docker.internal \
  -e PGPORT=5432 \
  -e PGDATABASE=portfolio_db \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=portfolio_pass \
  transactions-api:native
```

## JVM Image Build

### Build:
```bash
docker build -f Dockerfile.jvm -t transactions-api:jvm .
```

### Run:
```bash
docker run --rm -p 8081:8080 \
  -e PGHOST=host.docker.internal \
  -e PGPORT=5432 \
  -e PGDATABASE=portfolio_db \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=portfolio_pass \
  transactions-api:jvm
```

## Environment Variables

The following environment variables are required for database connectivity:

| Variable | Description | Example Value |
|----------|-------------|---------------|
| `PGHOST` | PostgreSQL host | `host.docker.internal` (for local DB) |
| `PGPORT` | PostgreSQL port | `5432` |
| `PGDATABASE` | Database name | `portfolio_db` |
| `DATABASE_USERNAME` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | `portfolio_pass` |

## Additional Environment Variables (Optional)

You can also set these optional environment variables:

```bash
# For TwelveData API integration
-e TWELVE_DATA_API_KEY=your_api_key_here \

# For custom logging levels
-e QUARKUS_LOG_LEVEL=INFO \

# For custom application port (if needed)
-e PORT=8080 \
```

## Docker Compose Alternative

For easier local development, you can also use docker-compose:

```bash
docker-compose up --build
```

This will start both the application and PostgreSQL database together.

## Accessing the Application

Once running, the application will be available at:
- **API**: http://localhost:8081
- **Health Check**: http://localhost:8081/q/health
- **OpenAPI/Swagger**: http://localhost:8081/q/swagger-ui

## Build Performance Notes

- **Native Build**: Takes 5-10 minutes but results in faster startup (~0.1s) and lower memory usage
- **JVM Build**: Takes 1-2 minutes with standard startup time (~2-3s) but uses more memory

Choose the native build for production deployments and the JVM build for development/testing when build time matters more than runtime performance.

## Troubleshooting

### Database Connection Issues
If you encounter database connection issues:

1. Ensure PostgreSQL is running locally
2. Use `host.docker.internal` instead of `localhost` for the database host when running from Docker
3. Verify the database credentials and database name exist

### Port Conflicts
If port 8081 is already in use:
```bash
# Use a different external port
docker run --rm -p 8082:8080 ... transactions-api:native
```

### Memory Issues (Native Build)
If the native build fails due to memory constraints:
```bash
# Increase Docker memory limit to at least 8GB
# Or build without Docker using local GraalVM installation
```
