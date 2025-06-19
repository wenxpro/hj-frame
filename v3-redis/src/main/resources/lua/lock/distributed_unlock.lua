-- 分布式锁释放脚本
-- KEYS[1] - 锁的key
-- ARGV[1] - 请求ID
-- 返回值：1-成功，0-失败

if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end 