package com.neo.ftpserver.config;

import com.neo.ftpserver.ftp.CustomUserManager;
import com.neo.ftpserver.permission.IpFilterFtplet;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FtpServerConfig {

    @Value("${ftp.server.port:2121}")
    private int ftpPort;

    @Value("${ftp.server.passive-ports:30000-30100}")
    private String passivePorts;

    @Value("${ftp.server.passive-external-address:}")
    private String passiveExternalAddress;

    @Value("${ftp.ssl.keystore.path}")
    private String keystorePath;

    @Value("${ftp.ssl.keystore.password}")
    private String keystorePassword;

    private final IpFilterFtplet ipFilterFtplet;

    private final CustomUserManager userManager;

    public FtpServerConfig(IpFilterFtplet ipFilterFtplet, CustomUserManager userManager) {
        this.ipFilterFtplet = ipFilterFtplet;
        this.userManager = userManager;
    }

    @Bean
    public FtpServerFactory ftpServerFactory() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.setUserManager(userManager);

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(ftpPort);

        // SSL/TLS
//        SslConfigurationFactory ssl = new SslConfigurationFactory();
//        ssl.setKeystoreFile(new File(keystorePath));
//        ssl.setKeystorePassword(keystorePassword);
        // Passive mode config
        DataConnectionConfigurationFactory dataConnFactory = new DataConnectionConfigurationFactory();
        dataConnFactory.setPassivePorts(passivePorts);
        if (!passiveExternalAddress.isEmpty()) {
            dataConnFactory.setPassiveExternalAddress(passiveExternalAddress);
        }

        listenerFactory.setDataConnectionConfiguration(dataConnFactory.createDataConnectionConfiguration());
        serverFactory.addListener("default", listenerFactory.createListener());

        // Permission ip
        Map<String, Ftplet> ftplets = new HashMap<>();
        ftplets.put("ipFilterFtplet", ipFilterFtplet);
        serverFactory.setFtplets(ftplets);

        return serverFactory;
    }
}
