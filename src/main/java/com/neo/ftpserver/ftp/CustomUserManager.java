package com.neo.ftpserver.ftp;


import com.neo.ftpserver.entity.AccountFtp;
import com.neo.ftpserver.permission.IpRestrictionPermission;
import com.neo.ftpserver.repository.AccountFtpRepository;
import com.neo.ftpserver.util.EnCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomUserManager implements UserManager {

    private final AccountFtpRepository accountFtpRepository;


    @Override
    public User getUserByName(String username) throws FtpException {
        return accountFtpRepository.findByAccount(username)
                .map(this::convertToFtpUser)
                .orElse(null);
    }

    @Override
    public String[] getAllUserNames() throws FtpException {
        List<AccountFtp> users = accountFtpRepository.findAll();
        return users.stream()
                .map(AccountFtp::getAccount)
                .toArray(String[]::new);
    }

    @Override
    public void delete(String username) throws FtpException {
        accountFtpRepository.findByAccount(username)
                .ifPresent(accountFtpRepository::delete);
    }

    @Override
    public void save(User user) throws FtpException {
        AccountFtp ftpUser = accountFtpRepository.findByAccount(user.getName())
                .orElse(new AccountFtp());

        ftpUser.setAccount(user.getName());
        ftpUser.setPassword(EnCodeUtils.getEncodeSHA256(user.getPassword()));
        ftpUser.setFolderAccess(user.getHomeDirectory());
        ftpUser.setStatus(user.getEnabled() ? 1 : 0);
        accountFtpRepository.save(ftpUser);
    }

    @Override
    public boolean doesExist(String username) throws FtpException {
        return accountFtpRepository.existsByAccount(username);
    }

    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        if (authentication instanceof UsernamePasswordAuthentication upAuth) {
            String username = upAuth.getUsername();
            String password = upAuth.getPassword();
            AccountFtp user = accountFtpRepository.findByAccount(username).orElse(null);
            if (user == null) throw new AuthenticationFailedException("User not found");
            String passwordEnCode = EnCodeUtils.getEncodeSHA256(password);
            if (passwordEnCode.equals(user.getPassword()) && user.getStatus() == 1) {
                return convertToFtpUser(user);
            }
        }
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

    private User convertToFtpUser(AccountFtp ftpUser) {
        BaseUser user = new BaseUser();
        user.setName(ftpUser.getAccount());
        user.setPassword(ftpUser.getPassword());
        user.setHomeDirectory(ftpUser.getFolderAccess() != null ? ftpUser.getFolderAccess() : "/ftp/" + ftpUser.getAccount());
        user.setEnabled(ftpUser.getStatus() == 1);

        List<Authority> authorities = new ArrayList<>();
//        Cho phép: upload file, tạo thư mục, rename file/folder, xóa file/folder.
//        Không có quyền này → chỉ được phép read-only (download, list file).
//        Giới hạn session
        authorities.add(new ConcurrentLoginPermission(99, 99));
        if (ftpUser.getRoleAccess() != null && ftpUser.getRoleAccess().contains("WRITE")) {
            authorities.add(new WritePermission());
        }

        authorities.add(new TransferRatePermission(0, // downloadRate (KB/s)
                ftpUser.getMaxLength().intValue() * 1024 // uploadRate (KB/s)
        ));

        if (ftpUser.getIpList() != null && !ftpUser.getIpList().isBlank()) {
            authorities.add(new IpRestrictionPermission(ftpUser.getIpList()));
        }
        user.setAuthorities(authorities);
        return user;
    }
}
