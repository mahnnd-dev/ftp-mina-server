package com.neo.ftpserver.cache;

import com.neo.ftpserver.entity.AccountFtp;
import com.neo.ftpserver.repository.AccountFtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountFtpCache {

    private final AccountFtpRepository repository;
    private final CacheManager cacheManager;

    @Cacheable("accountFtp")
    public AccountFtp getAccountFtpByAccount(String account) {
        return repository.findByAccount(account).orElse(null);
    }

    @Scheduled(fixedRate = 10000)
    public void refreshAccountFtpCache() {
        List<AccountFtp> accountFtps = repository.findAll();
        Cache cache = cacheManager.getCache("accountFtp");
        for (AccountFtp ftp : accountFtps) {
            cache.put(ftp.getAccount(), ftp);
        }
        String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        log.info("Đã cập nhật cache AccountFtp lúc {}, Cache size: {}", formattedDate, ((ConcurrentMap<?, ?>) cache.getNativeCache()).size());
    }
}
