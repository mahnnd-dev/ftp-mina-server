package com.neo.ftpserver.permission;

import com.neo.ftpserver.cache.AccountFtpCache;
import com.neo.ftpserver.dto.AccountFtp;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ConnectionTypeFtplet extends DefaultFtplet {

    @Autowired
    private AccountFtpCache accountFtpCache;
    private static final String USERNAME_ATTR = "ftp.username";

    @Override
    public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {

        String command = request.getCommand().toUpperCase();

        // 🔹 Lưu username khi nhận lệnh USER
        if ("USER".equals(command)) {
            String username = request.getArgument();
            session.setAttribute(USERNAME_ATTR, username);
            return FtpletResult.DEFAULT;
        }

        // 🔹 Kiểm tra TRƯỚC KHI xử lý lệnh PASS
        if ("PASS".equals(command)) {
            String username = (String) session.getAttribute(USERNAME_ATTR);
            if (username == null) {
                return FtpletResult.DEFAULT;
            }

            String clientIp = session.getClientAddress().getAddress().getHostAddress();
            AccountFtp ftpUser = accountFtpCache.getObject(username);

            if (ftpUser == null) {
                return FtpletResult.DEFAULT;
            }
            boolean isSecure = session.isSecure();
            log.info("User {} authenticating from {} - isSecure: {}, required type: {}",
                    username, clientIp, isSecure, ftpUser.getTypeConnect());
            // ✅ Kiểm tra loại kết nối TRƯỚC KHI authenticate
            if (ftpUser.getTypeConnect() == 2 && !isSecure) {
                log.warn("❌ User {} requires FTPS but connected via plain FTP from {}", username, clientIp);
                session.write(new DefaultFtpReply(530, "User requires FTPS connection"));
                if (session instanceof FtpIoSession ioSession) {
                    ioSession.closeOnFlush();
                }
                return FtpletResult.SKIP; // 🔹 BỎ QUA xử lý lệnh PASS
            }
            if (ftpUser.getTypeConnect() == 1 && isSecure) {
                log.warn("❌ User {} requires plain FTP but connected via FTPS from {}", username, clientIp);
                session.write(new DefaultFtpReply(530, "User not allowed to connect via FTPS"));
                if (session instanceof FtpIoSession ioSession) {
                    ioSession.closeOnFlush();
                }
                return FtpletResult.SKIP; // 🔹 BỎ QUA xử lý lệnh PASS
            }
        }
        return FtpletResult.DEFAULT;
    }


    @Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request) throws FtpException, IOException {
        String username = session.getUser().getName();
        String clientIp = session.getClientAddress().getAddress().getHostAddress();

        AccountFtp ftpUser = accountFtpCache.getObject(username);
        if (ftpUser == null) {
            return FtpletResult.DEFAULT;
        }
        boolean isSecure = session.isSecure();

        if (ftpUser.getTypeConnect() == 2 && !isSecure) {
            log.warn("❌ User {} requires FTPS but connected via FTP - disconnecting", username);
            session.write(new DefaultFtpReply(530, "User requires FTPS connection"));
            if (session instanceof FtpIoSession ioSession) {
                ioSession.closeOnFlush();
            }
            return FtpletResult.DISCONNECT;
        } else if (ftpUser.getTypeConnect() == 1 && isSecure) {
            log.warn("❌ User {} requires FTP but connected via FTPS - disconnecting", username);
            session.write(new DefaultFtpReply(530, "User not allowed to connect via FTPS"));
            if (session instanceof FtpIoSession ioSession) {
                ioSession.closeOnFlush();
            }
            return FtpletResult.DISCONNECT;
        }
        log.info("User {} connected from {} via {}", username, clientIp, isSecure ? "FTPS" : "FTP");
        return FtpletResult.DEFAULT;
    }
}

