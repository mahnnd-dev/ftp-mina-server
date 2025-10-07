package com.neo.ftpserver.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Account_Ftp")
@Comment("Thông tin cấu hình kết nối FTP cho đối tác")
public class AccountFtp {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_ftp_seq")
    @SequenceGenerator(
            name = "account_ftp_seq",
            sequenceName = "SEQ_ACCOUNT_FTP_ID",
            allocationSize = 1
    )
    @Comment("ID của bản ghi")
    private Long id;

    @Column(name = "ACCOUNT", nullable = false, length = 100)
    @Comment("Account truy cập server FTP")
    private String account;

    @Column(name = "DESCRIPTION", length = 1000)
    @Comment("Mô tả")
    private String description;

    @Column(name = "TYPE_CONNECT", columnDefinition = "NUMBER(1,0)")
    @Comment("Loại kết nối, 1: FTP; 2: FTPS")
    private Integer typeConnect;

    @Column(name = "STATUS", columnDefinition = "NUMBER(1,0)")
    @Comment("Trạng thái, 1: Active; 0: Inactive")
    private Integer status;

    @Column(name = "UPDATED_BY", nullable = false)
    @Comment("ID người thực hiện")
    private Long updatedBy;

    @Column(name = "UPDATED_DATE", nullable = false)
    @Comment("Ngày thực hiện")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedDate;

    @Column(name = "PASSWORD", length = 100)
    @Comment("Password")
    private String password;

    @Column(name = "PARTNER_ID")
    @Comment("ID của đối tác")
    private Long partnerId;

    @Column(name = "FOLDER_ACCESS", length = 500)
    @Comment("Thư mục được truy cập")
    private String folderAccess;

    @Column(name = "ROLE_ACCESS", length = 12)
    @Comment("Các quyền được truy cập, cách nhau dấu (,)")
    private String roleAccess;

    @Column(name = "IP_LIST", length = 1000)
    @Comment("Danh sách IP truy cập, cách nhau dấu (,)")
    private String ipList;

    @Column(name = "MAX_LENGTH", nullable = false)
    @Comment("Đơn vị MB")
    private Long maxLength = 50L;

    @Column(name = "GROUP_ID")
    @Comment("ID nhóm FTP")
    private Long groupId;

    @Column(name = "FOLDER_FIX", nullable = false, length = 200)
    @Comment("Thư mục cố định")
    private String folderFix;

    @PrePersist
    public void prePersist() {
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
