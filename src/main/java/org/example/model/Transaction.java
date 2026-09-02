package org.example.model;

import org.example.exceptions.TransactionParamsException;
import org.example.exceptions.TransactionTypeException;
import org.example.util.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Transaction {
    private final UUID transactionId;
    private final UUID accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime timeStamp;
    private final UUID relatedAccountId;

    public Transaction(UUID accountId, TransactionType type, BigDecimal amount) {
        if (TransactionType.TRANSFER_IN.equals(type) || TransactionType.TRANSFER_OUT.equals(type)) {
            throw new TransactionTypeException("Для данного типа транзакции требуется обязательное указание второго счета");
        }
        this.transactionId = UUID.randomUUID();
        this.accountId = Optional.ofNullable(accountId).orElseThrow(() -> new TransactionParamsException("Не передано значения accountId"));
        this.type = Optional.ofNullable(type).orElseThrow(() -> new TransactionParamsException("Не передано значение type"));
        this.amount = Optional.ofNullable(amount).orElseThrow(() -> new TransactionParamsException("Не передано значение amount"));
        this.timeStamp = LocalDateTime.now();
        this.relatedAccountId = null;
    }

    public Transaction(UUID accountId, TransactionType type, BigDecimal amount, UUID relatedAccountId) {
        if (TransactionType.DEPOSIT.equals(type) || TransactionType.WITHDRAW.equals(type)) {
            throw new TransactionTypeException("Для данного типа транзакции не требуется дополнительны счет");
        }

        this.transactionId = UUID.randomUUID();
        this.accountId = Optional.ofNullable(accountId).orElseThrow(() -> new TransactionParamsException("Не передано значения accountId"));
        this.type = Optional.ofNullable(type).orElseThrow(() -> new TransactionParamsException("Не передано значение type"));
        this.amount = Optional.ofNullable(amount).orElseThrow(() -> new TransactionParamsException("Не передано значение amount"));
        this.timeStamp = LocalDateTime.now();
        this.relatedAccountId = Optional.ofNullable(relatedAccountId).orElseThrow(() -> new TransactionParamsException("Не передано значение relatedAccountId"));
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public UUID getRelatedAccountId() {
        return relatedAccountId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId) && Objects.equals(accountId, that.accountId) && type == that.type && Objects.equals(amount, that.amount) && Objects.equals(timeStamp, that.timeStamp) && Objects.equals(relatedAccountId, that.relatedAccountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, accountId, type, amount, timeStamp, relatedAccountId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", accountId=" + accountId +
                ", type=" + type +
                ", amount=" + amount +
                ", timeStamp=" + timeStamp +
                ", relatedAccountId=" + relatedAccountId +
                '}';
    }
}
