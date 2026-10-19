package com.greatprice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.greatprice.common.web.DbTimes;
import com.greatprice.dto.admin.JobRunSummary;
import com.greatprice.entity.JobRun;
import com.greatprice.mapper.DealMapper;
import com.greatprice.mapper.JobRunMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 把已到结束时间的已发布好价改为 EXPIRED。条件更新是幂等的，多实例重复执行不会把同一条记录过期两次。
 */
@Service
public class DealExpiryService {

    public static final String JOB_NAME = "DEAL_EXPIRE";
    static final int MAX_BATCHES = 500;

    private static final Logger log = LoggerFactory.getLogger(DealExpiryService.class);

    private final DealMapper dealMapper;
    private final JobRunMapper jobRunMapper;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;

    public DealExpiryService(DealMapper dealMapper,
                             JobRunMapper jobRunMapper,
                             AuditService auditService,
                             PlatformTransactionManager transactionManager,
                             @Value("${app.deal-expiry.batch-size:200}") int batchSize) {
        this.dealMapper = dealMapper;
        this.jobRunMapper = jobRunMapper;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = Math.min(Math.max(batchSize, 1), 1000);
    }

    public JobRunSummary runOnce(LocalDateTime now) {
        return runOnce(now, this::expireOneBatch);
    }

    /** 供测试注入会失败的批次，验证已成功的批次会保留，后续执行可以继续。 */
    JobRunSummary runOnce(LocalDateTime now, BatchExpire batch) {
        LocalDateTime started = DbTimes.now();
        long begin = System.nanoTime();
        int processed = 0;
        try {
            for (int i = 0; i < MAX_BATCHES; i++) {
                int updated = batch.expire(now, batchSize);
                if (updated == 0) {
                    break;
                }
                processed += updated;
            }
            JobRunSummary summary = save(started, JobRun.SUCCESS, processed, 0, null);
            if (processed > 0) {
                auditService.record(null, "system", AuditService.DEAL_EXPIRE, "DEAL", null, "自动过期 " + processed + " 条");
            }
            log.info("自动过期完成 processed={} failed={} elapsedMs={}", processed, 0, elapsed(begin));
            return summary;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            if (message.length() > 500) {
                message = message.substring(0, 500);
            }
            JobRunSummary summary = save(started, JobRun.FAILED, processed, 1, message);
            log.error("自动过期失败 processed={} elapsedMs={}", processed, elapsed(begin), ex);
            return summary;
        }
    }

    int expireAll(LocalDateTime now, int size) {
        int processed = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            int updated = expireOneBatch(now, size);
            if (updated == 0) {
                break;
            }
            processed += updated;
        }
        return processed;
    }

    int expireOneBatch(LocalDateTime now, int size) {
        Integer updated = transactionTemplate.execute(status -> {
            List<Long> ids = dealMapper.selectExpiredIds(now, size);
            if (ids.isEmpty()) {
                return 0;
            }
            return dealMapper.expireByIds(ids, now);
        });
        return updated == null ? 0 : updated;
    }

    public JobRunSummary latest() {
        JobRun run = jobRunMapper.selectOne(Wrappers.<JobRun>lambdaQuery()
                .eq(JobRun::getJobName, JOB_NAME)
                .orderByDesc(JobRun::getId)
                .last("LIMIT 1"));
        return run == null ? null : toSummary(run);
    }

    private JobRunSummary save(LocalDateTime started, String status, int processed, int failed, String message) {
        JobRun run = new JobRun();
        run.setJobName(JOB_NAME);
        run.setStatus(status);
        run.setProcessedCount(processed);
        run.setFailedCount(failed);
        run.setMessage(message);
        run.setStartedAt(started);
        run.setFinishedAt(DbTimes.now());
        jobRunMapper.insert(run);
        return toSummary(run);
    }

    private static JobRunSummary toSummary(JobRun run) {
        return new JobRunSummary(
                run.getJobName(),
                run.getStatus(),
                run.getProcessedCount() == null ? 0 : run.getProcessedCount(),
                run.getFailedCount() == null ? 0 : run.getFailedCount(),
                run.getMessage(),
                DbTimes.toOffset(run.getStartedAt()),
                DbTimes.toOffset(run.getFinishedAt()));
    }

    private static long elapsed(long begin) {
        return (System.nanoTime() - begin) / 1_000_000L;
    }

    @FunctionalInterface
    interface BatchExpire {
        int expire(LocalDateTime now, int batchSize);
    }
}
