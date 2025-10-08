package com.neo.ftpserver.service;

import com.neo.ftpserver.entity.FtpAuditLog;
import com.neo.ftpserver.repository.FtpAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FtpAuditService {

    private final FtpAuditLogRepository repository;

    @Async
    public void logEvent(String username, String action, String filePath, Long fileSize,
                         String clientIp, boolean isSecure) {
        LocalDateTime timestamp = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formatted = timestamp.format(formatter);
        FtpAuditLog log = FtpAuditLog.builder()
                .username(username)
                .action(action)
                .filePath(filePath)
                .fileSize(fileSize)
                .clientIp(clientIp)
                .isSecure(isSecure)
                .timestamp(formatted)
                .build();
        repository.save(log);
    }
}
