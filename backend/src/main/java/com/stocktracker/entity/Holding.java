package com.stocktracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "holdings", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_symbol", columnNames = {"user_id", "symbol"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding extends BaseEntity {
    // Note: id (Long, auto-increment), createdAt, updatedAt inherited from BaseEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @NotBlank(message = "Symbol is required")
    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    @Column(nullable = false, length = 10)
    private String symbol;

    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @NotNull(message = "Shares is required")
    @Positive(message = "Shares must be positive")
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal shares;

    @NotNull(message = "Average cost is required")
    @Positive(message = "Average cost must be positive")
    @Column(name = "average_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal averageCost;
}
