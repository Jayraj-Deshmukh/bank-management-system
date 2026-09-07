package com.bank.entity;

import com.bank.entity.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_reference", columnNames = "transaction_reference")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transaction reference is required")
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 60)
    private String transactionReference;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Transaction amount must be positive")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Balance after transaction is required")
    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_account"))
    private Account account;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(String transactionReference, TransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter, String description, Account account) {
        this.transactionReference = transactionReference;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.account = account;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) || Objects.equals(transactionReference, that.transactionReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionReference);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionReference='" + transactionReference + '\'' +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
