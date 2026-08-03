package com.coursistant.lms.module.auth.identity.service;

import com.coursistant.lms.module.auth.identity.entity.AccountIdentity;
import com.coursistant.lms.module.auth.identity.repository.AccountIdentityMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountIdentityService {

    public static final String PRINCIPAL_ADMIN = "ADMIN";
    public static final String PRINCIPAL_USER = "USER";

    @Resource
    private AccountIdentityMapper accountIdentityMapper;

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    @Transactional
    public void claimEmail(String email, String principalType, Integer principalId) {
        String normalized = normalizeEmail(email);
        if (normalized == null || normalized.isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "email is required");
        }
        AccountIdentity existing = accountIdentityMapper.selectByEmail(normalized);
        if (existing != null) {
            boolean same = principalType.equals(existing.getPrincipalType())
                    && principalId.equals(existing.getPrincipalId());
            if (!same) {
                throw new ApiException(ErrorType.CONFLICT, "Email already in use");
            }
            return;
        }
        AccountIdentity row = new AccountIdentity();
        row.setNormalizedEmail(normalized);
        row.setPrincipalType(principalType);
        row.setPrincipalId(principalId);
        accountIdentityMapper.insert(row);
    }
}
