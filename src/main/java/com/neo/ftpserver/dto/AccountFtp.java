package com.neo.ftpserver.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountFtp {
    private String account;
    private String description;
    private Integer typeConnect;
    private Integer status;
    private Long updatedBy;
    private LocalDateTime updatedDate;
    private String password;
    private Long partnerId;
    private String folderAccess;
    private String roleAccess;
    private String ipList;
    private Long maxLength = 50l;
    private Long groupId;
    private String folderFix;
}
