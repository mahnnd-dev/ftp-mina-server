package com.neo.ftpserver.cache;

import com.neo.ftpserver.dto.AccountFtp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AccountFtpCache extends CacheSwapService<AccountFtp> {
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected ConcurrentHashMap<String, AccountFtp> fetchDataFromDB() {
        ConcurrentHashMap<String, AccountFtp> map = new ConcurrentHashMap<>();
        List<AccountFtp> partnerConfigList = findAll();
        for (AccountFtp partnerConfig : partnerConfigList) {
            map.put(partnerConfig.getAccount(), partnerConfig);
        }
        return map;
    }

    @Scheduled(fixedDelayString = "10000")
    public void forceRefresh() {
        super.forceRefresh();
    }

    public List<AccountFtp> findAll() {
        String sql = "SELECT * FROM ACCOUNT_FTP";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AccountFtp.class));
    }
}
