# Redis锁场景选型及实例

## 一、锁类型概览

| 锁类型 | Bean名称 | 特点 | 适用场景 |
|-------|---------|------|---------|
| 单机锁 | redisSingleLock | 简单高效，仅单机有效 | 单机应用，测试环境 |
| 分布式锁 | redisDistributedLock | 跨JVM互斥，高性能 | 分布式系统资源互斥 |
| 公平锁 | redisFairLock | 先到先得，保证公平性 | 需要严格顺序的场景 |
| 可重入锁 | redisReentrantLock | 支持同一线程多次获取 | 递归调用，嵌套事务 |
| 读写锁 | redisReadWriteLock | 读共享写独占 | 读多写少的场景 |
| 多重锁 | redisMultiLock | 同时获取多个锁 | 需要锁定多个资源 |

## 二、使用方式

### 1. 注解方式（推荐）

```java
@Service
public class OrderService {
    
    // 使用默认分布式锁
    @CacheLock(key = "#orderId", expire = 30)
    public void processOrder(String orderId) {
        // 业务逻辑
    }
    
    // 指定锁类型
    @CacheLock(key = "#userId", expire = 60, lockType = "redisFairLock")
    public void updateUserBalance(String userId, BigDecimal amount) {
        // 需要公平性的业务逻辑
    }
    
    // 使用SpEL表达式
    @CacheLock(
        prefix = "inventory",
        key = "#sku.id + ':' + #sku.warehouse",
        expire = 10,
        waitTime = 5,
        message = "库存操作中，请稍后重试"
    )
    public void updateInventory(SkuInfo sku, int quantity) {
        // 库存更新逻辑
    }
}
```

### 2. 编程方式

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    @Autowired
    @Qualifier("redisDistributedLock")
    private RedisLock distributedLock;
    
    @Autowired
    @Qualifier("redisReadWriteLock")
    private RedisLock readWriteLock;
    
    public void processPayment(String paymentId) {
        String lockKey = "payment:" + paymentId;
        String requestId = UUID.randomUUID().toString();
        
        try {
            // 获取锁，最多等待5秒
            if (distributedLock.tryLock(lockKey, requestId, 5, 30, TimeUnit.SECONDS)) {
                // 处理支付逻辑
                doPayment(paymentId);
            } else {
                throw new BusinessException("支付处理中，请稍后重试");
            }
        } finally {
            // 释放锁
            distributedLock.unlock(lockKey, requestId);
        }
    }
}
```

## 三、场景选型指南

### 1. 分布式锁（RedisDistributedLock）

**适用场景：**
- 分布式环境下的资源互斥访问
- 防止重复提交（订单、支付等）
- 定时任务的分布式协调
- 缓存更新的并发控制

**示例：防止订单重复提交**
```java
@RestController
@RequestMapping("/order")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/create")
    @CacheLock(
        prefix = "order:create",
        key = "#request.userId + ':' + #request.productId",
        expire = 10,
        waitTime = 0,  // 不等待，快速失败
        message = "订单创建中，请勿重复提交"
    )
    public Result<OrderInfo> createOrder(@RequestBody OrderRequest request) {
        return Result.success(orderService.createOrder(request));
    }
}
```

### 2. 公平锁（RedisFairLock）

**适用场景：**
- 秒杀抢购（保证先到先得）
- 排队系统
- 资源分配需要公平性的场景

**示例：秒杀系统**
```java
@Service
public class SeckillService {
    
    @Autowired
    @Qualifier("redisFairLock")
    private RedisLock fairLock;
    
    public SeckillResult doSeckill(String userId, String skuId) {
        String lockKey = "seckill:" + skuId;
        String requestId = userId; // 使用用户ID作为请求标识
        
        try {
            // 公平锁保证先到先得
            if (fairLock.tryLock(lockKey, requestId, 10, TimeUnit.SECONDS)) {
                // 检查库存
                int stock = getStock(skuId);
                if (stock > 0) {
                    // 扣减库存，创建订单
                    decreaseStock(skuId);
                    return SeckillResult.success(createSeckillOrder(userId, skuId));
                } else {
                    return SeckillResult.fail("商品已售罄");
                }
            } else {
                return SeckillResult.fail("排队超时，请重试");
            }
        } finally {
            fairLock.unlock(lockKey, requestId);
        }
    }
}
```

### 3. 可重入锁（RedisReentrantLock）

**适用场景：**
- 递归方法调用
- 嵌套事务处理
- 复杂业务流程需要多次获取同一把锁

**示例：嵌套业务处理**
```java
@Service
@Transactional
public class AccountService {
    
