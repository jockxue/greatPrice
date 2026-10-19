package com.greatprice.integration.taobao;

public interface TaobaoRetryDelay {

    void pause(int attempt, boolean rateLimited);
}
