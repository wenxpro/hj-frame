
### V3 框架层采用分层模块化设计，各模块职责清晰，依赖关系明确：

```
v3-frame
├── v3-core      # 核心组件 - 基础工具类、常量、异常处理
├── v3-data      # 数据访问 - MyBatis Plus、数据源配置
├── v3-redis     # 缓存组件 - Redis、分布式锁
├── v3-secure    # 安全组件 - 用户认证、权限管理
├── v3-seata     # 分布式事务 - Seata集成
└── v3-web       # Web组件 - 统一配置、异常处理、基础Controller
```

### 标准化所有API接口的返回格式，提供统一的成功和失败响应处理。

```java
// 成功响应
R.ok() // 默认成功
R.ok(data) // 带数据的成功响应
R.ok("操作成功", data) // 自定义消息

// 失败响应
R.failed() // 默认失败
R.failed("错误信息") // 自定义错误信息
R.failed(data, "错误信息") // 带数据的失败响应
```

**响应格式：**
```json
{
  "code": 0,
  "data": {...},
  "msg": "执行成功"
}
```

### 一键启用微服务常用功能的复合注解：

```java
@SpringBootApplication
@GlobalConfig(
    mapperScan = "com.wenx.*.mapper",      // MyBatis Mapper扫描
    feignPackages = "com.wenx",            // Feign客户端扫描
    componentPackages = "com.wenx"         // 组件扫描
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**功能特性：**
- `@MapperScan` - MyBatis Mapper接口扫描
- `@EnableDiscoveryClient` - 启用服务发现
- `@EnableCaching` - 启用缓存支持
- `@EnableFeignClients` - 启用Feign客户端
- `@ComponentScan` - 组件扫描
- `@EnableAsync` - 启用异步处理


### 提供标准的查询接口：

```java
public abstract class BaseReadController<D, Q, S> {
    
    @PostMapping("/page")
    public R page(Q query) {
        return R.ok(service.page(query));
    }
    
    @GetMapping("/get/{id}")
    public R get(@PathVariable Serializable id) {
        return R.ok(service.get(id));
    }
}
```

### 扩展查询控制器，提供完整的CRUD接口：

```java
public abstract class BaseRestController<D, Q, S> extends BaseReadController<D, Q, S> {
    
    @PostMapping("/add")
    public R add(@RequestBody D dto) {
        service.add(dto);
        return R.ok();
    }
    
    @DeleteMapping("/delete/{id}")
    public R delete(@PathVariable Serializable id) {
        service.delete(id);
        return R.ok();
    }
    
    @PutMapping("/update")
    public R update(@RequestBody D dto) {
        service.update(dto);
        return R.ok();
    }
}
```

**使用示例：**
```java
@RestController
@RequestMapping("/api/users")
public class UserController extends BaseRestController<UserDto, UserQuery, UserService> {
    
    public UserController(UserService service) {
        super(service);
    }
    
    // 自动获得以下接口：
    // POST /api/users/page - 分页查询
    // GET /api/users/get/{id} - 根据ID查询
    // POST /api/users/add - 新增
    // PUT /api/users/update - 更新
    // DELETE /api/users/delete/{id} - 删除
}
```

