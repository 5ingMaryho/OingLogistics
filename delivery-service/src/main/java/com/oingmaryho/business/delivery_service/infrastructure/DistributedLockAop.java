package com.oingmaryho.business.delivery_service.infrastructure;

import com.oingmaryho.business.delivery_service.exception.ErrorCode;
import com.oingmaryho.business.delivery_service.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        RLock rLock = redissonClient.getLock(key);

        try {
            log.info("lock 시도: key = {}", key);
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!available) {
                log.warn("lock 획득 실패: key = {}", key);
                throw new LockException(ErrorCode.LOCK_FAILED);
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (rLock.isHeldByCurrentThread()) {
                        try {
                            rLock.unlock();
                            log.info("afterCommit 이후 락 해제 완료: key = {}", key);
                        } catch (IllegalMonitorStateException e) {
                            log.info("Redisson Lock Already UnLock: key = {}", key);
                        }
                    }
                }
            });

            return aopForTransaction.proceed(joinPoint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 현재 스레드 인터럽트 요청 상태 복원
            throw new InterruptedException();
        } catch (Throwable e) {
            // 예외 발생 시에도 롤백 후 락 해제 보장
            if (rLock.isHeldByCurrentThread()) {
                try {
                    rLock.unlock();
                    log.info("예외 발생으로 즉시 락 해제: key = {}", key);
                } catch (Exception ex) {
                    log.error("락 해제 실패", ex);
                    throw new LockException(ErrorCode.UNLOCK_FAILED);
                }
            }
            throw e;
        }
    }

}
