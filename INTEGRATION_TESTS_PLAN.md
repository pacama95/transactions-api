# Integration Tests Plan for Transactions API

## Overview
This plan covers comprehensive integration testing for the Transactions API, including REST endpoints, MCP tools, and Redis Stream event publishing verification.

---

## 1. Testing Tools & Dependencies

### Required New Dependencies

```gradle
// Testcontainers for PostgreSQL and Redis
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testImplementation 'org.testcontainers:postgresql:1.19.3'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
testImplementation 'com.redis.testcontainers:testcontainers-redis:2.0.1'

// DbRider for database state management
testImplementation 'com.github.database-rider:rider-junit5:1.41.0'
testImplementation 'com.github.database-rider:rider-core:1.41.0'

// REST-Assured (already available)
// testImplementation 'io.rest-assured:rest-assured'

// JSON Path for assertions
testImplementation 'com.jayway.jsonpath:json-path:2.8.0'

// Awaitility for async verification
testImplementation 'org.awaitility:awaitility:4.2.0'

// Redis client for stream verification
// (already available via quarkus-redis-client)

// Quarkus test resources
testImplementation 'io.quarkus:quarkus-test-common'
```

### Tools Already Available
- ✅ REST-Assured - HTTP testing
- ✅ JUnit 5 - Test framework
- ✅ H2 Database - In-memory DB alternative
- ✅ Quarkus Test support

---

## 2. Test Infrastructure Setup

### 2.1 QuarkusTestResource Configuration

**File:** `src/test/java/com/transaction/infrastructure/testcontainers/PostgresTestResource.java`
- Start PostgreSQL testcontainer
- Configure connection properties for tests
- Lifecycle: start once per test suite

**File:** `src/test/java/com/transaction/infrastructure/testcontainers/RedisTestResource.java`
- Start Redis testcontainer
- Configure Redis connection for tests
- Lifecycle: start once per test suite

### 2.2 Test Configuration

**File:** `src/test/resources/application-test.properties`
```properties
# Database configuration (will be overridden by testcontainers)
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.database.generation=drop-and-create

# Redis configuration (will be overridden by testcontainers)
quarkus.redis.hosts=redis://localhost:6379

# Liquibase
quarkus.liquibase.migrate-at-start=true

# Logging
quarkus.log.level=INFO
quarkus.log.category."com.transaction".level=DEBUG

# Disable production features
quarkus.cache.enabled=false
```

### 2.3 Base Test Classes

**File:** `src/test/java/com/transaction/infrastructure/BaseIntegrationTest.java`
- Abstract base class with common setup
- Inject test resources (PostgreSQL, Redis)
- Helper methods for Redis stream verification
- Common test data builders
- Cleanup logic between tests

**File:** `src/test/java/com/transaction/infrastructure/RedisStreamVerifier.java`
- Utility class to read from Redis streams
- Methods to verify event publication
- Parse and assert on event payloads
- Wait for events with timeout (Awaitility)

---

## 3. REST Endpoint Integration Tests

### 3.1 Test Class Structure

**File:** `src/test/java/com/transaction/infrastructure/incoming/rest/TransactionRestControllerIT.java`

### 3.2 Test Scenarios

#### Create Transaction (POST /)
- ✅ **GIVEN** valid transaction data
  **WHEN** POST to `/api/transactions`
  **THEN**
  - Response status is 201 CREATED
  - Response body contains transaction with ID
  - Database contains the transaction
  - Redis stream `transaction:created` receives event with correct payload

- ✅ **GIVEN** invalid transaction data (missing required fields)
  **WHEN** POST to `/api/transactions`
  **THEN**
  - Response status is 400 BAD REQUEST
  - No database record created
  - No event published to Redis

- ✅ **GIVEN** invalid transaction data (negative quantity)
  **WHEN** POST to `/api/transactions`
  **THEN**
  - Response status is 400 BAD REQUEST
  - Error message indicates validation failure

#### Get Transaction by ID (GET /{id})
- ✅ **GIVEN** transaction exists in database
  **WHEN** GET to `/api/transactions/{id}`
  **THEN**
  - Response status is 200 OK
  - Response contains correct transaction data
  - All fields are correctly serialized (decimals, dates, enums)

- ✅ **GIVEN** transaction does NOT exist
  **WHEN** GET to `/api/transactions/{id}`
  **THEN**
  - Response status is 404 NOT FOUND
  - Response contains appropriate error message

