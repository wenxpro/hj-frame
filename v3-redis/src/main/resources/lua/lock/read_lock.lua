-- 读锁获取脚本
-- KEYS[1] - 锁的key
-- KEYS[2] - 读锁计数器key
-- KEYS[3] - 读锁持有者集合key
-- KEYS[4] - 写锁key
-- KEYS[5] - 锁模式key
-- ARGV[1] - 请求ID
-- ARGV[2] - 过期时间（秒）
-- 返回值：1-成功，0-失败

local key = KEYS[1]
local readCountKey = KEYS[2]
local readHoldersKey = KEYS[3]
local writeKey = KEYS[4]
local modeKey = KEYS[5]
local requestId = ARGV[1]
local expire = ARGV[2]

-- 检查是否有写锁
local writeOwner = redis.call('get', writeKey)
if writeOwner and writeOwner ~= requestId then
    return 0
end

-- 获取或设置锁模式
local mode = redis.call('get', modeKey)
if not mode or mode == 'READ' then
    redis.call('set', modeKey, 'READ', 'EX', expire)
    
    -- 增加读锁计数
    local count = redis.call('hincrby', readCountKey, requestId, 1)
    redis.call('expire', readCountKey, expire)
    
    -- 添加到持有者集合
    redis.call('sadd', readHoldersKey, requestId)
    redis.call('expire', readHoldersKey, expire)
    
    -- 设置主键
    redis.call('set', key, 'READ_LOCK', 'EX', expire)
    
    return 1
end

return 0 