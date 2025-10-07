package com.neo.ftpserver.service;

import com.neo.ftpserver.cache.AccountFtpCache;
import com.neo.ftpserver.dto.AccountFtpCreateDto;
import com.neo.ftpserver.dto.AccountFtpResponseDto;
import com.neo.ftpserver.dto.AccountFtpUpdateDto;
import com.neo.ftpserver.entity.AccountFtp;
import com.neo.ftpserver.mapper.AccountFtpMapper;
import com.neo.ftpserver.repository.AccountFtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountFtpService {

    private final AccountFtpRepository userRepository;
    private final AccountFtpMapper mapper;
    private final AccountFtpCache ftpCache;


    public AccountFtpResponseDto createUser(AccountFtpCreateDto dto) {
        if (userRepository.existsByAccount(dto.getAccount())) {
            throw new RuntimeException("Username already exists: " + dto.getAccount());
        }
        AccountFtp user = mapper.toEntity(dto);
        // Create home directory if it doesn't exist
        createFolder(dto.getFolderAccess());
        AccountFtp savedUser = userRepository.save(user);
        return mapToResponseDto(savedUser);
    }

    public AccountFtpResponseDto updateUser(Long id, AccountFtpUpdateDto dto) {
        AccountFtp user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        mapper.updateEntity(user, dto); // cập nhật từng trường
        createFolder(dto.getFolderAccess());
        AccountFtp updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public AccountFtpResponseDto getUserById(Long id) {
        AccountFtp user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToResponseDto(user);
    }

    public AccountFtpResponseDto getUserByUsername(String username) {
        AccountFtp user = userRepository.findByAccount(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return mapToResponseDto(user);
    }

    public List<AccountFtpResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private AccountFtpResponseDto mapToResponseDto(AccountFtp user) {
        return mapper.mapToResponseDto(user);
    }

    public void createFolder(String path) {
        try {
            Files.createDirectories(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục: " + path, e);
        }
    }
}
