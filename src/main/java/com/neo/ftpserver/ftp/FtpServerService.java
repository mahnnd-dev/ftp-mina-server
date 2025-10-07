package com.neo.ftpserver.ftp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FtpServerService {
    private final FtpServerFactory ftpServerFactory;
    private FtpServer ftpServer;
    @Value("${ftp.server.port:2121}")
    private int ftpPort;

    public synchronized void start() throws Exception {
        try {
            if (ftpServer == null || ftpServer.isStopped()) {
                ftpServer = ftpServerFactory.createServer();
                ftpServer.start();
                log.info("#FTP Server started successfully on port " + ftpPort);
            }
        } catch (FtpException e) {
            log.error("Failed to start FTP Server", e);
            throw new RuntimeException("Failed to start FTP Server: " + e.getMessage(), e);
        }

    }

    public synchronized void stop() {
        if (ftpServer != null && !ftpServer.isStopped()) {
            try {
                ftpServer.stop();
                log.info("#FTP Server stopped successfully");
            } catch (Exception e) {
                log.error("Error stopping FTP Server", e);
            }
        }
    }

    public boolean isRunning() {
        return ftpServer != null && !ftpServer.isStopped();
    }

    public boolean isSuspended() {
        return ftpServer != null && ftpServer.isSuspended();
    }
}
