package com.bm.bmrpc.fault.retry;

import com.bm.bmrpc.model.RpcResponse;
import com.github.rholder.retry.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 指数退避 - 重试策略
 * <p>
 * 每次重试的等待时间按指数增长：wait = multiplier * 2^(attempt-1)，
 * 例如 multiplier=1s 时：第1次重试等待 1s，第2次 2s，第3次 4s...
 * 相比固定间隔，指数退避能在服务端短暂过载时给其更多恢复时间，避免重试风暴。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习星球</a>
 */
@Slf4j
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

    /**
     * 指数退避乘数（单位：毫秒）
     * <p>
     * 实际等待时间 = multiplier * 2^(attempt-1)
     * 第1次重试：1000ms = 1s
     * 第2次重试：2000ms = 2s
     * 第3次重试：4000ms = 4s
     */
    private static final long MULTIPLIER = 1000L;

    /**
     * 最大等待时间（单位：毫秒），避免单次等待过长
     */
    private static final long MAX_WAIT = 10000L;

    /**
     * 最大重试次数
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 重试
     *
     * @param callable
     * @return
     * @throws ExecutionException
     * @throws RetryException
     */
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws ExecutionException, RetryException {
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfExceptionOfType(Exception.class)
                .withWaitStrategy(WaitStrategies.exponentialWait(MULTIPLIER, MAX_WAIT, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(MAX_ATTEMPTS))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        log.info("重试次数 {}", attempt.getAttemptNumber());
                    }
                })
                .build();
        return retryer.call(callable);
    }

}
