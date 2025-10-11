package com.neo.ftpserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.ftpserver.dto.FtpAuditLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FtpAuditService {

    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger("log-inbound");

    public void logEvent(String username, String action, String filePath, Long fileSize,
                         String clientIp, boolean isSecure) {
        try {
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
            String logApiStr = objectMapper.writeValueAsString(log);
            logger.info("{}", logApiStr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
