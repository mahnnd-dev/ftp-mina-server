package com.neo.ftpserver.ftp;


import com.neo.ftpserver.cache.AccountFtpCache;
import com.neo.ftpserver.dto.AccountFtp;
import com.neo.ftpserver.permission.IpRestrictionPermission;
import com.neo.ftpserver.util.EnCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String homeDir = buildHomeDirectory(accountFtp);
        user.setHomeDirectory(homeDir);
        user.setEnabled(accountFtp.getStatus() == 1);
        List<Authority> authorities = new ArrayList<>();
        // IP restriction
        if (accountFtp.getIpList() != null && !accountFtp.getIpList().isBlank()) {
            authorities.add(new IpRestrictionPermission(accountFtp.getIpList()));
        }
        authorities.add(new WritePermission());
        authorities.add(new ConcurrentLoginPermission(99, 99));
        user.setAuthorities(authorities);
        return user;
    }

    private void createFolder(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created directory: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to create directory: {}", path, e);
            throw new RuntimeException("Failed to create home directory", e);
        }
    }

    public String buildHomeDirectory(AccountFtp accountFtp) {
        String folderAccess = accountFtp.getFolderAccess();
        String folderFix = accountFtp.getFolderFix();
        String account = accountFtp.getAccount();

        // Dùng Paths.get() để tự động xử lý separator
        Path homePath = Paths.get("ftp");

        if (folderAccess != null && !folderAccess.trim().isEmpty()) {
            homePath = homePath.resolve(folderAccess.trim());
        }
        if (folderFix != null && !folderFix.trim().isEmpty()) {
            homePath = homePath.resolve(folderFix.trim());
        }
        homePath = homePath.resolve(account);
        // Normalize và convert về absolute path
        Path normalizedPath = homePath.normalize().toAbsolutePath();

        createFolder(normalizedPath);

        return normalizedPath.toString();
    }
}
