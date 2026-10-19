package com.greatprice.integration.taobao;

public final class SleepingTaobaoRetryDelay implements TaobaoRetryDelay {

    @Override
    public void pause(int attempt, boolean rateLimited) {
        long millis = (rateLimited ? 500L : 200L) * Math.max(attempt, 1);
        try {
            Thread.sleep(Math.min(millis, 2000L));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
