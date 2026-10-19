package com.greatprice.service;

import com.greatprice.dto.admin.JobRunSummary;
import com.greatprice.mapper.DealMapper;
import com.greatprice.mapper.JobRunMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DealExpiryLoopTest {

    @Test
    void failedBatchKeepsCompletedCountAndLaterRunContinues() {
        DealExpiryService service = new DealExpiryService(
                mock(DealMapper.class),
                mock(JobRunMapper.class),
                mock(AuditService.class),
                mock(PlatformTransactionManager.class),
                2);
        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 12, 0);
        AtomicInteger calls = new AtomicInteger();
        JobRunSummary failed = service.runOnce(now, (time, size) -> {
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("batch failed");
            }
            return 2;
        });
        assertEquals("FAILED", failed.status());
        assertEquals(2, failed.processedCount());
        assertEquals(1, failed.failedCount());

        JobRunSummary resumed = service.runOnce(now, (time, size) -> 0);
        assertEquals("SUCCESS", resumed.status());
        assertEquals(0, resumed.processedCount());
    }

    @Test
    void successAuditsOnlyWhenRowsChanged() {
        AuditService auditService = mock(AuditService.class);
        DealExpiryService service = new DealExpiryService(
                mock(DealMapper.class),
                mock(JobRunMapper.class),
                auditService,
                mock(PlatformTransactionManager.class),
                2);
        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 12, 0);
        service.runOnce(now, (time, size) -> 0);
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());

        AtomicInteger calls = new AtomicInteger();
        service.runOnce(now, (time, size) -> calls.getAndIncrement() == 0 ? 2 : 0);
        verify(auditService).record(isNull(), eq("system"), eq(AuditService.DEAL_EXPIRE), eq("DEAL"), isNull(), eq("自动过期 2 条"));
    }
}