    @Autowired
    @Qualifier("redisReentrantLock")
    private RedisLock reentrantLock;
    
    public void transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        String requestId = Thread.currentThread().getName();
        
        // 锁定转出账户
        lockAccount(fromAccount, requestId, () -> {
            // 检查余额
            checkBalance(fromAccount, amount);
            
            // 锁定转入账户（嵌套锁）
            lockAccount(toAccount, requestId, () -> {
                // 执行转账
                debitAccount(fromAccount, amount);
                creditAccount(toAccount, amount);
            });
        });
    }
    
    private void lockAccount(String accountId, String requestId, Runnable action) {
        String lockKey = "account:" + accountId;
        try {
            reentrantLock.lock(lockKey, requestId, 30, TimeUnit.SECONDS);
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("账户锁定失败");
        } finally {
            reentrantLock.unlock(lockKey, requestId);
        }
    }
}
```

### 4. 读写锁（RedisReadWriteLock）

**适用场景：**
- 缓存预热和更新
- 配置信息的读写
- 统计数据的查询和更新
- 任何读多写少的场景

**示例：配置管理系统**
```java
@Service
public class ConfigService {
    
    @Autowired
    @Qualifier("redisReadWriteLock")
    private RedisReadWriteLock readWriteLock;
    
    private final Map<String, ConfigInfo> configCache = new ConcurrentHashMap<>();
    
    // 读配置（可并发）
    public ConfigInfo getConfig(String configKey) {
        String lockKey = "config:lock:" + configKey;
        String requestId = UUID.randomUUID().toString();
        
        try {
            // 获取读锁
            readWriteLock.lockRead(lockKey, requestId, 10, TimeUnit.SECONDS);
            
            ConfigInfo config = configCache.get(configKey);
            if (config == null) {
                // 从数据库加载
                config = loadFromDatabase(configKey);
                configCache.put(configKey, config);
            }
            return config;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取配置失败");
        } finally {
            readWriteLock.unlock(lockKey, requestId);
        }
    }
    
    // 更新配置（独占）
    @CacheLock(prefix = "config:write", key = "#configKey", lockType = "redisReadWriteLock")
    public void updateConfig(String configKey, ConfigInfo newConfig) {
        String lockKey = "config:lock:" + configKey;
        String requestId = UUID.randomUUID().toString();
        
        try {
            // 获取写锁
            readWriteLock.lockWrite(lockKey, requestId, 30, TimeUnit.SECONDS);
            
            // 更新数据库
            saveToDatabase(configKey, newConfig);
            
            // 更新缓存
            configCache.put(configKey, newConfig);
            
            // 发布配置变更事件
            publishConfigChangeEvent(configKey, newConfig);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("更新配置失败");
        } finally {
            readWriteLock.unlock(lockKey, requestId);
        }
    }
}
```

### 5. 多重锁（RedisMultiLock）

**适用场景：**
- 需要同时锁定多个资源
- 跨表事务操作
- 防止死锁的多资源操作

**示例：跨店铺库存调拨**
```java
@Service
public class InventoryTransferService {
    
    @Autowired
    @Qualifier("redisMultiLock")
    private RedisMultiLock multiLock;
    
