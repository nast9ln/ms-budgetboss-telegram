package com.repository;

import com.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query(value = "SELECT * FROM expense ORDER BY date DESC LIMIT 1", nativeQuery = true)
    Expense findLastExpense();

    List<Expense> findByDateBetween(Instant start, Instant end);
}
