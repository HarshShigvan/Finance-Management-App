package edu.college.finance.repository;

import edu.college.finance.entity.FinanceTransaction;
import edu.college.finance.entity.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<FinanceTransaction, Long> {
    List<FinanceTransaction> findByUserIdOrderByDateDescIdDesc(Long userId);
    List<FinanceTransaction> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);
    Optional<FinanceTransaction> findByIdAndUserId(Long id, Long userId);
    List<FinanceTransaction> findByUserIdAndTypeAndDateBetween(Long userId, TransactionType type, LocalDate start, LocalDate end);
}
