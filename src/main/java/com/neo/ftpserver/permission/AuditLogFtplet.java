package com.neo.ftpserver.permission;

import com.neo.ftpserver.service.FtpAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogFtplet extends DefaultFtplet {

    private final FtpAuditService auditService;

    /**
     * Lấy đường dẫn file từ request
     */
    private String getFilePath(FtpSession session, FtpRequest request) {
        try {
            String argument = request.getArgument();
            if (argument == null || argument.isEmpty()) {
                return null;
            }

            // Nếu là đường dẫn tuyệt đối
            if (argument.startsWith("/")) {
                return argument;
            }

            // Nếu là đường dẫn tương đối
            FtpFile workingDir = session.getFileSystemView().getWorkingDirectory();
            String basePath = workingDir.getAbsolutePath();

            // Đảm bảo không có double slash
            if (basePath.endsWith("/")) {
                return basePath + argument;
            } else {
                return basePath + "/" + argument;
            }
        } catch (Exception e) {
            log.error("Error getting file path", e);
            return request.getArgument();
        }
    }

    /**
     * Lấy IP client
     */
    private String getClientIp(FtpSession session) {
        try {
            return session.getClientAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            log.error("Error getting client IP", e);
            return "unknown";
        }
    }

    /**
     * Kiểm tra kết nối có secure không
     */
    private boolean isSecure(FtpSession session) {
        try {
            // Kiểm tra control connection có secure không
            return session.isSecure();
        } catch (Exception e) {
            log.error("Error checking secure status", e);
            return false;
        }
    }

    /**
     * Lấy username an toàn
     */
    private String getUsername(FtpSession session) {
        try {
            User user = session.getUser();
            return user != null ? user.getName() : "anonymous";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[LOGIN] User={} IP={} Secure={}", username, ip, secure);
            auditService.logEvent(username, "LOGIN", null, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging login event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[LOGOUT] User={} IP={}", username, ip);
            auditService.logEvent(username, "LOGOUT", null, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging logout event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String filePath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[UPLOAD_START] User={} File={} IP={}", username, filePath, ip);
            auditService.logEvent(username, "UPLOAD_START", filePath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging upload start event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String filePath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            // Lấy kích thước file sau khi upload
            Long size = null;
            try {
                FtpFile file = session.getFileSystemView().getFile(request.getArgument());
                if (file != null && file.doesExist()) {
                    size = file.getSize();
                }
            } catch (Exception e) {
                log.warn("Cannot get file size for: {}", filePath, e);
            }

            log.info("[UPLOAD_END] User={} File={} Size={} bytes IP={}", username, filePath, size, ip);
            auditService.logEvent(username, "UPLOAD", filePath, size, ip, secure);
        } catch (Exception e) {
            log.error("Error logging upload end event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String filePath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[DOWNLOAD_START] User={} File={} IP={}", username, filePath, ip);
            auditService.logEvent(username, "DOWNLOAD_START", filePath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging download start event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onDownloadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String filePath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            // Lấy kích thước file
            Long size = null;
            try {
                FtpFile file = session.getFileSystemView().getFile(request.getArgument());
                if (file != null && file.doesExist()) {
                    size = file.getSize();
                }
            } catch (Exception e) {
                log.warn("Cannot get file size for: {}", filePath, e);
            }

            log.info("[DOWNLOAD_END] User={} File={} Size={} bytes IP={}", username, filePath, size, ip);
            auditService.logEvent(username, "DOWNLOAD", filePath, size, ip, secure);
        } catch (Exception e) {
            log.error("Error logging download end event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onDeleteStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String filePath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[DELETE_START] User={} File={} IP={}", username, filePath, ip);
            auditService.logEvent(username, "DELETE_START", filePath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging delete start event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String filePath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[DELETE_END] User={} File={} IP={}", username, filePath, ip);
            auditService.logEvent(username, "DELETE", filePath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging delete end event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onRmdirStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String dirPath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[RMDIR_START] User={} Dir={} IP={}", username, dirPath, ip);
            auditService.logEvent(username, "RMDIR_START", dirPath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging rmdir start event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String dirPath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[RMDIR_END] User={} Dir={} IP={}", username, dirPath, ip);
            auditService.logEvent(username, "RMDIR", dirPath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging rmdir end event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String dirPath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[MKDIR_START] User={} Dir={} IP={}", username, dirPath, ip);
            auditService.logEvent(username, "MKDIR_START", dirPath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging mkdir start event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String dirPath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[MKDIR_END] User={} Dir={} IP={}", username, dirPath, ip);
            auditService.logEvent(username, "MKDIR", dirPath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging mkdir end event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String fromPath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[RENAME_START] User={} From={} IP={}", username, fromPath, ip);
            auditService.logEvent(username, "RENAME_START", fromPath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging rename start event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        try {
            String username = getUsername(session);
            String toPath = getFilePath(session, request);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            log.info("[RENAME_END] User={} To={} IP={}", username, toPath, ip);
            auditService.logEvent(username, "RENAME", toPath, null, ip, secure);
        } catch (Exception e) {
            log.error("Error logging rename end event", e);
        }
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
        try {
            String cmd = request.getCommand().toUpperCase();
            String username = getUsername(session);
            String ip = getClientIp(session);
            boolean secure = isSecure(session);

            // Log các command đặc biệt khác (nếu cần)
            switch (cmd) {
                case "SITE":
                    String siteCmd = request.getArgument();
                    log.info("[SITE_CMD] User={} Command={} IP={}", username, siteCmd, ip);
                    auditService.logEvent(username, "SITE_" + siteCmd, null, null, ip, secure);
                    break;

                case "QUIT":
                    // QUIT được handle bởi onDisconnect
                    break;

                default:
                    // Có thể log thêm các command khác nếu cần
                    break;
            }
        } catch (Exception e) {
            log.error("Error in afterCommand", e);
        }
        return FtpletResult.DEFAULT;
    }
}