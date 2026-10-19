package com.greatprice.service;

import com.greatprice.entity.AuditLog;
import com.greatprice.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String LOGOUT = "LOGOUT";
    public static final String PRODUCT_CREATE = "PRODUCT_CREATE";
    public static final String PRODUCT_UPDATE = "PRODUCT_UPDATE";
    public static final String PRODUCT_ENABLE = "PRODUCT_ENABLE";
    public static final String PRODUCT_DISABLE = "PRODUCT_DISABLE";
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";
    public static final String DEAL_CREATE = "DEAL_CREATE";
    public static final String DEAL_UPDATE = "DEAL_UPDATE";
    public static final String DEAL_SUBMIT = "DEAL_SUBMIT";
    public static final String DEAL_APPROVE = "DEAL_APPROVE";
    public static final String DEAL_REJECT = "DEAL_REJECT";
    public static final String DEAL_OFFLINE = "DEAL_OFFLINE";
    public static final String DEAL_REVISE = "DEAL_REVISE";
    public static final String DEAL_REOPEN = "DEAL_REOPEN";
    public static final String PRICE_VERIFY = "PRICE_VERIFY";
    public static final String DEAL_EXPIRE = "DEAL_EXPIRE";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_DISABLE = "USER_DISABLE";
    public static final String USER_ENABLE = "USER_ENABLE";
    public static final String TAOBAO_IMPORT = "TAOBAO_IMPORT";
    public static final String TAOBAO_REFRESH = "TAOBAO_REFRESH";
    public static final String TAOBAO_PROMOTION = "TAOBAO_PROMOTION";

    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(Long actorId, String actorName, String action, String resourceType, Long resourceId, String detail) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorName(actorName == null || actorName.isBlank() ? "unknown" : actorName.strip());
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        if (detail != null && detail.length() > 1000) {
            detail = detail.substring(0, 1000);
        }
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }
}
