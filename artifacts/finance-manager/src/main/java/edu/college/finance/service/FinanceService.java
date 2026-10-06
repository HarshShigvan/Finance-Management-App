package edu.college.finance.service;

import edu.college.finance.entity.*;
import edu.college.finance.repository.BudgetRepository;
import edu.college.finance.repository.TransactionRepository;
import edu.college.finance.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final UserRepository users;

    public FinanceService(TransactionRepository transactions, BudgetRepository budgets, UserRepository users) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.users = users;
    }

    public List<FinanceTransaction> allTransactions(Long userId) {
        return transactions.findByUserIdOrderByDateDescIdDesc(userId);
    }

    public List<FinanceTransaction> searchTransactions(Long userId, String q, String type) {
        return allTransactions(userId).stream()
                .filter(t -> q == null || q.isBlank()
                        || t.getTitle().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))
                        || t.getCategory().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
                .filter(t -> type == null || type.isBlank() || t.getType().name().equals(type))
                .toList();
    }

    @Transactional
    public FinanceTransaction saveTransaction(Long userId, FinanceTransaction form) {
        FinanceTransaction target = form.getId() == null ? new FinanceTransaction()
                : transactions.findByIdAndUserId(form.getId(), userId)
                    .orElseThrow(() -> new NoSuchElementException("Transaction not found."));
        target.setTitle(form.getTitle().trim());
        target.setAmount(form.getAmount());
        target.setType(form.getType());
        target.setCategory(form.getCategory().trim());
        target.setDate(form.getDate());
        target.setPaymentMethod(form.getPaymentMethod());
        target.setNotes(form.getNotes());
        target.setUser(user(userId));
        return transactions.save(target);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long id) {
        transactions.delete(transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found.")));
    }

    public FinanceTransaction transaction(Long userId, Long id) {
        return transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found."));
    }

    public List<Budget> budgets(Long userId, String month) {
        return budgets.findByUserIdAndMonthOrderByCategory(userId, month);
    }

    @Transactional
    public Budget saveBudget(Long userId, Budget form) {
        Budget target = form.getId() == null ? new Budget()
                : budgets.findByIdAndUserId(form.getId(), userId)
                    .orElseThrow(() -> new NoSuchElementException("Budget not found."));
        target.setCategory(form.getCategory().trim());
        target.setMonthlyLimit(form.getMonthlyLimit());
        target.setMonth(form.getMonth());
        target.setUser(user(userId));
        return budgets.save(target);
    }

    @Transactional
    public void deleteBudget(Long userId, Long id) {
        budgets.delete(budgets.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Budget not found.")));
    }

    public DashboardSummary dashboard(Long userId) {
        List<FinanceTransaction> all = allTransactions(userId);
        YearMonth thisMonth = YearMonth.now();
        List<FinanceTransaction> month = transactions.findByUserIdAndDateBetweenOrderByDateDesc(
                userId, thisMonth.atDay(1), thisMonth.atEndOfMonth());
        BigDecimal income = total(all, TransactionType.INCOME);
        BigDecimal expenses = total(all, TransactionType.EXPENSE);
        BigDecimal monthIncome = total(month, TransactionType.INCOME);
        BigDecimal monthExpenses = total(month, TransactionType.EXPENSE);
        List<Budget> currentBudgets = budgets(userId, thisMonth.toString());
        BigDecimal budgetLimit = currentBudgets.stream().map(Budget::getMonthlyLimit)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal budgetRemaining = currentBudgets.stream()
                .map(b -> b.getMonthlyLimit().subtract(spent(userId, b.getCategory(), thisMonth)))
                .reduce(ZERO, BigDecimal::add);
        BigDecimal budgetSpent = budgetLimit.subtract(budgetRemaining);
        double budgetUsedPercent = budgetLimit.signum() == 0 ? 0
                : budgetSpent.multiply(BigDecimal.valueOf(100)).divide(budgetLimit, 1, RoundingMode.HALF_UP).doubleValue();
        return new DashboardSummary(income.subtract(expenses), income, expenses, monthIncome, monthExpenses,
                monthIncome.subtract(monthExpenses), budgetRemaining, budgetUsedPercent,
                Math.min(100, budgetUsedPercent), all.stream().limit(6).toList(), month);
    }

    public ReportSummary report(Long userId, LocalDate start, LocalDate end) {
        List<FinanceTransaction> period = transactions.findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end);
        BigDecimal income = total(period, TransactionType.INCOME);
        BigDecimal expenses = total(period, TransactionType.EXPENSE);
        Map<String, BigDecimal> categories = period.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(FinanceTransaction::getCategory,
                        TreeMap::new, Collectors.mapping(FinanceTransaction::getAmount,
                                Collectors.reducing(ZERO, BigDecimal::add))));
        List<TrendPoint> trends = new ArrayList<>();
        List<ReportBudget> budgetPerformance = new ArrayList<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth lastMonth = YearMonth.from(end);
        while (!cursor.isAfter(lastMonth)) {
            YearMonth current = cursor;
            List<FinanceTransaction> monthRows = period.stream()
                    .filter(t -> YearMonth.from(t.getDate()).equals(current)).toList();
            trends.add(new TrendPoint(current, total(monthRows, TransactionType.INCOME),
                    total(monthRows, TransactionType.EXPENSE), 0, 0));
            for (Budget budget : budgets(userId, current.toString())) {
                BudgetView view = budgetView(userId, budget);
                budgetPerformance.add(new ReportBudget(current, budget.getCategory(),
                        budget.getMonthlyLimit(), view.spent(), view.percentage(), view.status()));
            }
            cursor = cursor.plusMonths(1);
        }
        BigDecimal peak = trends.stream().map(t -> t.income().max(t.expenses()))
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO).max(BigDecimal.ONE);
        trends = trends.stream().map(t -> new TrendPoint(t.month(), t.income(), t.expenses(),
                t.income().multiply(BigDecimal.valueOf(100)).divide(peak, 1, RoundingMode.HALF_UP).doubleValue(),
                t.expenses().multiply(BigDecimal.valueOf(100)).divide(peak, 1, RoundingMode.HALF_UP).doubleValue()))
                .toList();
        return new ReportSummary(start, end, income, expenses, income.subtract(expenses), categories,
                period.size(), trends, budgetPerformance);
    }

    public Budget budget(Long userId, Long id) {
        return budgets.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Budget not found."));
    }

    public BudgetView budgetView(Long userId, Budget budget) {
        BigDecimal spent = spent(userId, budget.getCategory(), YearMonth.parse(budget.getMonth()));
        BigDecimal remaining = budget.getMonthlyLimit().subtract(spent);
        double percent = budget.getMonthlyLimit().signum() == 0 ? 0
                : spent.multiply(BigDecimal.valueOf(100)).divide(budget.getMonthlyLimit(), 1, RoundingMode.HALF_UP).doubleValue();
        String status = percent >= 100 ? "Exceeded" : percent >= 80 ? "Near limit" : "On track";
        return new BudgetView(budget, spent, remaining, Math.min(percent, 100), status);
    }

    private BigDecimal spent(Long userId, String category, YearMonth month) {
        return transactions.findByUserIdAndDateBetweenOrderByDateDesc(userId, month.atDay(1), month.atEndOfMonth())
                .stream().filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .map(FinanceTransaction::getAmount).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal total(List<FinanceTransaction> list, TransactionType type) {
        return list.stream().filter(t -> t.getType() == type)
                .map(FinanceTransaction::getAmount).reduce(ZERO, BigDecimal::add);
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found."));
    }

    public record DashboardSummary(BigDecimal balance, BigDecimal income, BigDecimal expenses,
            BigDecimal monthIncome, BigDecimal monthExpenses, BigDecimal savings,
            BigDecimal budgetRemaining, double budgetUsedPercent, double budgetProgressPercent,
            List<FinanceTransaction> recent, List<FinanceTransaction> monthTransactions) {}
    public record ReportSummary(LocalDate start, LocalDate end, BigDecimal income, BigDecimal expenses,
            BigDecimal savings, Map<String, BigDecimal> categories, int transactionCount,
            List<TrendPoint> trends, List<ReportBudget> budgetPerformance) {}
    public record TrendPoint(YearMonth month, BigDecimal income, BigDecimal expenses,
            double incomeHeight, double expenseHeight) {}
    public record ReportBudget(YearMonth month, String category, BigDecimal limit, BigDecimal spent,
            double percentage, String status) {}
    public record BudgetView(Budget budget, BigDecimal spent, BigDecimal remaining, double percentage, String status) {}
}
