package com.neo.ftpserver.ftp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FtpServerService {
    private FtpServer ftpServer;
    private final FtpServerFactory serverFactory;
    @Value("${ftp.server.ftp-port:2121}")
    private int ftpPort;
    @Value("${ftp.server.ftps-port:990}")
    private int ftpsPort;


    @PostConstruct
    public void initFtpServer() {
        try {
            log.info("🔧 Initializing FTP/FTPS server on port {}/{}", ftpPort,ftpsPort);
            ftpServer = serverFactory.createServer();
            ftpServer.start();
            log.info("✅ FTP/FTPS server started successfully on port {}/{}", ftpPort,ftpsPort);
        } catch (Exception e) {
            log.error("❌ Failed to start FTP/FTPS server", e);
        }
    }

    @PreDestroy
    public void stopFtpServer() {
        try {
            if (ftpServer != null && !ftpServer.isStopped()) {
                ftpServer.stop();
                log.info("🛑 FTP/FTPS server stopped successfully");
            }
        } catch (Exception e) {
            log.error("⚠️ Error while stopping FTP/FTPS server", e);
        }
    }
}
