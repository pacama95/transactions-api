package com.transaction.infrastructure.outgoing.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class KafkaProducerTest {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Inject
    KafkaProducer kafkaProducer;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void testProcessor() throws Exception {
        // Given
        UUID transactionId = UUID.randomUUID();
        String ticker = "AAPL";
        BigDecimal quantity = new BigDecimal("50");
        BigDecimal price = new BigDecimal("10.50");
        BigDecimal fees = new BigDecimal("1.55");
        LocalDate transactionDate = LocalDate.of(2024, 1, 15);
        String notes = "Test transaction";

        var transaction = new Transaction(
                transactionId,
                ticker,
                TransactionType.BUY,
                quantity,
                price,
                fees,
                Currency.EUR,
                transactionDate,
                notes,
                false, // isFractional is set to false to match the actual JSON
                false,
                new BigDecimal("1.0"),
                Currency.EUR,
                Collections.emptyList());

        var transactionCreatedEvent = new TransactionCreatedEvent(transaction);

        // When
        kafkaProducer.publish(transactionCreatedEvent).await().indefinitely();

        // Then
        ConsumerTask<String, String> transactions = companion.consume(String.class).fromTopics("transactions", 1);
        transactions.awaitCompletion();

        assertEquals(1, transactions.count());

        // Verify the message content
        ConsumerRecord<String, String> record = transactions.getRecords().get(0);
        String rawMessage = record.value();
        JsonNode messageJson = objectMapper.readTree(rawMessage);

        // Verify message structure
        assertNotNull(messageJson.get("eventId"), "eventId should not be null");
        assertNotNull(messageJson.get("occurredAt"), "occurredAt should not be null");
        assertNotNull(messageJson.get("messageCreatedAt"), "messageCreatedAt should not be null");
        assertNotNull(messageJson.get("payload"), "payload should not be null");

        // Verify payload content
        JsonNode payload = messageJson.get("payload");
        assertEquals(transactionId.toString(), payload.get("id").asText());
        assertEquals(ticker, payload.get("ticker").asText());
        assertEquals("BUY", payload.get("transactionType").asText());
        assertEquals(0, quantity.compareTo(new BigDecimal(payload.get("quantity").asText())));
        assertEquals(0, price.compareTo(new BigDecimal(payload.get("price").asText())));
        assertEquals(0, fees.compareTo(new BigDecimal(payload.get("fees").asText())));
        assertEquals("EUR", payload.get("currency").asText());
        assertEquals(transactionDate.toString(), payload.get("transactionDate").asText());
        assertEquals(notes, payload.get("notes").asText());
        assertEquals(false, payload.get("isFractional").asBoolean());
        assertEquals(0, new BigDecimal("1.0").compareTo(new BigDecimal(payload.get("fractionalMultiplier").asText())));
        assertEquals("EUR", payload.get("commissionCurrency").asText());
    }
}
