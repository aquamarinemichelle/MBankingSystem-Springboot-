package com.mbank.controller;

import com.mbank.model.BankAccount;
import com.mbank.service.BankAccountService;
import com.mbank.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class TransferController {

    @Autowired
    private BankAccountService accountService;

    @Autowired
    private TransactionService transactionService;

    // Show transfer page
    @GetMapping("/transfer")
    public String showTransferPage(HttpSession session, Model model) {
        BankAccount account = (BankAccount) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", account);
        return "transfer";
    }

    // Process transfer
    @PostMapping("/transfer")
    public String processTransfer(@RequestParam Integer toAccount,
                                  @RequestParam Double amount,
                                  @RequestParam(required = false) String description,
                                  HttpSession session,
                                  Model model) {

        BankAccount fromAccount = (BankAccount) session.getAttribute("account");
        if (fromAccount == null) {
            return "redirect:/login";
        }

        try {
            // Set common attributes
            model.addAttribute("fromAccountNumber", fromAccount.getAccountNumber());
            model.addAttribute("fromAccountHolder", fromAccount.getAccountHolder());
            model.addAttribute("oldBalance", fromAccount.getBalance());
            model.addAttribute("toAccountNumber", toAccount);
            model.addAttribute("transferAmount", amount);
            model.addAttribute("description", description != null ? description : "");

            // Validations
            if (amount <= 0) {
                model.addAttribute("outcome", "error");
                model.addAttribute("message", "Transfer amount must be greater than zero!");
                return "transfer_outcome";
            }

            if (amount > 100000) {
                model.addAttribute("outcome", "error");
                model.addAttribute("message", "Maximum transfer is R100,000 per transaction!");
                return "transfer_outcome";
            }

            if (toAccount.equals(fromAccount.getAccountNumber())) {
                model.addAttribute("outcome", "error");
                model.addAttribute("message", "Cannot transfer to your own account!");
                return "transfer_outcome";
            }

            // Check if recipient account exists
            if (!accountService.accountExists(toAccount)) {
                model.addAttribute("outcome", "error");
                model.addAttribute("message", "Recipient account #" + toAccount + " not found!");
                return "transfer_outcome";
            }

            String toAccountHolder = accountService.getAccountHolderName(toAccount);
            model.addAttribute("toAccountHolder", toAccountHolder);

            // Calculate fee (R10 for transfers over R1000)
            double fee = amount > 1000 ? 10 : 0;
            double totalDebit = amount + fee;

            // Check if sufficient funds
            if (fromAccount.getBalance() < totalDebit) {
                model.addAttribute("outcome", "error");
                model.addAttribute("message", String.format(
                        "Insufficient funds! Required: R%,.2f (including R%.2f fee). Available: R%,.2f",
                        totalDebit, fee, fromAccount.getBalance()
                ));
                return "transfer_outcome";
            }

            // Perform the transfer
            BankAccount fromAccountUpdated = accountService.getAccount(fromAccount.getAccountNumber())
                    .orElseThrow(() -> new RuntimeException("From account not found"));
            BankAccount toAccountUpdated = accountService.getAccount(toAccount)
                    .orElseThrow(() -> new RuntimeException("To account not found"));

            fromAccountUpdated.setBalance(fromAccountUpdated.getBalance() - totalDebit);
            toAccountUpdated.setBalance(toAccountUpdated.getBalance() + amount);

            accountService.updateAccount(fromAccountUpdated);
            accountService.updateAccount(toAccountUpdated);

            // Generate UNIQUE transaction IDs for each transaction
            String timestamp = String.valueOf(System.currentTimeMillis());
            String debitTransactionId = "TRF-DEBIT-" + timestamp + "-" + (int)(Math.random() * 1000);
            String creditTransactionId = "TRF-CREDIT-" + timestamp + "-" + (int)(Math.random() * 1000);


            transactionService.recordTransactionWithFee(
                    fromAccount.getAccountNumber(),
                    "TRANSFER_DEBIT",
                    -amount,
                    toAccount.toString(),
                    description != null ? description : "Transfer to account #" + toAccount,
                    fee,
                    debitTransactionId
            );

            transactionService.recordTransactionWithFee(
                    toAccount,
                    "TRANSFER_CREDIT",
                    amount,
                    fromAccount.getAccountNumber().toString(),
                    description != null ? description : "Transfer from account #" + fromAccount.getAccountNumber(),
                    0.0,
                    creditTransactionId
            );

            // Update session with new balance
            BankAccount updatedSessionAccount = accountService.getAccount(fromAccount.getAccountNumber())
                    .orElseThrow(() -> new RuntimeException("Account not found after transfer"));
            session.setAttribute("account", updatedSessionAccount);


            model.addAttribute("outcome", "success");
            model.addAttribute("message", "Transfer Successful!");
            model.addAttribute("newBalance", updatedSessionAccount.getBalance());
            model.addAttribute("fee", fee);
            model.addAttribute("totalDebit", totalDebit);
            model.addAttribute("transactionId", debitTransactionId); // Use debit transaction ID for display
            model.addAttribute("transactionDate", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
            ));

            return "transfer_outcome";

        } catch (NumberFormatException e) {
            model.addAttribute("outcome", "error");
            model.addAttribute("message", "Please enter valid account number and amount!");
            return "transfer_outcome";
        } catch (RuntimeException e) {
            model.addAttribute("outcome", "error");
            model.addAttribute("message", e.getMessage());
            return "transfer_outcome";
        } catch (Exception e) {
            model.addAttribute("outcome", "error");
            model.addAttribute("message", "System error: " + e.getMessage());
            e.printStackTrace();
            return "transfer_outcome";
        }
    }
}