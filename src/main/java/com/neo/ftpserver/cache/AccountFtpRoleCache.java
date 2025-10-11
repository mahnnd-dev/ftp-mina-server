package com.neo.ftpserver.cache;

import com.neo.ftpserver.dto.AccountFtpRoleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AccountFtpRoleCache extends CacheSwapService<AccountFtpRoleDto> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    protected ConcurrentHashMap<String, AccountFtpRoleDto> fetchDataFromDB() {
        ConcurrentHashMap<String, AccountFtpRoleDto> map = new ConcurrentHashMap<>();
        List<AccountFtpRoleDto> partnerConfigList = findAll();
        for (AccountFtpRoleDto partnerConfig : partnerConfigList) {
            map.put(partnerConfig.getCode(), partnerConfig);
        }
        return map;
    }

    @Scheduled(fixedDelayString = "10000")
    public void forceRefresh() {
        super.forceRefresh();
    }

    public List<AccountFtpRoleDto> findAll() {
        String sql = "SELECT * FROM ACCOUNT_FTP_ROLE";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AccountFtpRoleDto.class));
    }
}
