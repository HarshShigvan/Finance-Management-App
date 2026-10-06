package edu.college.finance.web;

import edu.college.finance.entity.*;
import edu.college.finance.repository.UserRepository;
import edu.college.finance.service.FinanceService;
import edu.college.finance.service.FinanceService.BudgetView;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FinanceController {
    private static final List<String> CATEGORIES = List.of("Salary", "Freelance", "Business", "Bonus", "Other Income",
            "Food", "Shopping", "Rent", "Bills", "Transport", "Entertainment", "Education", "Healthcare", "Travel", "Other Expense");
    private static final List<String> METHODS = List.of("Cash", "Bank Transfer", "UPI", "Credit Card", "Debit Card");
    private final FinanceService finance;
    private final UserRepository users;

    public FinanceController(FinanceService finance, UserRepository users) {
        this.finance = finance;
        this.users = users;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("summary", finance.dashboard(userId(session)));
        return "dashboard";
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String type,
                               HttpSession session, Model model) {
        model.addAttribute("transactions", finance.searchTransactions(userId(session), q, type));
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("form", new FinanceTransaction());
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("methods", METHODS);
        model.addAttribute("today", LocalDate.now());
        return "transactions";
    }

    @PostMapping("/transactions/save")
    public String saveTransaction(@ModelAttribute("form") @Valid FinanceTransaction form, BindingResult errors,
                                  HttpSession session, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("transactions", finance.allTransactions(userId(session)));
            model.addAttribute("categories", CATEGORIES);
            model.addAttribute("methods", METHODS);
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("formError", "Please complete all required fields with a valid amount.");
            return "transactions";
        }
        finance.saveTransaction(userId(session), form);
        flash.addFlashAttribute("success", form.getId() == null ? "Transaction added." : "Transaction updated.");
        return "redirect:/transactions";
    }

    @GetMapping("/transactions/{id}/edit")
    public String editTransaction(@PathVariable Long id, HttpSession session, Model model) {
        model.addAttribute("transactions", finance.allTransactions(userId(session)));
        model.addAttribute("form", finance.transaction(userId(session), id));
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("methods", METHODS);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("editing", true);
        return "transactions";
    }

    @PostMapping("/transactions/{id}/delete")
    public String deleteTransaction(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        finance.deleteTransaction(userId(session), id);
        flash.addFlashAttribute("success", "Transaction deleted.");
        return "redirect:/transactions";
    }

    @GetMapping("/budgets")
    public String budgets(@RequestParam(required = false) String month, HttpSession session, Model model) {
        String selectedMonth = validMonth(month);
        List<BudgetView> rows = finance.budgets(userId(session), selectedMonth).stream()
                .map(b -> finance.budgetView(userId(session), b)).toList();
        model.addAttribute("month", selectedMonth);
        model.addAttribute("budgets", rows);
        model.addAttribute("form", new Budget());
        model.addAttribute("categories", CATEGORIES.subList(5, CATEGORIES.size()));
        return "budgets";
    }

    @PostMapping("/budgets/save")
    public String saveBudget(@ModelAttribute("form") @Valid Budget form, BindingResult errors,
                             HttpSession session, Model model, RedirectAttributes flash) {
        try {
            YearMonth.parse(form.getMonth());
        } catch (Exception ex) {
            errors.rejectValue("month", "invalid", "Choose a valid month.");
        }
        if (errors.hasErrors()) {
            String selectedMonth = validMonth(form.getMonth());
            model.addAttribute("month", selectedMonth);
            model.addAttribute("budgets", finance.budgets(userId(session), selectedMonth).stream()
                    .map(b -> finance.budgetView(userId(session), b)).toList());
            model.addAttribute("categories", CATEGORIES.subList(5, CATEGORIES.size()));
            model.addAttribute("formError", "Please enter a category, month and valid budget amount.");
            return "budgets";
        }
        finance.saveBudget(userId(session), form);
        flash.addFlashAttribute("success", "Budget saved.");
        return "redirect:/budgets?month=" + form.getMonth();
    }

    @GetMapping("/budgets/{id}/edit")
    public String editBudget(@PathVariable Long id, HttpSession session, Model model) {
        Budget target = finance.budget(userId(session), id);
        model.addAttribute("month", target.getMonth());
        model.addAttribute("budgets", finance.budgets(userId(session), target.getMonth()).stream()
                .map(b -> finance.budgetView(userId(session), b)).toList());
        model.addAttribute("form", target);
        model.addAttribute("categories", CATEGORIES.subList(5, CATEGORIES.size()));
        model.addAttribute("editing", true);
        return "budgets";
    }

    @PostMapping("/budgets/{id}/delete")
    public String deleteBudget(@PathVariable Long id, @RequestParam String month,
                               HttpSession session, RedirectAttributes flash) {
        finance.deleteBudget(userId(session), id);
        flash.addFlashAttribute("success", "Budget deleted.");
        return "redirect:/budgets?month=" + validMonth(month);
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) String start,
                          @RequestParam(required = false) String end,
                          HttpSession session, Model model) {
        LocalDate defaultStart = YearMonth.now().atDay(1);
        LocalDate defaultEnd = LocalDate.now();
        LocalDate from = parseDate(start, defaultStart);
        LocalDate to = parseDate(end, defaultEnd);
        if (from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        model.addAttribute("report", finance.report(userId(session), from, to));
        model.addAttribute("start", from);
        model.addAttribute("end", to);
        return "reports";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        model.addAttribute("profile", users.findById(userId(session)).orElseThrow());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String name, @RequestParam(required = false) String phone,
                                HttpSession session, RedirectAttributes flash) {
        User user = users.findById(userId(session)).orElseThrow();
        if (name == null || name.isBlank() || name.length() > 80) {
            flash.addFlashAttribute("error", "Enter a name up to 80 characters.");
            return "redirect:/profile";
        }
        user.setName(name.trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        users.save(user);
        session.setAttribute("userName", user.getName());
        flash.addFlashAttribute("success", "Profile updated.");
        return "redirect:/profile";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        model.addAttribute("profile", users.findById(userId(session)).orElseThrow());
        return "settings";
    }

    @PostMapping("/settings/preferences")
    public String preferences(@RequestParam String currency, @RequestParam(required = false) String notifications,
                              HttpSession session, RedirectAttributes flash) {
        if (!List.of("INR", "USD", "EUR", "GBP").contains(currency)) {
            flash.addFlashAttribute("error", "Select a supported currency.");
            return "redirect:/settings";
        }
        User user = users.findById(userId(session)).orElseThrow();
        user.setCurrency(currency);
        user.setNotificationsEnabled(notifications != null);
        users.save(user);
        flash.addFlashAttribute("success", "Preferences saved.");
        return "redirect:/settings";
    }

    @PostMapping("/settings/password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                @RequestParam String confirmPassword, HttpSession session,
                                org.springframework.security.crypto.password.PasswordEncoder encoder,
                                RedirectAttributes flash) {
        User user = users.findById(userId(session)).orElseThrow();
        if (!encoder.matches(currentPassword, user.getPassword())) {
            flash.addFlashAttribute("error", "Current password is incorrect.");
        } else if (newPassword.length() < 8 || newPassword.length() > 72) {
            flash.addFlashAttribute("error", "New password must be at least 8 characters.");
        } else if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "New passwords do not match.");
        } else {
            user.setPassword(encoder.encode(newPassword));
            users.save(user);
            flash.addFlashAttribute("success", "Password changed.");
        }
        return "redirect:/settings";
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() { return "ok"; }

    private Long userId(HttpSession session) { return (Long) session.getAttribute("userId"); }
    private String validMonth(String month) {
        try { return YearMonth.parse(month).toString(); }
        catch (Exception ex) { return YearMonth.now().toString(); }
    }
    private LocalDate parseDate(String value, LocalDate fallback) {
        try { return LocalDate.parse(value); }
        catch (Exception ex) { return fallback; }
    }
}