#### Get All Transactions (GET /)
- ✅ **GIVEN** multiple transactions in database
  **WHEN** GET to `/api/transactions`
  **THEN**
  - Response status is 200 OK
  - Response contains all transactions as streaming array
  - Transactions are ordered correctly

- ✅ **GIVEN** empty database
  **WHEN** GET to `/api/transactions`
  **THEN**
  - Response status is 200 OK
  - Response is empty array

#### Get Transactions by Ticker (GET /ticker/{ticker})
- ✅ **GIVEN** transactions exist for ticker "AAPL"
  **WHEN** GET to `/api/transactions/ticker/AAPL`
  **THEN**
  - Response status is 200 OK
  - All returned transactions have ticker "AAPL"
  - Transactions from other tickers are not included

- ✅ **GIVEN** no transactions for ticker
  **WHEN** GET to `/api/transactions/ticker/UNKNOWN`
  **THEN**
  - Response status is 404 NOT FOUND

#### Search Transactions (GET /search)
- ✅ **GIVEN** transactions with various filters
  **WHEN** GET to `/api/transactions/search?ticker=AAPL&type=BUY&fromDate=2024-01-01&toDate=2024-12-31`
  **THEN**
  - Response status is 200 OK
  - All results match ALL filter criteria
  - Results are correctly filtered by date range

- ✅ **GIVEN** search with no matching results
  **WHEN** GET to `/api/transactions/search?ticker=UNKNOWN`
  **THEN**
  - Response status is 200 OK
  - Response is empty array

#### Update Transaction (PUT /{id})
- ✅ **GIVEN** existing transaction in database
  **WHEN** PUT to `/api/transactions/{id}` with updated data
  **THEN**
  - Response status is 200 OK
  - Response contains updated transaction
  - Database reflects the changes (verify implementation: delete + create)
  - Redis stream `transaction:updated` receives event
  - Event payload contains correct updated data

- ✅ **GIVEN** transaction does NOT exist
  **WHEN** PUT to `/api/transactions/{id}`
  **THEN**
  - Response status is 404 NOT FOUND

- ✅ **GIVEN** invalid update data
  **WHEN** PUT to `/api/transactions/{id}`
  **THEN**
  - Response status is 400 BAD REQUEST
  - Database remains unchanged
  - No event published

#### Delete Transaction (DELETE /{id})
- ✅ **GIVEN** existing transaction
  **WHEN** DELETE to `/api/transactions/{id}`
  **THEN**
  - Response status is 204 NO CONTENT
  - Transaction is removed from database
  - Redis stream `transaction:deleted` receives event
  - Event payload contains transaction ID and details

- ✅ **GIVEN** transaction does NOT exist
  **WHEN** DELETE to `/api/transactions/{id}`
  **THEN**
  - Response status is 404 NOT FOUND

#### Count Transactions (GET /count)
- ✅ **GIVEN** 10 transactions in database
  **WHEN** GET to `/api/transactions/count`
  **THEN**
  - Response status is 200 OK
  - Response body is 10

#### Count by Ticker (GET /count/{ticker})
- ✅ **GIVEN** 5 transactions for "AAPL"
  **WHEN** GET to `/api/transactions/count/AAPL`
  **THEN**
  - Response status is 200 OK
  - Response body is 5

### 3.3 DbRider Usage
```java
@DataSet(value = "datasets/transactions.yml", cleanBefore = true)
@Test
void shouldReturnTransactionsByTicker() {
    // Test implementation
}
```

---

## 4. MCP Endpoint Integration Tests

### 4.1 Test Class Structure

**File:** `src/test/java/com/transaction/infrastructure/incoming/mcp/PortfolioMcpServerIT.java`

### 4.2 MCP Testing Approach

**Challenge:** MCP tools are invoked via server-sent events (SSE) and JSON-RPC protocol

**Solution Options:**
1. **Direct Method Testing** (Recommended for integration tests)
   - Inject `PortfolioMcpServer` bean directly
   - Call tool methods programmatically
   - Verify database and Redis changes

2. **SSE Protocol Testing** (Optional - more E2E)
   - Use WebSocket/SSE client to call MCP endpoint
   - Send JSON-RPC formatted requests
   - Parse SSE responses

**Recommendation:** Use Option 1 (Direct Method Testing) for integration tests as it provides:
- Faster execution
- Easier debugging
- Clear test isolation
- Direct access to return values

