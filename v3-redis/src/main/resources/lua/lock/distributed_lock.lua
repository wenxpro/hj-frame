-- 分布式锁获取脚本
-- KEYS[1] - 锁的key
-- ARGV[1] - 请求ID
-- ARGV[2] - 过期时间（秒）
-- 返回值：1-成功，0-失败

if redis.call('exists', KEYS[1]) == 0 then
    redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2])
    return 1
elseif redis.call('get', KEYS[1]) == ARGV[1] then
    redis.call('expire', KEYS[1], ARGV[2])
    return 1
else
    return 0
end 