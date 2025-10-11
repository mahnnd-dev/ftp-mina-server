package com.neo.ftpserver.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.ftpserver.dto.FtpAuditLog;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;

@Slf4j
@Component
public class LogFtp extends AbstractJobProcessLog {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    @Value("${logging.job.sql-insert}")
    private String sql;

    @Autowired
    public LogFtp(
            @Qualifier("logJdbcTemplate") JdbcTemplate logJdbcTemplate,
            PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper
    ) {
        super(logJdbcTemplate, transactionManager);
        this.objectMapper = objectMapper;
        this.jdbcTemplate = logJdbcTemplate;
    }

    @Override
    public void processData(String data) {
        try {
            if (data == null || data.trim().isEmpty()) {
                log.warn("Empty data line, skipping");
                return;
            }
            data = data.trim();
            FtpAuditLog ftpAuditLog = objectMapper.readValue(data, FtpAuditLog.class);
            Object[] paramsArray = {
                    ftpAuditLog.getUsername(),
                    ftpAuditLog.getAction(),
                    ftpAuditLog.getFilePath(),
                    ftpAuditLog.getFileSize(),
                    ftpAuditLog.getClientIp(),
                    ftpAuditLog.isSecure(),
                    ftpAuditLog.getTimestamp()};
            log.info("#paramsArray: {}", Arrays.toString(paramsArray));
            addBatchInsert(paramsArray);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while processing data: {}", e.getMessage());
        }
    }

    @Override
    public String getSql() {
        return sql;
    }

    @PostConstruct
    public void init() {
        if (!tableExists()) {
            createTable();
        }
    }

    private boolean tableExists() {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM " + "ftp_audit_log" + " WHERE 1 = 0", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void createTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE ftp_audit_log (id NUMBER PRIMARY KEY, username VARCHAR2(255), action VARCHAR2(50), file_path VARCHAR2(500), file_size NUMBER, client_ip VARCHAR2(50), is_secure NUMBER(1), timestamp TIMESTAMP)");
            log.info("✅ Created table ftp_audit_log in schema");
        } catch (Exception e) {
            e.getMessage();
        }
    }
}