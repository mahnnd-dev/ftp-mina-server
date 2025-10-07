package com.neo.ftpserver.permission;

import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class IpFilterFtplet extends DefaultFtplet {

    @Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request) throws FtpException, IOException {
        User user = session.getUser();
        String clientIp = session.getClientAddress().getAddress().getHostAddress();
        // Duyệt qua authorities để check IpRestrictionPermission
        for (Authority authority : user.getAuthorities()) {
            if (authority instanceof IpRestrictionPermission ipPerm) {
                if (!ipPerm.isIpAllowed(clientIp)) {
                    session.write(new DefaultFtpReply(530, "Connection not allowed from IP: " + clientIp));
                    if (session instanceof FtpIoSession ioSession) {
                        ioSession.closeNow(); // 🔹 Đóng kết nối ngay lập tức
                    }
                    return FtpletResult.DISCONNECT;
                }
            }
        }
        return FtpletResult.DEFAULT;
    }
}

