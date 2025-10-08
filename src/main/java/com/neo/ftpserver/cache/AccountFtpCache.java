package com.neo.ftpserver.cache;

import com.neo.ftpserver.dto.AccountFtpDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AccountFtpCache extends CacheSwapService<AccountFtpDto> {
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected ConcurrentHashMap<String, AccountFtpDto> fetchDataFromDB() {
        ConcurrentHashMap<String, AccountFtpDto> map = new ConcurrentHashMap<>();
        List<AccountFtpDto> partnerConfigList = findAll();
        for (AccountFtpDto partnerConfig : partnerConfigList) {
            map.put(partnerConfig.getAccount(), partnerConfig);
        }
        return map;
    }

    @Scheduled(fixedDelayString = "10000")
    public void forceRefresh() {
        super.forceRefresh();
    }

    public List<AccountFtpDto> findAll() {
        String sql = "SELECT * FROM ACCOUNT_FTP";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AccountFtpDto.class));
    }
}
