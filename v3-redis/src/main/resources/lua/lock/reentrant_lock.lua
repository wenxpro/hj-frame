-- 可重入锁获取脚本
-- KEYS[1] - 锁的key
-- ARGV[1] - 请求ID
-- ARGV[2] - 过期时间（秒）
-- 返回值：>0-成功（返回重入次数），0-失败

local key = KEYS[1]
local requestId = ARGV[1]
local expire = ARGV[2]

local lockValue = redis.call('hget', key, 'owner')
if lockValue == requestId then
    -- 已经拥有锁，增加重入次数
    local count = redis.call('hincrby', key, 'count', 1)
    redis.call('expire', key, expire)
    return count
elseif lockValue == false then
    -- 锁不存在，获取锁
    redis.call('hset', key, 'owner', requestId)
    redis.call('hset', key, 'count', 1)
    redis.call('expire', key, expire)
    return 1
else
    -- 锁被其他线程持有
    return 0
end 