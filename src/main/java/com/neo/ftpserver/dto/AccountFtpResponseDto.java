package com.neo.ftpserver.dto;

import lombok.Data;

@Data
public class AccountFtpResponseDto {

    private Long id;

    private String account;

    private String description;

    private Integer typeConnect; // 1: FTP, 2: FTPS

    private Integer status;

    private Long updatedBy;

    private String password;

    private Long partnerId;

    private String folderAccess;

    private String roleAccess;

    private String ipList;

    private Long maxLength;

    private Long groupId;

    private String folderFix;

    private String updatedDate; // ISO-8601 format (yyyy-MM-dd'T'HH:mm:ss)
}
