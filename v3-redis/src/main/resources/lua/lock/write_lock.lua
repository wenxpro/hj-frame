-- 写锁获取脚本
-- KEYS[1] - 锁的key
-- KEYS[2] - 读锁计数器key
-- KEYS[3] - 写锁key
-- KEYS[4] - 锁模式key
-- ARGV[1] - 请求ID
-- ARGV[2] - 过期时间（秒）
-- 返回值：1-成功，0-失败

local key = KEYS[1]
local readCountKey = KEYS[2]
local writeKey = KEYS[3]
local modeKey = KEYS[4]
local requestId = ARGV[1]
local expire = ARGV[2]

-- 检查是否已经拥有写锁
local writeOwner = redis.call('get', writeKey)
if writeOwner == requestId then
    redis.call('expire', writeKey, expire)
    redis.call('expire', key, expire)
    redis.call('expire', modeKey, expire)
    return 1
end

-- 检查是否有其他锁
local mode = redis.call('get', modeKey)
if mode then
    return 0
end

-- 获取写锁
redis.call('set', writeKey, requestId, 'EX', expire)
redis.call('set', modeKey, 'WRITE', 'EX', expire)
redis.call('set', key, 'WRITE_LOCK', 'EX', expire)

return 1 