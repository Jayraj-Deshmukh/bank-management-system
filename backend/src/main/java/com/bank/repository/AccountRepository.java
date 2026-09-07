package com.bank.repository;

import com.bank.entity.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @EntityGraph(attributePaths = {"user"})
    List<Account> findByUserId(Long userId);
}
