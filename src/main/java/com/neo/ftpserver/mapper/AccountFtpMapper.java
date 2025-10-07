package com.neo.ftpserver.mapper;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.ftpserver.dto.AccountFtpCreateDto;
import com.neo.ftpserver.dto.AccountFtpResponseDto;
import com.neo.ftpserver.dto.AccountFtpUpdateDto;
import com.neo.ftpserver.entity.AccountFtp;
import org.springframework.stereotype.Service;

@Service
public class AccountFtpMapper {

    private final ObjectMapper objectMapper;

    public AccountFtpMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AccountFtpCreateDto toCreateDto(AccountFtp entity) {
        return objectMapper.convertValue(entity, AccountFtpCreateDto.class);
    }

    public AccountFtpUpdateDto toUpdateDto(AccountFtp entity) {
        return objectMapper.convertValue(entity, AccountFtpUpdateDto.class);
    }

    public AccountFtp toEntity(AccountFtpCreateDto dto) {
        return objectMapper.convertValue(dto, AccountFtp.class);
    }

    public AccountFtp toEntity(AccountFtpUpdateDto dto) {
        return objectMapper.convertValue(dto, AccountFtp.class);
    }

    public AccountFtpResponseDto mapToResponseDto(AccountFtp entity) {
        return objectMapper.convertValue(entity, AccountFtpResponseDto.class);
    }

    public void updateEntity(AccountFtp entity, AccountFtpUpdateDto dto) {
        try {
            objectMapper.updateValue(entity, dto);
        } catch (JsonMappingException e) {
            e.getMessage();
        }
    }
}

