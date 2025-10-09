package com.neo.ftpserver.permission;

import com.neo.ftpserver.constans.FtpCommandGroup;
import com.neo.ftpserver.service.FtpAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogFtplet extends DefaultFtplet {

    private final FtpAuditService auditService;

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

    private String getClientIp(FtpSession session) {
        try {
            return session.getClientAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            log.error("Error getting client IP", e);
            return "unknown";
        }
    }

    private boolean isSecure(FtpSession session) {
        try {
            // Kiểm tra control connection có secure không
            return session.isSecure();
        } catch (Exception e) {
            log.error("Error checking secure status", e);
            return false;
        }
    }

    private String getUsername(FtpSession session) {
        try {
            User user = session.getUser();
            return user != null ? user.getName() : "anonymous";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request) {
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
    public FtpletResult onDisconnect(FtpSession session) {
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
    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) {
        String cmd = request.getCommand().toUpperCase();
        String username = getUsername(session);
        String ip = getClientIp(session);
        boolean secure = isSecure(session);
        log.info("#afterCommand: {} from {}", cmd, ip);
        try {
            FtpCommandGroup group = classifyCommand(cmd);
            switch (group) {
                case FILE -> handleFileCommand(cmd, session, request, username, ip, secure);
                case DIRECTORY -> handleDirectoryCommand(cmd, session, request, username, ip, secure);
                case CONNECTION -> handleConnectionCommand(cmd, username, ip, secure);
                case SYSTEM -> handleSystemCommand(cmd, request, username, ip, secure);
                default -> log.debug("[UNKNOWN_CMD] {} {} IP={}", username, cmd, ip);
            }
        } catch (Exception e) {
            log.error("Error in afterCommand for cmd={}", cmd, e);
        }
        return FtpletResult.DEFAULT;
    }

    public FtpCommandGroup classifyCommand(String cmd) {
        return switch (cmd.toUpperCase()) {
            case "STOR", "APPE", "RETR", "DELE", "RNFR", "RNTO", "SIZE", "MDTM" -> FtpCommandGroup.FILE;
            case "MKD", "RMD", "CWD", "PWD", "LIST", "NLST" -> FtpCommandGroup.DIRECTORY;
//            case "USER", "PASS", "QUIT", "PORT", "PASV", "AUTH", "PBSZ", "PROT" -> FtpCommandGroup.CONNECTION;
            case "SITE", "STAT", "FEAT", "OPTS" -> FtpCommandGroup.SYSTEM;
            default -> FtpCommandGroup.UNKNOWN;
        };
    }

    public void handleFileCommand(String cmd, FtpSession session, FtpRequest request, String username, String ip, boolean secure) {
        String filePath = getFilePath(session, request);
        Long size = (cmd.equals("STOR") || cmd.equals("APPE")) ? getFileSize(session, request) : null;
        log.info("[FILE] {} {} Size={} IP={}", username, filePath, size, ip);
        auditService.logEvent(username, cmd, filePath, size, ip, secure);
    }

    public void handleDirectoryCommand(String cmd, FtpSession session, FtpRequest request, String username, String ip, boolean secure) {
        String dirPath = getFilePath(session, request); // dùng chung với filePath
        log.info("[DIR] {} {} IP={}", username, dirPath, ip);
        auditService.logEvent(username, cmd, dirPath, null, ip, secure);
    }

    public void handleConnectionCommand(String cmd, String username, String ip, boolean secure) {
        if (!cmd.equals("QUIT")) { // QUIT đã xử lý trong onDisconnect
            log.info("[CONNECTION] {} {} IP={} Secure={}", username, cmd, ip, secure);
            auditService.logEvent(username, cmd, null, null, ip, secure);
        }
    }

    public void handleSystemCommand(String cmd, FtpRequest request, String username, String ip, boolean secure) {
        String arg = request.getArgument();
        log.info("[SYSTEM] {} {} {} IP={}", username, cmd, arg, ip);
        auditService.logEvent(username, cmd + "_" + arg, null, null, ip, secure);
    }

    private Long getFileSize(FtpSession session, FtpRequest request) {
        String argument = request.getArgument();

        try {
            log.debug("=== Getting file size for argument: {} ===", argument);

            // Cách 1: Lấy từ FtpFile API
            FtpFile file = session.getFileSystemView().getFile(argument);
            log.debug("FtpFile obtained: {}", file != null ? "yes" : "no");

            if (file != null) {
                log.debug("File exists: {}", file.doesExist());
                log.debug("File absolute path: {}", file.getAbsolutePath());
                log.debug("File is file: {}", file.isFile());
                log.debug("File is readable: {}", file.isReadable());

                if (file.doesExist()) {
                    long size = file.getSize();
                    log.debug("File size from FtpFile API: {} bytes", size);
                    if (size > 0) {
                        log.debug("Got size from FtpFile API: {} bytes", size);
                        return size;
                    }
                }

                // Cách 2: Lấy từ physical path (nếu FtpFile không có size)
                String physicalPath = file.getAbsolutePath();
                log.debug("Trying physical path: {}", physicalPath);

                if (physicalPath != null) {
                    Path path = Paths.get(physicalPath);
                    log.debug("Physical path exists: {}", Files.exists(path));
                    log.debug("Physical path is file: {}", Files.isRegularFile(path));

                    if (Files.exists(path)) {
                        long size = Files.size(path);
                        log.debug("Got size from physical file: {} bytes", size);
                        return size;
                    }
                }
            }

            // Cách 3: Thử với working directory + argument
            FtpFile workingDir = session.getFileSystemView().getWorkingDirectory();
            String workingPath = workingDir.getAbsolutePath();
            log.debug("Working directory: {}", workingPath);

            String fullPath = workingPath.endsWith("/")
                    ? workingPath + argument
                    : workingPath + "/" + argument;
            log.debug("Trying full path: {}", fullPath);

            Path path = Paths.get(fullPath);
            if (Files.exists(path)) {
                long size = Files.size(path);
                log.debug("Got size from full path: {} bytes", size);
                return size;
            }

            // Cách 4: Thử với delay (file có thể chưa flush hoặc đang bị modify bởi MFMT)
            log.debug("Waiting 200ms for file to be flushed...");
            Thread.sleep(200);

            file = session.getFileSystemView().getFile(argument);
            if (file != null && file.doesExist()) {
                long size = file.getSize();
                if (size > 0) {
                    log.debug("Got size after retry: {} bytes", size);
                    return size;
                }

                // Thử lại với physical path
                String physicalPath = file.getAbsolutePath();
                if (physicalPath != null) {
                    Path retryPath = Paths.get(physicalPath);
                    if (Files.exists(retryPath)) {
                        size = Files.size(retryPath);
                        log.debug("Got size from physical file after retry: {} bytes", size);
                        return size;
                    }
                }
            }

            log.warn("Cannot get file size for: {} after all attempts", argument);
            return null;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while getting file size", ie);
            return null;
        } catch (Exception e) {
            log.error("Error getting file size for: {}", argument, e);
            return null;
        }
    }
}