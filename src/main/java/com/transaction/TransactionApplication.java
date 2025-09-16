package com.transaction;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Quarkus application entry point
 */
@QuarkusMain
@OpenAPIDefinition(
    info = @Info(
        title = "Transactions Management API",
        version = "1.0.0",
        description = "Reactive transaction management system with CRUD operations for transactions, positions, and market data",
        contact = @Contact(
            name = "Transaction Support",
            email = "support@transactions.com"
        ),
        license = @License(
            name = "MIT",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8081", description = "Development server")
    }
)
public class TransactionApplication implements QuarkusApplication {

    private static final Logger logger = LoggerFactory.getLogger(TransactionApplication.class);

    public static void main(String... args) {
        Quarkus.run(TransactionApplication.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        logger.info("Starting Transaction Management Application...");
        
        Quarkus.waitForExit();
        return 0;
    }
} 