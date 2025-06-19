-- 公平锁释放脚本
-- KEYS[1] - 锁的key
-- KEYS[2] - 等待队列key
-- KEYS[3] - 锁拥有者key
-- ARGV[1] - 请求ID
-- 返回值：1-成功，0-失败

local key = KEYS[1]
local queueKey = KEYS[2]
local ownerKey = KEYS[3]
local requestId = ARGV[1]

-- 检查是否是锁的拥有者
if redis.call('get', key) == requestId then
    redis.call('del', key)
    redis.call('del', ownerKey)
    
    -- 通知队列中的下一个请求
    local nextInQueue = redis.call('lindex', queueKey, 0)
    if nextInQueue then
        -- 发布通知事件
        redis.call('publish', key .. ':notify', nextInQueue)
    end
    
    return 1
else
    -- 从队列中移除（可能在等待中）
    redis.call('lrem', queueKey, 0, requestId)
    return 0
end 