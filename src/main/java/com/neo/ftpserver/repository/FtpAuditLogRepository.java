package com.neo.ftpserver.repository;

import com.neo.ftpserver.entity.FtpAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FtpAuditLogRepository extends JpaRepository<FtpAuditLog, Long> {
}
