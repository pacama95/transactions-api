package com.transaction.infrastructure.persistence.entity;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Default;
import com.transaction.domain.model.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_ticker", columnList = "ticker"),
                @Index(name = "idx_transactions_date", columnList = "transaction_date"),
                @Index(name = "idx_transactions_ticker_date", columnList = "ticker,transaction_date")
        }
)
@NoArgsConstructor(force = true)
@AllArgsConstructor(onConstructor = @__({@Default}))
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, columnDefinition = "transaction_type")
    private TransactionType transactionType;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "cost_per_share", nullable = false, precision = 18, scale = 4)
    private BigDecimal costPerShare;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, columnDefinition = "currency_type")
    private Currency currency;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "commission", precision = 18, scale = 4)
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_currency", columnDefinition = "currency_type")
    private Currency commissionCurrency;

    @Column(name = "exchange", length = 20)
    private String exchange;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "drip_confirmed")
    private Boolean dripConfirmed = false;

    @Column(name = "is_fractional")
    private Boolean isFractional = false;

    @Column(name = "fractional_multiplier", precision = 10, scale = 8)
    private BigDecimal fractionalMultiplier = BigDecimal.ONE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

}