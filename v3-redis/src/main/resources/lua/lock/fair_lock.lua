-- 公平锁获取脚本
-- KEYS[1] - 锁的key
-- KEYS[2] - 等待队列key
-- KEYS[3] - 锁拥有者key
-- ARGV[1] - 请求ID
-- ARGV[2] - 过期时间（秒）
-- 返回值：1-成功，0-失败

local key = KEYS[1]
local queueKey = KEYS[2]
local ownerKey = KEYS[3]
local requestId = ARGV[1]
local expire = ARGV[2]

-- 检查是否已经拥有锁
local owner = redis.call('get', ownerKey)
if owner == requestId then
    redis.call('expire', key, expire)
    redis.call('expire', ownerKey, expire)
    return 1
end

-- 将请求ID加入队列
local inQueue = redis.call('lpos', queueKey, requestId)
if not inQueue then
    redis.call('rpush', queueKey, requestId)
end

-- 检查是否可以获取锁
if redis.call('exists', key) == 0 then
    -- 锁不存在，从队列头部取出请求ID
    local firstInQueue = redis.call('lindex', queueKey, 0)
    if firstInQueue == requestId then
        -- 当前请求在队列头部，可以获取锁
        redis.call('lpop', queueKey)
        redis.call('set', key, requestId, 'EX', expire)
        redis.call('set', ownerKey, requestId, 'EX', expire)
        return 1
    end
end

return 0 