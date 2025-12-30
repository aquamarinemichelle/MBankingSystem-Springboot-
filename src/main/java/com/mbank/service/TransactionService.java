package com.mbank.service;

import com.mbank.model.BankAccount;
import com.mbank.model.Transaction;
import com.mbank.repository.BankAccountRepository;
import com.mbank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Generate unique transaction ID
    public String generateTransactionId(String prefix) {
        return prefix + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    // Record transaction
    @Transactional
    public Transaction recordTransaction(Integer accountNumber, String type, Double amount,
                                         String toAccount, String description) {

        String transactionId = generateTransactionId(
                type.equals("DEPOSIT") ? "DPT" :
                        type.equals("WITHDRAWAL") ? "WDR" :
                                type.equals("TRANSFER_DEBIT") || type.equals("TRANSFER_CREDIT") ? "TRF" : "TXN"
        );

        Transaction transaction = new Transaction(
                accountNumber, type, amount, toAccount, description, transactionId
        );

        return transactionRepository.save(transaction);
    }

    // Get transactions for account
    public List<Transaction> getAccountTransactions(Integer accountNumber) {
        return transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountNumber);
    }

    @Transactional
    public Transaction recordTransactionWithFee(Integer accountNumber, String type, Double amount,
                                                String toAccount, String description,
                                                Double fee, String transactionId) {

        Transaction transaction = new Transaction();
        transaction.setAccountNumber(accountNumber);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setToAccount(toAccount);
        transaction.setDescription(description != null ? description : "");
        transaction.setFee(fee != null ? fee : 0.0);
        transaction.setTransactionId(transactionId);
        transaction.setTransactionDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    // Get transactions with date range
    public List<Transaction> getTransactionsByDateRange(Integer accountNumber,
                                                        LocalDateTime startDate,
                                                        LocalDateTime endDate) {
        return transactionRepository.findByAccountNumberAndTransactionDateBetweenOrderByTransactionDateDesc(
                accountNumber, startDate, endDate
        );
    }

    // Get transactions with date range
    public List<Transaction> getTransactionsByDateRange(Integer accountNumber,
                                                        java.time.LocalDate startDate,
                                                        java.time.LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return getTransactionsByDateRange(accountNumber, startDateTime, endDateTime);
    }

    // Get transaction summary for account
    public TransactionSummary getTransactionSummary(Integer accountNumber,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate) {
        List<Transaction> transactions = getTransactionsByDateRange(accountNumber, startDate, endDate);

        TransactionSummary summary = new TransactionSummary();
        for (Transaction transaction : transactions) {
            String type = transaction.getTransactionType();
            double amount = transaction.getAmount();
            double fee = transaction.getFee();

            if (type.equals("DEPOSIT")) {
                summary.totalDeposits += amount;
            } else if (type.equals("WITHDRAWAL")) {
                summary.totalWithdrawals += Math.abs(amount);
            } else if (type.startsWith("TRANSFER_")) {
                if (amount < 0) {
                    summary.totalTransfers += Math.abs(amount);
                }
                summary.totalTransfersOut += (amount < 0) ? Math.abs(amount) : 0;
                summary.totalTransfersIn += (amount > 0) ? amount : 0;
            }
            summary.totalFees += fee;
            summary.transactionCount++;
        }

        return summary;
    }

    // Transaction summary
    public static class TransactionSummary {
        public double totalDeposits = 0;
        public double totalWithdrawals = 0;
        public double totalTransfers = 0;
        public double totalTransfersOut = 0;
        public double totalTransfersIn = 0;
        public double totalFees = 0;
        public int transactionCount = 0;
    }
}