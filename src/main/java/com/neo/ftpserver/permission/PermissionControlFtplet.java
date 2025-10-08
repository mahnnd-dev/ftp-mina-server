package com.neo.ftpserver.permission;

import com.neo.ftpserver.cache.AccountFtpCache;
import com.neo.ftpserver.dto.AccountFtp;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PermissionControlFtplet extends DefaultFtplet {

    @Autowired
    private AccountFtpCache accountFtpCache;

    @Override
    public FtpletResult beforeCommand(FtpSession session, FtpRequest request)
            throws FtpException, IOException {

        User user = session.getUser();
        if (user == null) {
            return FtpletResult.DEFAULT;
        }

        String username = user.getName();
        String command = request.getCommand().toUpperCase();
        String argument = request.getArgument();

        AccountFtp ftpUser = accountFtpCache.getObject(username);
        if (ftpUser == null) {
            return FtpletResult.DEFAULT;
        }
        log.info("--> RoleAccess: {}", ftpUser.getRoleAccess());
        // 🔹 Kiểm tra quyền DELETE (DELE = delete file, RMD = remove directory)
        if ("DELE".equals(command) || "RMD".equals(command)) {
            if (!ftpUser.getRoleAccess().equals("DELETE")) {
                log.warn("User {} denied DELETE: {}", username, argument);
                session.write(new DefaultFtpReply(550, "Permission denied: Delete not allowed"));
                return FtpletResult.SKIP;
            }
        }

        // 🔹 Kiểm tra quyền WRITE (STOR = upload, APPE = append, MKD = mkdir)
        if ("STOR".equals(command) || "APPE".equals(command) || "MKD".equals(command)) {
            if (!ftpUser.getRoleAccess().equals("WRITE")) {
                log.warn("User {} denied WRITE: {}", username, argument);
                session.write(new DefaultFtpReply(550, "Permission denied: Write not allowed"));
                return FtpletResult.SKIP;
            }
        }

        // 🔹 Kiểm tra quyền READ (RETR = download)
        if ("RETR".equals(command)) {
            if (!ftpUser.getRoleAccess().equals("READ")) {
                log.warn("User {} denied READ: {}", username, argument);
                session.write(new DefaultFtpReply(550, "Permission denied: Read not allowed"));
                return FtpletResult.SKIP;
            }
        }

        // 🔹 Kiểm tra quyền LIST (LIST, NLST, MLSD)
        if ("LIST".equals(command) || "NLST".equals(command) || "MLSD".equals(command)) {
            if (!ftpUser.getRoleAccess().equals("LIST")) {
                log.warn("User {} denied LIST", username);
                session.write(new DefaultFtpReply(550, "Permission denied: List not allowed"));
                return FtpletResult.SKIP;
            }
        }

        // 🔹 Kiểm tra quyền RENAME (RNFR/RNTO)
        if ("RNFR".equals(command) || "RNTO".equals(command)) {
            if (!ftpUser.getRoleAccess().equals("RENAME")) { // Rename cần quyền write
                log.warn("User {} denied RENAME: {}", username, argument);
                session.write(new DefaultFtpReply(550, "Permission denied: Rename not allowed"));
                return FtpletResult.SKIP;
            }
        }

        return FtpletResult.DEFAULT;
    }
}
