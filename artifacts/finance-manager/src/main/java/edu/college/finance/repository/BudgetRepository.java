package edu.college.finance.repository;

import edu.college.finance.entity.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndMonthOrderByCategory(Long userId, String month);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}