### 4.3 Test Scenarios

#### createTransaction Tool
- ✅ **GIVEN** valid transaction parameters
  **WHEN** `createTransaction` tool is invoked
  **THEN**
  - Tool returns `CreateTransactionResponseDto.Success`
  - Database contains new transaction
  - Redis stream `transaction:created` receives event
  - Event payload matches input parameters

- ✅ **GIVEN** invalid parameters (missing required fields)
  **WHEN** `createTransaction` tool is invoked
  **THEN**
  - Tool throws `ToolCallException` with validation error
  - No database record created
  - No Redis event published

- ✅ **GIVEN** invalid data types (e.g., negative quantity)
  **WHEN** `createTransaction` tool is invoked
  **THEN**
  - Tool throws validation exception
  - Appropriate error response returned

#### getTransaction Tool
- ✅ **GIVEN** transaction exists with ID
  **WHEN** `getTransaction` tool is invoked with ID
  **THEN**
  - Tool returns `GetTransactionResponseDto.Success`
  - Response contains correct transaction data

- ✅ **GIVEN** transaction does NOT exist
  **WHEN** `getTransaction` tool is invoked
  **THEN**
  - Tool returns `GetTransactionResponseDto.NotFound`

#### updateTransaction Tool
- ✅ **GIVEN** existing transaction
  **WHEN** `updateTransaction` tool is invoked with changes
  **THEN**
  - Tool returns `UpdateTransactionResponseDto.Success`
  - Database reflects changes
  - Redis stream `transaction:updated` receives event
  - Event contains before/after state (if applicable)

- ✅ **GIVEN** transaction does NOT exist
  **WHEN** `updateTransaction` tool is invoked
  **THEN**
  - Tool returns `UpdateTransactionResponseDto.NotFound`

- ✅ **GIVEN** Redis publishing fails
  **WHEN** `updateTransaction` tool is invoked
  **THEN**
  - Tool returns `UpdateTransactionResponseDto.PublishError`
  - Database may or may not reflect changes (verify transaction behavior)

#### deleteTransaction Tool
- ✅ **GIVEN** existing transaction
  **WHEN** `deleteTransaction` tool is invoked
  **THEN**
  - Tool returns `DeleteTransactionResponseDto.Success`
  - Transaction removed from database
  - Redis stream `transaction:deleted` receives event

- ✅ **GIVEN** transaction does NOT exist
  **WHEN** `deleteTransaction` tool is invoked
  **THEN**
  - Tool returns `DeleteTransactionResponseDto.NotFound`

#### getTransactionsByTicker Tool
- ✅ **GIVEN** multiple transactions for ticker
  **WHEN** `getTransactionsByTicker` tool is invoked
  **THEN**
  - Tool returns `GetTransactionsByTickerResponseDto.Success`
  - All returned transactions match ticker
  - Results are complete and correctly formatted

#### searchTransactions Tool
- ✅ **GIVEN** various search criteria
  **WHEN** `searchTransactions` tool is invoked with filters
  **THEN**
  - Tool returns `SearchTransactionsResponseDto.Success`
  - Results match all filter criteria
  - Date range filtering works correctly

### 4.4 DbRider Usage
```java
@DataSet(value = "datasets/transactions.yml", cleanBefore = true)
@Test
void mcpCreateTransaction_ShouldPublishToRedis() {
    // Test implementation
}
```

---

## 5. Redis Stream Event Verification

### 5.1 Redis Stream Verification Utility

**File:** `src/test/java/com/transaction/infrastructure/testutil/RedisStreamVerifier.java`

**Key Methods:**
```java
public class RedisStreamVerifier {

    // Wait for and retrieve event from stream
    public Optional<Message<TransactionCreatedData>> waitForCreatedEvent(
        String eventId,
        Duration timeout
    );

    // Verify event exists in stream
    public void assertEventPublished(
        String streamKey,
        String eventId,
        Consumer<String> payloadAssertion
    );

    // Get all events from stream since timestamp
    public List<StreamMessage> getEventsSince(
        String streamKey,
        Instant since
    );

    // Clear stream for cleanup
    public void clearStream(String streamKey);
}
```

### 5.2 Event Verification Test Scenarios