    public void transferInventory(String fromShop, String toShop, String skuId, int quantity) {
        // 需要同时锁定两个店铺的库存
        List<String> lockKeys = Arrays.asList(
            "inventory:" + fromShop + ":" + skuId,
            "inventory:" + toShop + ":" + skuId
        );
        
        String requestId = UUID.randomUUID().toString();
        
        try {
            // 同时获取多个锁，内部会排序防止死锁
            multiLock.lockMulti(lockKeys, requestId, 30, TimeUnit.SECONDS);
            
            // 检查源店铺库存
            int fromStock = getInventory(fromShop, skuId);
            if (fromStock < quantity) {
                throw new BusinessException("源店铺库存不足");
            }
            
            // 执行调拨
            decreaseInventory(fromShop, skuId, quantity);
            increaseInventory(toShop, skuId, quantity);
            
            // 记录调拨日志
            logTransfer(fromShop, toShop, skuId, quantity);
            
        } finally {
            // 释放所有锁
            multiLock.unlockMulti(requestId);
        }
    }
}
```

## 四、最佳实践

### 1. 锁的粒度控制

```java
// ❌ 错误：锁粒度过大
@CacheLock(key = "'global'", expire = 60)
public void updateUserInfo(String userId, UserInfo info) {
    // 锁住了所有用户的更新操作
}

// ✅ 正确：细粒度锁
@CacheLock(key = "#userId", expire = 10)
public void updateUserInfo(String userId, UserInfo info) {
    // 只锁住特定用户
}
```

### 2. 超时时间设置

```java
// 根据业务执行时间合理设置
@CacheLock(
    key = "#orderId",
    expire = 30,        // 锁过期时间30秒
    waitTime = 5,       // 等待时间5秒
    fair = true         // 启用看门狗自动续期
)
public void processComplexOrder(String orderId) {
    // 复杂业务逻辑，可能执行时间较长
}
```

### 3. 异常处理

```java
@Service
public class CriticalService {
    
    @Autowired
    @Qualifier("redisDistributedLock")
    private RedisLock lock;
    
    public void criticalOperation(String resourceId) {
        String lockKey = "critical:" + resourceId;
        String requestId = UUID.randomUUID().toString();
        boolean locked = false;
        
        try {
            locked = lock.tryLock(lockKey, requestId, 10, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("资源正在被占用");
            }
            
            // 执行关键操作
            doCriticalWork(resourceId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("锁获取被中断", e);
            throw new BusinessException("操作被中断");
        } catch (Exception e) {
            log.error("关键操作执行失败", e);
            // 根据业务决定是否回滚
            rollbackIfNeeded(resourceId);
            throw e;
        } finally {
            if (locked) {
                try {
                    lock.unlock(lockKey, requestId);
                } catch (Exception e) {
                    log.error("锁释放失败", e);
                    // 可以考虑强制释放或等待过期
                }
            }
        }
    }
}
```

### 4. 监控和告警

```java
@Component
@Slf4j
public class LockMonitor {
    
    @Autowired
    private List<RedisLock> locks;
    
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void monitorLocks() {
        List<String> criticalLocks = Arrays.asList(
            "payment:*",
            "inventory:*",
            "order:create:*"
        );
        
        for (String pattern : criticalLocks) {
            // 检查锁的状态
            Set<String> keys = redisTemplate.keys(pattern);
            for (String key : keys) {
                long remainTime = locks.get(0).getRemainTime(key);
                if (remainTime > 300000) { // 超过5分钟
                    log.warn("锁持有时间过长：{}, 剩余时间：{}ms", key, remainTime);
                    // 发送告警
                    alertService.sendAlert("锁持有时间异常", key, remainTime);
                }
            }
        }
    }
}
```

## 五、性能优化建议

1. **合理使用锁类型**：根据场景选择合适的锁类型，避免过度设计
2. **控制锁粒度**：锁的范围越小越好，减少锁竞争
3. **设置合理超时**：根据业务执行时间设置，避免死锁
4. **使用看门狗**：对于执行时间不确定的业务，启用自动续期
5. **监控锁状态**：及时发现和处理锁异常情况

## 六、常见问题

### Q1: 如何避免死锁？
A: 使用多重锁时会自动排序，单独使用时注意获取锁的顺序一致。

### Q2: 锁过期了业务还没执行完怎么办？
A: 启用看门狗自动续期，或者设置更长的过期时间。

### Q3: 如何处理锁释放失败？
A: 设置合理的过期时间，依靠Redis自动过期机制兜底。

### Q4: 性能有问题怎么优化？
A: 检查锁粒度是否过大，考虑使用读写锁或分段锁。 