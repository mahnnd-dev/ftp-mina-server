package com.neo.ftpserver.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AccountFtpUpdateDto {

    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String account;

    @Size(max = 1000)
    private String description;

    @Min(1)
    @Max(2)
    private Integer typeConnect;

    @Min(0)
    @Max(1)
    private Integer status;

    @NotNull
    private Long updatedBy;

    @Size(max = 100)
    private String password;

    private Long partnerId;

    @Size(max = 500)
    private String folderAccess;

    @Size(max = 12)
    private String roleAccess;

    @Size(max = 1000)
    private String ipList;

    @NotNull
    @Min(1)
    private Long maxLength;

    private Long groupId;

    @NotBlank
    @Size(max = 200)
    private String folderFix;
}