#### Created Event Verification
- ✅ Verify `transaction:created` stream receives event
- ✅ Verify event structure: eventId, occurredAt, publishedAt, eventType, payload
- ✅ Verify payload contains: id, ticker, transactionType, quantity, price, fees, currency, transactionDate, notes, isFractional, fractionalMultiplier, commissionCurrency, exchange, country
- ✅ Verify decimal precision matches domain model
- ✅ Verify timestamps are in ISO-8601 format
- ✅ Verify eventId matches UUID format

#### Updated Event Verification
- ✅ Verify `transaction:updated` stream receives event
- ✅ Verify event payload contains updated transaction data
- ✅ Verify all fields are present (considering delete+create implementation)

#### Deleted Event Verification
- ✅ Verify `transaction:deleted` stream receives event
- ✅ Verify payload contains transaction ID and details
- ✅ Verify event is published even when transaction is soft-deleted

#### Event Ordering & Timing
- ✅ Verify events are published in correct order for multiple operations
- ✅ Verify `publishedAt` timestamp is set correctly
- ✅ Verify events are available immediately after operation completes

#### Error Scenarios
- ✅ When Redis is unavailable, verify `ServiceException` with `PUBLISH_ERROR`
- ✅ Verify database rollback when event publishing fails (if transactional)
- ✅ Verify error handling for serialization failures

---

## 6. Test Data Management

### 6.1 DbRider Dataset Files

**File:** `src/test/resources/datasets/empty-db.yml`
```yaml
# Empty dataset to ensure clean database state
```

**File:** `src/test/resources/datasets/transactions.yml`
```yaml
transactions:
  - id: "123e4567-e89b-12d3-a456-426614174000"
    ticker: "AAPL"
    transaction_type: "BUY"
    quantity: 10.000000
    cost_per_share: 150.2500
    currency: "USD"
    transaction_date: "2024-01-15"
    commission: 5.0000
    commission_currency: "USD"
    is_fractional: false
    notes: "Test transaction"
    exchange: "NASDAQ"
    country: "US"
    created_at: "2024-01-15 10:00:00"
    updated_at: "2024-01-15 10:00:00"

  - id: "223e4567-e89b-12d3-a456-426614174001"
    ticker: "GOOGL"
    transaction_type: "BUY"
    quantity: 5.000000
    cost_per_share: 2800.7500
    currency: "USD"
    transaction_date: "2024-02-20"
    commission: 7.5000
    commission_currency: "USD"
    is_fractional: false
    notes: "Another test"
    exchange: "NASDAQ"
    country: "US"
    created_at: "2024-02-20 14:30:00"
    updated_at: "2024-02-20 14:30:00"
```

**File:** `src/test/resources/datasets/transactions-by-ticker.yml`
```yaml
# Multiple AAPL transactions for ticker-based queries
transactions:
  - id: "323e4567-e89b-12d3-a456-426614174002"
    ticker: "AAPL"
    # ... more AAPL transactions
  - id: "423e4567-e89b-12d3-a456-426614174003"
    ticker: "AAPL"
    # ... more AAPL transactions
```

### 6.2 Test Data Builders

**File:** `src/test/java/com/transaction/infrastructure/testutil/TransactionTestDataBuilder.java`
```java
public class TransactionTestDataBuilder {
    public static CreateTransactionCommand.Builder defaultCreateCommand();
    public static TransactionEntity.Builder defaultEntity();
    public static CreateTransactionRequestDto.Builder defaultCreateRequest();
    // Additional builders for various scenarios
}
```

---

## 7. Test Execution & Coverage

### 7.1 Test Execution Commands

```bash
# Run all tests
./gradlew test

# Run only integration tests
./gradlew test --tests "*IT"

# Run specific test class
./gradlew test --tests "TransactionRestControllerIT"

# Run with coverage
./gradlew test jacocoTestReport
```

### 7.2 Coverage Goals

- **REST Controller:** 100% endpoint coverage
- **MCP Server:** 100% tool coverage
- **Redis Publishing:** 100% event stream verification
- **Error Scenarios:** All error paths tested
- **Database Operations:** All CRUD operations verified

---

## 8. Test Organization

### 8.1 Directory Structure

