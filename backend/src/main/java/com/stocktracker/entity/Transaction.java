package com.stocktracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_user_symbol", columnList = "user_id, symbol"),
    @Index(name = "idx_user_date", columnList = "user_id, transaction_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TransactionType type;

    @NotBlank(message = "Symbol is required")
    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    @Column(nullable = false, length = 10)
    private String symbol;

    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Shares is required")
    @Positive(message = "Shares must be positive")
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal shares;

    @NotNull(message = "Price per share is required")
    @Positive(message = "Price must be positive")
    @Column(name = "price_per_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerShare;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @PositiveOrZero(message = "Fees cannot be negative")
    @Column(name = "broker_fee", precision = 10, scale = 2)
    private BigDecimal brokerFee;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @PrePersist
    @PreUpdate
    public void calculateTotalAmount() {
        if (shares != null && pricePerShare != null) {
            this.totalAmount = shares.multiply(pricePerShare);
            if (brokerFee != null) {
                this.totalAmount = this.totalAmount.add(brokerFee);
            }
        }
    }
}
