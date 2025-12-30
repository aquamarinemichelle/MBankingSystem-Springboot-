package com.mbank.repository;

import com.mbank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountNumber(Integer accountNumber);
    List<Transaction> findByAccountNumberOrderByTransactionDateDesc(Integer accountNumber);

    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountNumberAndTransactionDateBetweenOrderByTransactionDateDesc(
            @Param("accountNumber") Integer accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    List<Transaction> findByAccountNumberAndTransactionTypeOrderByTransactionDateDesc(
            Integer accountNumber, String transactionType);

    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
            "AND t.transactionType = :transactionType " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountNumberAndTransactionTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
            @Param("accountNumber") Integer accountNumber,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByTransactionId(String transactionId);

    // Get latest transaction for account
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
            "ORDER BY t.transactionDate DESC LIMIT 1")
    Transaction findTopByAccountNumberOrderByTransactionDateDesc(@Param("accountNumber") Integer accountNumber);
}