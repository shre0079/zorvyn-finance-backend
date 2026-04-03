package com.shreyash.zorvyn.finance_dashboard_backend.entities;



import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_tx_user_id",       columnList = "user_id"),
                @Index(name = "idx_tx_type",           columnList = "type"),
                @Index(name = "idx_tx_category",       columnList = "category"),
                @Index(name = "idx_tx_date",           columnList = "transaction_date"),
                @Index(name = "idx_tx_is_deleted",     columnList = "is_deleted"),
                @Index(name = "idx_tx_user_date_del",  columnList = "user_id, transaction_date, is_deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class Transaction {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    /**
     * The user who owns this transaction.
     * EAGER fetch is avoided.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transactions_user"))
    private User user;

    /**
     * Monetary amount. Must be > 0 (enforced by DB constraint and Bean Validation).
     * Using BigDecimal to avoid floating-point rounding errors in financial calculations.
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 100)
    private TransactionCategory category;

    /**
     * The date the transaction took place (not the record creation timestamp).
     * Cannot be in the future (validated in service layer).
     */
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** Optional free-text description; max 500 characters. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Soft-delete flag. Soft-deleted transactions are invisible to VIEWER/ANALYST
     * and excluded from all dashboard aggregations.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
