package edu.college.finance.service;

import edu.college.finance.entity.*;
import edu.college.finance.repository.BudgetRepository;
import edu.college.finance.repository.TransactionRepository;
import edu.college.finance.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(UserRepository users, TransactionRepository transactions,
                          BudgetRepository budgets, PasswordEncoder encoder) {
        this.users = users;
        this.transactions = transactions;
        this.budgets = budgets;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.findByEmailIgnoreCase("viewer@demo.com").isPresent()) return;
        User viewer = new User();
        viewer.setName("Alex Morgan");
        viewer.setUsername("alexmorgan");
        viewer.setEmail("viewer@demo.com");
        viewer.setPassword(encoder.encode("viewer123"));
        viewer.setPhone("+91 98765 43210");
        viewer.setCurrency("INR");
        viewer = users.save(viewer);

        LocalDate today = LocalDate.now();
        record(viewer, "Monthly salary", "Salary", TransactionType.INCOME, "52000", "Bank Transfer", today.withDayOfMonth(1), "Primary income");
        record(viewer, "Freelance design", "Freelance", TransactionType.INCOME, "8500", "UPI", today.minusDays(4), "Brand identity project");
        record(viewer, "Apartment rent", "Rent", TransactionType.EXPENSE, "14000", "Bank Transfer", today.withDayOfMonth(2), "Monthly rent");
        record(viewer, "Groceries", "Food", TransactionType.EXPENSE, "2450", "UPI", today.minusDays(1), "Weekly groceries");
        record(viewer, "Metro pass", "Transport", TransactionType.EXPENSE, "1200", "Debit Card", today.minusDays(2), "Monthly commute");
        record(viewer, "Internet and mobile", "Bills", TransactionType.EXPENSE, "1399", "Credit Card", today.minusDays(5), "Monthly utilities");
        record(viewer, "Course materials", "Education", TransactionType.EXPENSE, "2150", "UPI", today.minusDays(7), "Semester reading");
        record(viewer, "Dinner with friends", "Food", TransactionType.EXPENSE, "980", "Credit Card", today.minusDays(9), "Weekend dinner");
        record(viewer, "Quarterly bonus", "Bonus", TransactionType.INCOME, "12000", "Bank Transfer", today.minusMonths(1).withDayOfMonth(20), "Performance bonus");
        record(viewer, "Home supplies", "Shopping", TransactionType.EXPENSE, "3200", "Debit Card", today.minusMonths(1).withDayOfMonth(17), "Household items");
        record(viewer, "Electricity bill", "Bills", TransactionType.EXPENSE, "1860", "UPI", today.minusMonths(1).withDayOfMonth(12), "Monthly bill");
        record(viewer, "Weekend trip", "Travel", TransactionType.EXPENSE, "5400", "Credit Card", today.minusMonths(2).withDayOfMonth(11), "Pune weekend");
        record(viewer, "Freelance project", "Freelance", TransactionType.INCOME, "11500", "Bank Transfer", today.minusMonths(2).withDayOfMonth(8), "Landing page project");
        record(viewer, "Health check-up", "Healthcare", TransactionType.EXPENSE, "1750", "UPI", today.minusMonths(3).withDayOfMonth(14), "Annual check-up");
        record(viewer, "Streaming annual plan", "Entertainment", TransactionType.EXPENSE, "2499", "Credit Card", today.minusMonths(4).withDayOfMonth(5), "Annual subscription");
        record(viewer, "Scholarship stipend", "Other Income", TransactionType.INCOME, "6000", "Bank Transfer", today.minusMonths(5).withDayOfMonth(3), "College stipend");
        seedBudget(viewer, "Food", "9000");
        seedBudget(viewer, "Transport", "4000");
        seedBudget(viewer, "Shopping", "6000");
        seedBudget(viewer, "Bills", "5000");
        seedBudget(viewer, "Entertainment", "3500");
        seedBudget(viewer, "Education", "5000");
        seedBudget(viewer, "Healthcare", "3000");
        seedBudget(viewer, "Travel", "8000");
    }

    private void record(User user, String title, String category, TransactionType type, String amount,
                        String method, LocalDate date, String notes) {
        FinanceTransaction row = new FinanceTransaction();
        row.setUser(user);
        row.setTitle(title);
        row.setCategory(category);
        row.setType(type);
        row.setAmount(new BigDecimal(amount));
        row.setPaymentMethod(method);
        row.setDate(date);
        row.setNotes(notes);
        transactions.save(row);
    }

    private void seedBudget(User user, String category, String limit) {
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonthlyLimit(new BigDecimal(limit));
        budget.setMonth(YearMonth.now().toString());
        budgets.save(budget);
    }
}
