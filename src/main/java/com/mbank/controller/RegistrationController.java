package com.mbank.controller;

import com.mbank.model.BankAccount;
import com.mbank.service.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistrationController {

    @Autowired
    private BankAccountService accountService;

    // Show registration page
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    // Process registration
    @PostMapping("/register")
    public String register(@RequestParam String accountHolder,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {

        // Validation
        if (accountHolder == null || accountHolder.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Full name is required!");
            return "register";
        }

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            model.addAttribute("errorMessage", "Valid email is required!");
            return "register";
        }

        if (password == null || password.trim().isEmpty() || password.length() < 6) {
            model.addAttribute("errorMessage", "Password must be at least 6 characters!");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match!");
            return "register";
        }

        try {
            // Create account
            BankAccount account = accountService.registerAccount(
                    accountHolder.trim(),
                    email.trim(),
                    password
            );

            model.addAttribute("successMessage",
                    "Successfully registered! Account #" + account.getAccountNumber() +
                            " created. You can now login.");

            return "login";

        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }
}