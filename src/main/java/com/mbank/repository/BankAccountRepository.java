package com.mbank.repository;

import com.mbank.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Integer> {


    Optional<BankAccount> findByEmail(String email);

    Optional<BankAccount> findByEmailAndPassword(String email, String password);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE BankAccount b SET b.balance = :balance WHERE b.accountNumber = :accountNumber")
    void updateBalance(@Param("accountNumber") Integer accountNumber,
                       @Param("balance") Double balance);
}