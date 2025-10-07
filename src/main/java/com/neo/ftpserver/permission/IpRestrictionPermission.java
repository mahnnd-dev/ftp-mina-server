package com.neo.ftpserver.permission;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;

import java.util.Arrays;
import java.util.List;

public class IpRestrictionPermission implements Authority {

    private final List<String> allowedIps;

    public IpRestrictionPermission(String ipList) {
        if (ipList == null || ipList.isBlank()) {
            this.allowedIps = List.of(); // không cấu hình nghĩa là không giới hạn
        } else {
            this.allowedIps = Arrays.stream(ipList.split(","))
                    .map(String::trim)
                    .toList();
        }
    }

    public boolean isIpAllowed(String clientIp) {
        if (allowedIps.isEmpty()) {
            return true; // không khai báo -> cho phép tất cả
        }
        return allowedIps.contains(clientIp);
    }

    @Override
    public AuthorizationRequest authorize(AuthorizationRequest request) {
        // Không check theo kiểu request mà check trực tiếp khi login
        return request;
    }

    @Override
    public boolean canAuthorize(AuthorizationRequest request) {
        return false;
    }
}
