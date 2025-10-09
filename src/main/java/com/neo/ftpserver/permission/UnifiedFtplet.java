package com.neo.ftpserver.permission;

import com.neo.ftpserver.cache.AccountFtpCache;
import com.neo.ftpserver.cache.AccountFtpRoleCache;
import com.neo.ftpserver.dto.AccountFtpDto;
import com.neo.ftpserver.dto.AccountFtpRoleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedFtplet extends DefaultFtplet {

    private final AccountFtpCache accountFtpCache;
    private final AccountFtpRoleCache accountFtpRoleCache;

    @Override
    public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
        String command = request.getCommand().toUpperCase();
        String clientIp = session.getClientAddress().getAddress().getHostAddress();

        log.debug("Processing command: {} from IP: {}", command, clientIp);

        // Bước 1: Kiểm tra USER command - chỉ validate IP tại thời điểm login
        if ("USER".equals(command)) {
            return handleUserCommand(session, request, clientIp);
        }

        // Bước 2: Kiểm tra PASS command - validate login và connection type
        if ("PASS".equals(command)) {
            return handlePassCommand(session, request, clientIp);
        }

        // Bước 3: Kiểm tra các lệnh khác - validate permissions
        return handleOtherCommands(session, command);
    }

    /**
     * Xử lý lệnh USER - Chỉ kiểm tra IP whitelist
     */
    private FtpletResult handleUserCommand(FtpSession session, FtpRequest request, String clientIp) throws FtpException {
        String username = request.getArgument();
        log.info("User login attempt: {} from IP: {}", username, clientIp);

        // Lấy thông tin user từ cache để kiểm tra IP
        AccountFtpDto ftpUser = accountFtpCache.getObject(username);
        if (ftpUser == null) {
            log.warn("User not found in cache: {}", username);
            return FtpletResult.DEFAULT; // Để FTP server xử lý authentication
        }

        // Kiểm tra IP whitelist
        if (!isIpAllowedForUser(ftpUser, clientIp)) {
            log.warn("IP {} not allowed for user: {}", clientIp, username);
            session.write(new DefaultFtpReply(530, "Connection not allowed from your IP address"));
            closeSession(session);
            return FtpletResult.SKIP;
        }

        log.info("IP {} is allowed for user: {}", clientIp, username);
        return FtpletResult.DEFAULT;
    }

    /**
     * Xử lý lệnh PASS - Kiểm tra connection type sau khi authenticate
     */
    private FtpletResult handlePassCommand(FtpSession session, FtpRequest request, String clientIp) {
        // Lưu ý: User chưa được authenticate tại thời điểm này
        // Cần kiểm tra connection type sau khi login thành công
        return FtpletResult.DEFAULT;
    }

    /**
     * Xử lý các lệnh khác - Kiểm tra connection type và command permissions
     */
    private FtpletResult handleOtherCommands(FtpSession session, String command) throws FtpException {
        User user = session.getUser();
        if (user == null) {
            log.debug("No authenticated user for command: {}", command);
            return FtpletResult.DEFAULT;
        }

        String username = user.getName();
        AccountFtpDto ftpUser = accountFtpCache.getObject(username);
        if (ftpUser == null) {
            log.warn("User data not found in cache: {}", username);
            return FtpletResult.DEFAULT;
        }

        // Kiểm tra connection type (chỉ lần đầu sau khi login)
        if (!isConnectionTypeAllowed(session, ftpUser)) {
            log.warn("Connection type not allowed for user: {}", username);
            session.write(new DefaultFtpReply(530, "Connection type (FTP/FTPS) not allowed for your account"));
            closeSession(session);
            return FtpletResult.SKIP;
        }

        // Kiểm tra command permissions
        AccountFtpRoleDto roleDto = accountFtpRoleCache.getObject(ftpUser.getRoleAccess());
        if (roleDto != null && roleDto.getCmdsDenied() != null && roleDto.getCmdsDenied().contains(command)) {
            log.warn("Command {} denied for user {} with role {}", command, username, ftpUser.getRoleAccess());
            session.write(new DefaultFtpReply(550, "Permission denied: Command not allowed"));
            return FtpletResult.SKIP;
        }

        return FtpletResult.DEFAULT;
    }

    /**
     * Kiểm tra IP có được phép cho user không
     * Sử dụng IpRestrictionPermission từ authorities
     */
    private boolean isIpAllowedForUser(AccountFtpDto ftpUser, String clientIp) {
        // Nếu không có IP restriction, cho phép tất cả
        if (ftpUser.getIpList() == null || ftpUser.getIpList().isEmpty()) {
            log.debug("No IP restriction for user: {}", ftpUser.getAccount());
            return true;
        }
        if (ftpUser.getIpList().contains("*")) {
            return true;
        }
        List<String> stringList = Arrays.stream(ftpUser.getIpList().split(",")).map(String::trim).toList();
        // Kiểm tra IP trong whitelist
        boolean allowed = stringList.contains(clientIp);

        log.debug("IP {} check result for user {}: {}", clientIp, ftpUser.getAccount(), allowed);
        return allowed;
    }

    /**
     * Kiểm tra connection type (FTP/FTPS) có được phép không
     */
    private boolean isConnectionTypeAllowed(FtpSession session, AccountFtpDto ftpUser) {
        boolean isSecure = session.isSecure();
        int requiredType = ftpUser.getTypeConnect(); // 1 = FTP only, 2 = FTPS only, 3 = Both

        log.info("Connection validation - User: {}, IsSecure: {}, RequiredType: {}",
                ftpUser.getAccount(), isSecure, requiredType);

        // Type 3 = cho phép cả FTP và FTPS
        if (requiredType == 3) {
            return true;
        }

        // Type 1 = chỉ cho phép FTP (không secure)
        // Type 2 = chỉ cho phép FTPS (secure)
        return (requiredType == 1 && !isSecure) || (requiredType == 2 && isSecure);
    }

    /**
     * Convert IP string to long
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Long.parseLong(octets[i]) << (24 - (8 * i)));
        }
        return result;
    }

    /**
     * Đóng session một cách an toàn
     */
    private void closeSession(FtpSession session) {
        try {
            if (session instanceof FtpIoSession ioSession) {
                ioSession.closeOnFlush();
            }
        } catch (Exception e) {
            log.error("Error closing session", e);
        }
    }
}