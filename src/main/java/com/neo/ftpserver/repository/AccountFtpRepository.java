package com.neo.ftpserver.repository;

import com.neo.ftpserver.entity.AccountFtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountFtpRepository extends JpaRepository<AccountFtp, Long> {
    Optional<AccountFtp> findByAccount(String username);

    boolean existsByAccount(String username);
}
