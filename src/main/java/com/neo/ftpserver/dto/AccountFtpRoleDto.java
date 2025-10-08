package com.neo.ftpserver.dto;

import lombok.Data;

@Data
public class AccountFtpRoleDto {
    private Long id;               // ID nhóm quyền
    private String code;           // Mã nhóm quyền (READ, WRITE, etc.)
    private String description;    // Mô tả quyền
    private Boolean writeEnable;   // Có cho phép ghi hay không
    private Integer idx;           // Thứ tự hiển thị
    private String cmdsDenied;     // Danh sách lệnh FTP bị từ chối
}
