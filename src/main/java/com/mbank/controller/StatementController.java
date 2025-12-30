package com.mbank.controller;

import com.mbank.model.BankAccount;
import com.mbank.model.Transaction;
import com.mbank.repository.TransactionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class StatementController {

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/statement")
    public String viewStatement(
            HttpSession session,
            Model model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateParam,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateParam,
            @RequestParam(required = false, defaultValue = "ALL") String typeFilter) {

        System.out.println("=== StatementController called ===");

        // Check if user is logged in
        BankAccount account = (BankAccount) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        int accountNumber = account.getAccountNumber();
        System.out.println("Processing statement for account #" + accountNumber);

        // Set account info
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("accountHolder", account.getAccountHolder());
        model.addAttribute("currentBalance", account.getBalance());

        // Set default dates
        LocalDate defaultEndDate = LocalDate.now();
        LocalDate defaultStartDate = defaultEndDate.minusDays(30);

        LocalDate startDate = (startDateParam != null) ? startDateParam : defaultStartDate;
        LocalDate endDate = (endDateParam != null) ? endDateParam : defaultEndDate;

        // Ensure start date is not after end date
        if (startDate.isAfter(endDate)) {
            startDate = endDate.minusDays(30);
        }


        model.addAttribute("startDate", startDate.toString());
        model.addAttribute("endDate", endDate.toString());
        model.addAttribute("typeFilter", typeFilter);

        try {
            System.out.println("Fetching transactions from database...");
            System.out.println("Date range: " + startDate + " to " + endDate);
            System.out.println("Filter type: " + typeFilter);

            List<Transaction> dbTransactions = new ArrayList<>();
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            if ("ALL".equals(typeFilter)) {
                // Get all transactions for account within date range
                dbTransactions = transactionRepository.findByAccountNumberAndTransactionDateBetweenOrderByTransactionDateDesc(
                        accountNumber, startDateTime, endDateTime);
                System.out.println("Found " + dbTransactions.size() + " ALL transactions");
            } else if ("TRANSFER".equals(typeFilter)) {
                // Get transfer transactions
                List<Transaction> debitTransfers = transactionRepository
                        .findByAccountNumberAndTransactionTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                                accountNumber, "TRANSFER_DEBIT", startDateTime, endDateTime);
                List<Transaction> creditTransfers = transactionRepository
                        .findByAccountNumberAndTransactionTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                                accountNumber, "TRANSFER_CREDIT", startDateTime, endDateTime);

                System.out.println("Found " + debitTransfers.size() + " debit transfers");
                System.out.println("Found " + creditTransfers.size() + " credit transfers");

                dbTransactions.addAll(debitTransfers);
                dbTransactions.addAll(creditTransfers);

                // Sort by date descending
                dbTransactions.sort((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()));
            } else {

                dbTransactions = transactionRepository
                        .findByAccountNumberAndTransactionTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                                accountNumber, typeFilter, startDateTime, endDateTime);
                System.out.println("Found " + dbTransactions.size() + " " + typeFilter + " transactions");
            }


            if (dbTransactions.size() > 100) {
                dbTransactions = dbTransactions.subList(0, 100);
            }

            System.out.println("Total transactions to display: " + dbTransactions.size());


            List<Map<String, Object>> transactions = new ArrayList<>();
            double totalDeposits = 0;
            double totalWithdrawals = 0;
            double totalTransfers = 0;
            double totalFees = 0;

            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

            for (Transaction tx : dbTransactions) {
                Map<String, Object> transaction = new HashMap<>();

                String dbType = tx.getTransactionType();
                String displayType;

                if (dbType.startsWith("TRANSFER_")) {
                    displayType = "TRANSFER";
                } else {
                    displayType = dbType;
                }

                transaction.put("type", displayType);
                transaction.put("amount", tx.getAmount());
                transaction.put("toAccount", tx.getToAccount());
                transaction.put("description", tx.getDescription());
                transaction.put("fee", tx.getFee() != null ? tx.getFee() : 0.0);
                transaction.put("date", tx.getTransactionDate());
                transaction.put("transactionId", tx.getTransactionId());

                // Format date
                if (tx.getTransactionDate() != null) {
                    transaction.put("formattedDate", tx.getTransactionDate().format(displayFormatter));
                } else {
                    transaction.put("formattedDate", "N/A");
                }

                transactions.add(transaction);

                // Update summary
                switch (displayType) {
                    case "DEPOSIT":
                        totalDeposits += tx.getAmount();
                        break;
                    case "WITHDRAWAL":
                        totalWithdrawals += Math.abs(tx.getAmount());
                        break;
                    case "TRANSFER":
                        if (tx.getAmount() < 0) {
                            totalTransfers += Math.abs(tx.getAmount());
                        }
                        break;
                }
                totalFees += (tx.getFee() != null ? tx.getFee() : 0.0);
            }

            model.addAttribute("transactions", transactions);

            // Create summary
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDeposits", totalDeposits);
            summary.put("totalWithdrawals", totalWithdrawals);
            summary.put("totalTransfers", totalTransfers);
            summary.put("totalFees", totalFees);
            summary.put("transactionCount", transactions.size());

            model.addAttribute("summary", summary);
            System.out.println("Summary created: Deposits=" + totalDeposits +
                    ", Withdrawals=" + totalWithdrawals + ", Transfers=" + totalTransfers);

        } catch (Exception e) {
            System.err.println("Database error in StatementController: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Database error: " + e.getMessage());


            model.addAttribute("transactions", new ArrayList<>());

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDeposits", 0.0);
            summary.put("totalWithdrawals", 0.0);
            summary.put("totalTransfers", 0.0);
            summary.put("totalFees", 0.0);
            summary.put("transactionCount", 0);
            model.addAttribute("summary", summary);
        }

        return "statement";
    }
}