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
public class WithdrawController {

    @Autowired
    private BankAccountService accountService;

    // Show withdraw page
    @GetMapping("/withdraw")
    public String showWithdrawPage(HttpSession session, Model model) {
        BankAccount account = (BankAccount) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", account);
        return "withdraw";
    }

    // Process withdrawal
    @PostMapping("/withdraw")
    public String processWithdrawal(@RequestParam Double amount,
                                    HttpSession session,
                                    Model model) {

        BankAccount account = (BankAccount) session.getAttribute("account");
        if (account == null) {
            return "redirect:/login";
        }

        try {
            // Perform withdrawal
            BankAccount updatedAccount = accountService.withdraw(account.getAccountNumber(), amount);

            // Update session with new account data
            session.setAttribute("account", updatedAccount);

            // Generate transaction ID
            String transactionId = "WDR" + System.currentTimeMillis() + (int)(Math.random() * 1000);


            model.addAttribute("outcome", "success");
            model.addAttribute("message", "Withdrawal Successful!");
            model.addAttribute("withdrawAmount", amount);
            model.addAttribute("oldBalance", account.getBalance());
            model.addAttribute("newBalance", updatedAccount.getBalance());
            model.addAttribute("transactionId", transactionId);
            model.addAttribute("transactionDate", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
            ));
            model.addAttribute("accountNumber", account.getAccountNumber());
            model.addAttribute("accountHolder", account.getAccountHolder());

            return "withdraw_outcome";

        } catch (RuntimeException e) {
            model.addAttribute("outcome", "error");
            model.addAttribute("message", e.getMessage());
            model.addAttribute("account", account);
            return "withdraw";
        }
    }
}