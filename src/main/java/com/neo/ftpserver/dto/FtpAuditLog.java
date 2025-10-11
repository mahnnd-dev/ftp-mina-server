package com.neo.ftpserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtpAuditLog {
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
