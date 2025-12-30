package com.mbank.controller;

import com.mbank.model.BankAccount;
import com.mbank.service.BankAccountService;
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
public class DepositController {

    @Autowired
    private BankAccountService accountService;

    // Show deposit page
    @GetMapping("/deposit")
    public String showDepositPage(HttpSession session, Model model) {
        BankAccount account = (BankAccount) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", account);
        return "deposit";
    }

    // Process deposit
    @PostMapping("/deposit")
    public String processDeposit(@RequestParam Double amount,
                                 HttpSession session,
                                 Model model) {

        BankAccount account = (BankAccount) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        try {
            // Perform deposit
            BankAccount updatedAccount = accountService.deposit(account.getAccountNumber(), amount);

            // Update session with new account data
            session.setAttribute("account", updatedAccount);

            // Generate transaction ID
            String transactionId = "DPT" + System.currentTimeMillis() + (int)(Math.random() * 1000);

            // Prepare model attributes for outcome page
            model.addAttribute("outcome", "success");
            model.addAttribute("message", "Deposit Successful!");
            model.addAttribute("depositAmount", amount);
            model.addAttribute("oldBalance", account.getBalance());
            model.addAttribute("newBalance", updatedAccount.getBalance());
            model.addAttribute("transactionId", transactionId);
            model.addAttribute("transactionDate", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
            ));
            model.addAttribute("accountNumber", account.getAccountNumber());
            model.addAttribute("accountHolder", account.getAccountHolder());

            return "deposit_outcome";

        } catch (RuntimeException e) {
            model.addAttribute("outcome", "error");
            model.addAttribute("message", e.getMessage());
            model.addAttribute("account", account);
            return "deposit";
        }
    }
}