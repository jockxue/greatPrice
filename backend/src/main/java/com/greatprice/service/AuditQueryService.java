package com.greatprice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greatprice.common.api.PageResult;
import com.greatprice.common.web.DbTimes;
import com.greatprice.dto.admin.AuditLogResponse;
import com.greatprice.dto.admin.AuditQuery;
import com.greatprice.entity.AuditLog;
import com.greatprice.mapper.AuditLogMapper;
import com.greatprice.security.AdminActors;
import org.springframework.stereotype.Service;

@Service
public class AuditQueryService {

    private final AuditLogMapper auditLogMapper;
    private final AdminActors adminActors;

    public AuditQueryService(AuditLogMapper auditLogMapper, AdminActors adminActors) {
        this.auditLogMapper = auditLogMapper;
        this.adminActors = adminActors;
    }

    public PageResult<AuditLogResponse> page(AuditQuery query) {
        adminActors.requireAdmin();
        IPage<AuditLog> page = auditLogMapper.selectPage(Page.of(query.page(), query.size()), Wrappers.<AuditLog>lambdaQuery()
                .eq(query.action() != null, AuditLog::getAction, query.action())
                .eq(query.resourceType() != null, AuditLog::getResourceType, query.resourceType())
                .orderByDesc(AuditLog::getId));
        return PageResult.of(page, log -> new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorName(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetail(),
                DbTimes.toOffset(log.getCreatedAt())));
    }
}
