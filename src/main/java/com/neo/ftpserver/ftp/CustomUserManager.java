package com.neo.ftpserver.ftp;


import com.neo.ftpserver.cache.AccountFtpCache;
import com.neo.ftpserver.dto.AccountFtp;
import com.neo.ftpserver.permission.IpRestrictionPermission;
import com.neo.ftpserver.util.EnCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomUserManager implements UserManager {

    private final AccountFtpCache accountFtpCache;

    // --- User retrieval ---
    @Override
    public User getUserByName(String username) throws FtpException {
        AccountFtp user = accountFtpCache.getObject(username);
        return user != null ? convertToFtpUser(user) : null;
    }

    @Override
    public String[] getAllUserNames() throws FtpException {
        // Lấy tất cả key trong cache
        return accountFtpCache.getCache().keySet()
                .toArray(new String[0]);
    }

    @Override
    public boolean doesExist(String username) throws FtpException {
        return accountFtpCache.containsKey(username);
    }

    // --- Save / Delete ---
    @Override
    public void save(User user) throws FtpException {
        log.debug("save() called for {}, but ignored (read-only cache mode)", user.getName());
    }

    @Override
    public void delete(String username) throws FtpException {
        log.debug("delete() called for {}, but ignored (read-only cache mode)", username);
    }

    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        if (authentication instanceof UsernamePasswordAuthentication upAuth) {
            String username = upAuth.getUsername();
            String password = upAuth.getPassword();
            AccountFtp user = accountFtpCache.getObject(username);
            if (user == null) throw new AuthenticationFailedException("User not found");
            String passwordEnCode = EnCodeUtils.getEncodeSHA256(password);
            // 🔹 KIỂM TRA LOẠI KẾT NỐI (lấy từ FtpSession)
            if (passwordEnCode.equals(user.getPassword()) && user.getStatus() == 1) {
                log.info("FTP login successful for user {}", username);
                return convertToFtpUser(user);
            }
        }
        log.warn("FTP login failed");
        throw new AuthenticationFailedException("Authentication failed");
    }

    @Override
    public String getAdminName() throws FtpException {
        return "admin";
    }

    @Override
    public boolean isAdmin(String username) throws FtpException {
        return "admin".equals(username);
    }

    // --- Convert AccountFtp → BaseUser ---
    private User convertToFtpUser(AccountFtp accountFtp) {
        BaseUser user = new BaseUser();
        user.setName(accountFtp.getAccount());
        user.setPassword(accountFtp.getPassword());
        String homeDir = accountFtp.getFolderAccess() != null ? accountFtp.getFolderAccess() : "/ftp/" + accountFtp.getAccount();
        createFolder(homeDir);
        user.setHomeDirectory(homeDir);
        user.setEnabled(accountFtp.getStatus() == 1);
        List<Authority> authorities = new ArrayList<>();
        // Giới hạn session
        authorities.add(new ConcurrentLoginPermission(99, 99));
        // Write permission
        if (accountFtp.getRoleAccess() != null && accountFtp.getRoleAccess().contains("WRITE")) {
            authorities.add(new WritePermission());
        }
        // Transfer rate
        authorities.add(new TransferRatePermission(99, 99));
        // IP restriction
        if (accountFtp.getIpList() != null && !accountFtp.getIpList().isBlank()) {
            authorities.add(new IpRestrictionPermission(accountFtp.getIpList()));
        }
        user.setAuthorities(authorities);
        return user;
    }

    public void createFolder(String path) {
        try {
            Files.createDirectories(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục: " + path, e);
        }
    }
}
