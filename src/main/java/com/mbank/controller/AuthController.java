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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private BankAccountService accountService;

    // Show login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // Process login
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        try {
            Optional<BankAccount> accountOpt = accountService.login(email, password);

            if (accountOpt.isPresent()) {
                BankAccount account = accountOpt.get();
                session.setAttribute("account", account);
                session.setAttribute("accountNumber", account.getAccountNumber());


                return "redirect:/dashboard";
            } else {
                model.addAttribute("errorMessage", "Invalid email or password!");
                return "login";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "System error during login");
            return "login";
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}