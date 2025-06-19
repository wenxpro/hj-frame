-- 可重入锁释放脚本
-- KEYS[1] - 锁的key
-- ARGV[1] - 请求ID
-- 返回值：>=0-成功（返回剩余重入次数），-1-失败

local key = KEYS[1]
local requestId = ARGV[1]

local lockValue = redis.call('hget', key, 'owner')
if lockValue == requestId then
    local count = redis.call('hincrby', key, 'count', -1)
    if count <= 0 then
        -- 完全释放锁
        redis.call('del', key)
        return 1
    else
        -- 减少重入次数
        return count
    end
else
    return 0
end 