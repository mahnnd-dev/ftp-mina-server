package com.neo.ftpserver.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "FTP_AUDIT_LOG")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtpAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String action;
    private String filePath;
    private Long fileSize;
    private String clientIp;
    private boolean isSecure;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private String timestamp;
}
