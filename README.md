
### V3 框架层采用分层模块化设计，各模块职责清晰，依赖关系明确：

```
v3-frame
├── v3-core      # 核心组件 - 基础工具类、常量、异常处理、统一响应格式
├── v3-data      # 数据访问 - MyBatis Plus、数据源配置、事务管理
├── v3-redis     # 缓存组件 - Redis操作、分布式锁实现
├── v3-secure    # 安全组件 - 用户认证、权限管理、安全切面
├── v3-seata     # 分布式事务 - Seata集成和自动配置
└── v3-web       # Web组件 - 统一配置、异常处理、基础Controller、消息总线
```

## 核心特性

### 1. 统一响应格式

标准化所有API接口的返回格式，提供统一的成功和失败响应处理：

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

### 2. 复合注解配置

一键启用微服务常用功能的复合注解：

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

### 3. 基础控制器

提供标准的查询接口：

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

扩展查询控制器，提供完整的CRUD接口：

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

### 4. CQRS模式支持

支持CQRS（命令查询职责分离）模式，统一的MessageBus（消息总线）处理：

#### Message（消息基类）

```java
public class CreateUserCommand extends Message implements Message.Command {
    private final String username;
    private final String email;
    
    public CreateUserCommand(String username, String email) {
        super("CREATE_USER_COMMAND");
        this.username = username;
        this.email = email;
    }
    
    // getters...
}
```

#### MessageHandler（消息处理器）

```java
@Component
public class CreateUserCommandHandler implements MessageHandler<CreateUserCommand, UserDto> {
    
    @Override
    public UserDto handle(CreateUserCommand command) {
        // 业务逻辑处理
        return userService.createUser(command.getUsername(), command.getEmail());
    }
    
    @Override
    public Class<CreateUserCommand> getMessageType() {
        return CreateUserCommand.class;
    }
}
```

#### 在控制器中使用

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final MessageBus messageBus;
    
    @PostMapping
    public R<UserDto> createUser(@RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
            request.getUsername(),
            request.getEmail()
        );
        
        UserDto user = messageBus.send(command);
        return R.ok(user);
    }
    
    @GetMapping("/{id}")
    public R<UserDto> getUserById(@PathVariable Long id) {
        GetUserByIdQuery query = new GetUserByIdQuery(id);
        UserDto user = messageBus.send(query);
        return R.ok(user);
    }
}
```

### 5. 全局异常处理

统一的异常处理机制，支持多种异常类型：

- **业务异常**：BusinessException
- **服务异常**：ServiceException
- **认证异常**：AuthenticationException
- **参数验证异常**：MethodArgumentNotValidException
- **权限异常**：AccessDeniedException

### 6. 安全切面

基于注解的权限和角色验证：

```java
@RestController
public class AdminController {
    
    @RequiresPermissions("user:delete")
    @DeleteMapping("/users/{id}")
    public R deleteUser(@PathVariable Long id) {
        // 需要user:delete权限
        return R.ok();
    }
    
    @RequiresRoles("admin")
    @GetMapping("/admin/stats")
    public R getStats() {
        // 需要admin角色
        return R.ok();
    }
}
```

### 7. 分布式锁

Redis实现的可重入锁：

```java
@Autowired
private RedisReentrantLock redisLock;

public void processOrder(String orderId) {
    String lockKey = "order:" + orderId;
    if (redisLock.tryLock(lockKey, 30, TimeUnit.SECONDS)) {
        try {
            // 业务处理
        } finally {
            redisLock.unlock(lockKey);
        }
    }
}
```

