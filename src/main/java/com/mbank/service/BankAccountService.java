package com.mbank.service;

import com.mbank.model.BankAccount;
import com.mbank.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Service
public class BankAccountService {

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    // Generate unique account number
    public Integer generateAccountNumber() {
        Integer accountNumber = 100000 + (int)(Math.random() * 900000);

        // Ensure uniqueness
        while (accountRepository.existsById(accountNumber)) {
            accountNumber = 100000 + (int)(Math.random() * 900000);
        }

        return accountNumber;
    }

    // Register new account
    @Transactional
    public BankAccount registerAccount(String accountHolder, String email, String password) {
        if (accountRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        Integer accountNumber = generateAccountNumber();
        BankAccount account = new BankAccount();
        account.setAccountNumber(accountNumber);
        account.setAccountHolder(accountHolder);
        account.setBalance(0.0);
        account.setEmail(email);
        account.setPassword(password);

        BankAccount savedAccount = accountRepository.save(account);

        // Record initial deposit transaction
        transactionService.recordTransaction(
                accountNumber,
                "INITIAL_DEPOSIT",
                0.0,
                null,
                "Account opening"
        );

        return savedAccount;
    }

    // Login
    public Optional<BankAccount> login(String email, String password) {
        return accountRepository.findByEmailAndPassword(email, password);
    }

    // Get account by number
    public Optional<BankAccount> getAccount(Integer accountNumber) {
        return accountRepository.findById(accountNumber);
    }

    // Get account by email
    public Optional<BankAccount> getAccountByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    // Enhanced Deposit with transaction recording
    @Transactional
    public BankAccount deposit(Integer accountNumber, Double amount) {
        BankAccount account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Validate
        if (amount <= 0) {
            throw new RuntimeException("Amount must be greater than zero!");
        }

        if (amount > 100000) {
            throw new RuntimeException("Maximum deposit is R100,000 per transaction!");
        }

        // Perform deposit
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        BankAccount updatedAccount = accountRepository.save(account);

        // Record transaction
        transactionService.recordTransaction(
                accountNumber,
                "DEPOSIT",
                amount,
                null,
                "Deposit to account"
        );

        return updatedAccount;
    }

    // Enhanced Withdraw with transaction recording
    @Transactional
    public BankAccount withdraw(Integer accountNumber, Double amount) {
        BankAccount account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Validate
        if (amount <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than zero!");
        }

        if (amount > 50000) {
            throw new RuntimeException("Maximum withdrawal is R50,000 per transaction!");
        }

        if (amount > account.getBalance()) {
            throw new RuntimeException("Insufficient funds! Available: R" + account.getBalance());
        }

        // Perform withdrawal
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance - amount);
        BankAccount updatedAccount = accountRepository.save(account);

        // Record transaction (negative amount for withdrawal)
        transactionService.recordTransaction(
                accountNumber,
                "WITHDRAWAL",
                -amount,
                null,
                "Withdrawal from account"
        );

        return updatedAccount;
    }

    // ============ UPDATED TRANSFER METHOD ============
    // Transfer (with transaction management and fee calculation)
    @Transactional
    public TransferResult transfer(Integer fromAccountNumber, Integer toAccountNumber, Double amount, String description) {
        BankAccount fromAccount = accountRepository.findById(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("From account not found"));

        BankAccount toAccount = accountRepository.findById(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("To account not found"));

        // Validate
        if (amount <= 0) {
            throw new RuntimeException("Transfer amount must be positive");
        }

        if (amount > 100000) {
            throw new RuntimeException("Maximum transfer amount is R100,000");
        }

        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new RuntimeException("Cannot transfer to your own account");
        }

        // Calculate fee (R10 for transfers over R1000)
        double fee = amount > 1000 ? 10 : 0;
        double totalDebit = amount + fee;

        if (fromAccount.getBalance() < totalDebit) {
            throw new RuntimeException("Insufficient funds. Available: R" + fromAccount.getBalance() +
                    ", Required: R" + totalDebit + " (including R" + fee + " fee)");
        }

        // Perform transfer
        fromAccount.setBalance(fromAccount.getBalance() - totalDebit);
        toAccount.setBalance(toAccount.getBalance() + amount);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Generate transaction ID
        String transactionId = "TRF" + System.currentTimeMillis() + (int)(Math.random() * 1000);

        // Record transactions for both accounts (will be saved by controller)
        TransferResult result = new TransferResult();
        result.fromAccount = fromAccount;
        result.toAccount = toAccount;
        result.fee = fee;
        result.totalDebit = totalDebit;
        result.transactionId = transactionId;
        result.description = description;

        return result;
    }

    // Transfer result DTO
    public static class TransferResult {
        public BankAccount fromAccount;
        public BankAccount toAccount;
        public double fee;
        public double totalDebit;
        public String transactionId;
        public String description;
    }

    // Alternative transfer method without description (for backward compatibility)
    @Transactional
    public void transfer(Integer fromAccountNumber, Integer toAccountNumber, Double amount) {
        transfer(fromAccountNumber, toAccountNumber, amount, null);
    }

    // Get all accounts
    public List<BankAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    // Check if email exists
    public boolean emailExists(String email) {
        return accountRepository.existsByEmail(email);
    }

    // Update account
    @Transactional
    public BankAccount updateAccount(BankAccount account) {
        return accountRepository.save(account);
    }

    // Delete account
    @Transactional
    public void deleteAccount(Integer accountNumber) {
        accountRepository.deleteById(accountNumber);
    }

    // ============ NEW METHODS ADDED ============

    // Check if account exists
    public boolean accountExists(Integer accountNumber) {
        return accountRepository.existsById(accountNumber);
    }

    // Get account holder name
    public String getAccountHolderName(Integer accountNumber) {
        return accountRepository.findById(accountNumber)
                .map(BankAccount::getAccountHolder)
                .orElse("Account not found");
    }

    // Update balance directly (for admin operations)
    @Transactional
    public void updateBalance(Integer accountNumber, Double newBalance) {
        BankAccount account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    // Search accounts by name
    public List<BankAccount> searchAccountsByName(String name) {
        return accountRepository.findAll().stream()
                .filter(account -> account.getAccountHolder().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }
}