package com.neo.ftpserver.config;

import com.neo.ftpserver.ftp.CustomUserManager;
import com.neo.ftpserver.permission.AuditLogFtplet;
import com.neo.ftpserver.permission.UnifiedFtplet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FtpServerConfig {

    @Value("${ftp.server.ftp-port:2121}")
    private int ftpPort;
    @Value("${ftp.server.ftps-port:990}")
    private int ftpsPort;

    @Value("${ftp.server.passive-ports:30000-30100}")
    private String passivePorts;

    @Value("${ftp.server.passive-external-address:}")
    private String passiveExternalAddress;

    @Value("${ftp.ssl.keystore.path}")
    private String keystorePath;

    @Value("${ftp.ssl.keystore.password}")
    private String keystorePassword;

    private final AuditLogFtplet auditLogFtplet;
    private final UnifiedFtplet unifiedFtplet;
    private final CustomUserManager userManager;


    @Bean
    public FtpServerFactory ftpServerFactory() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.setUserManager(userManager);
        // Listener 1: FTP thường
        ListenerFactory ftpListenerFactory = new ListenerFactory();
        ftpListenerFactory.setPort(ftpPort);
        serverFactory.addListener("default", ftpListenerFactory.createListener());
        // Listener 2: FTPS (explicit)
        ListenerFactory ftpsListenerFactory = new ListenerFactory();
        ftpsListenerFactory.setPort(ftpsPort);
        // SSL/TLS
        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile(new File(keystorePath));
        ssl.setKeystorePassword(keystorePassword);
        // Listener (explicit FTPS)
        ftpsListenerFactory.setSslConfiguration(ssl.createSslConfiguration());
        ftpsListenerFactory.setImplicitSsl(false);
        serverFactory.addListener("ftps", ftpsListenerFactory.createListener());
        // Passive mode config
        DataConnectionConfigurationFactory dataConnFactory = new DataConnectionConfigurationFactory();
        dataConnFactory.setPassivePorts(passivePorts);
        if (!passiveExternalAddress.isEmpty()) {
            dataConnFactory.setPassiveExternalAddress(passiveExternalAddress);
        }
//        listenerFactory.setDataConnectionConfiguration(dataConnFactory.createDataConnectionConfiguration());
//        serverFactory.addListener("default", listenerFactory.createListener());
        // Permission ip
        Map<String, Ftplet> map = new LinkedHashMap<>();
        map.put("auditLogFtplet", auditLogFtplet); // Đặt đầu tiên
        map.put("unifiedFtplet", unifiedFtplet);
        serverFactory.setFtplets(map);

        return serverFactory;
    }
}
