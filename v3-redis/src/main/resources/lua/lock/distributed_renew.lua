-- 分布式锁续期脚本
-- KEYS[1] - 锁的key
-- ARGV[1] - 请求ID
-- ARGV[2] - 过期时间（秒）
-- 返回值：1-成功，0-失败

if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('expire', KEYS[1], ARGV[2])
else
    return 0
end 