```
src/test/java/com/transaction/
├── infrastructure/
│   ├── incoming/
│   │   ├── rest/
│   │   │   └── TransactionRestControllerIT.java
│   │   └── mcp/
│   │       └── PortfolioMcpServerIT.java
│   ├── outgoing/
│   │   └── messaging/
│   │       └── RedisPublisherIT.java
│   ├── testcontainers/
│   │   ├── PostgresTestResource.java
│   │   └── RedisTestResource.java
│   ├── testutil/
│   │   ├── RedisStreamVerifier.java
│   │   ├── TransactionTestDataBuilder.java
│   │   └── BaseIntegrationTest.java
│   └── persistence/
│       └── TransactionRepositoryAdapterIT.java
│
src/test/resources/
├── application-test.properties
├── datasets/
│   ├── empty-db.yml
│   ├── transactions.yml
│   └── transactions-by-ticker.yml
└── dbunit/
    └── dbunit.yml (DbRider configuration)
```

### 8.2 Naming Conventions

- Integration test classes: `*IT.java` (e.g., `TransactionRestControllerIT`)
- Unit test classes: `*Test.java` (e.g., `TransactionMapperTest`)
- Test methods: `should[ExpectedBehavior]_When[Condition]` (e.g., `shouldReturnCreated_WhenValidTransactionPosted`)

---

## 9. CI/CD Considerations

### 9.1 CI Pipeline Integration

```yaml
# Example GitHub Actions / GitLab CI
test:
  stage: test
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: build/test-results/test/**/TEST-*.xml
    paths:
      - build/reports/tests/test/
      - build/reports/jacoco/
```

### 9.2 Test Containers in CI

- Testcontainers automatically handles Docker container lifecycle
- Ensure CI environment has Docker available
- Configure testcontainers reuse for faster execution (optional)

---

## 10. Additional Considerations

### 10.1 Performance Testing
- Consider adding performance benchmarks for high-volume scenarios
- Test Redis stream performance under load
- Measure response times for REST endpoints

### 10.2 Security Testing
- Validate authentication/authorization (if applicable)
- Test CORS configuration
- Verify input sanitization

### 10.3 Contract Testing
- Consider adding contract tests for REST API (e.g., Spring Cloud Contract)
- Document API contracts using OpenAPI spec

### 10.4 Observability Testing
- Verify metrics are published correctly
- Test health check endpoints
- Validate logging output

---

## 11. Implementation Timeline

### Phase 1: Infrastructure Setup (Week 1)
- [ ] Add dependencies to build.gradle
- [ ] Create test resource classes (PostgreSQL, Redis)
- [ ] Set up base test classes
- [ ] Create RedisStreamVerifier utility
- [ ] Configure application-test.properties

### Phase 2: REST Integration Tests (Week 2)
- [ ] Implement TransactionRestControllerIT
- [ ] Create DbRider datasets
- [ ] Test all CRUD operations
- [ ] Verify Redis event publishing for each operation
- [ ] Test error scenarios

### Phase 3: MCP Integration Tests (Week 3)
- [ ] Implement PortfolioMcpServerIT
- [ ] Test all MCP tools
- [ ] Verify database changes
- [ ] Verify Redis event publishing
- [ ] Test error scenarios

### Phase 4: Event Verification & Edge Cases (Week 4)
- [ ] Comprehensive Redis stream verification tests
- [ ] Edge case testing (concurrent operations, race conditions)
- [ ] Performance baseline tests
- [ ] Documentation and cleanup

---

## 12. Success Criteria

✅ All REST endpoints have integration test coverage
✅ All MCP tools have integration test coverage
✅ Redis event publishing is verified for all operations
✅ Database state changes are validated
✅ Error scenarios are tested
✅ Test execution time is reasonable (< 5 minutes for full suite)
✅ Tests are deterministic and repeatable
✅ CI/CD pipeline includes integration tests
✅ Code coverage meets or exceeds 80% for integration paths

---

## Questions for Approval

1. **DbRider vs Testcontainers only:** Do you prefer DbRider for dataset management, or would you like to use pure SQL scripts with Testcontainers?

2. **MCP Testing Approach:** Should we test MCP tools via direct injection (faster) or through SSE protocol (more realistic but slower)?

3. **Redis Verification:** Should we verify exact event payload structure or just presence of events?

4. **Test Data:** Should we use DbRider YAML datasets or programmatic test data builders?

5. **Additional Tools:** Are there any other specific tools or frameworks you'd like included?

6. **Coverage Goals:** Is 80% coverage target acceptable, or do you want higher/lower?

---

**Next Steps:**
Please review this plan and provide approval or feedback. Once approved, I'll begin implementation starting with Phase 1.
