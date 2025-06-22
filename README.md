

# V3 桌面开发框架

## 项目简介
V3 框架层采用分层模块化设计，各模块职责清晰，依赖关系明确。本框架提供统一的API响应格式、微服务常用功能注解、标准查询接口、完整的CRUD接口支持、CQRS模式实现等核心功能。

## 框架模块
```
v3-frame
├── v3-core      # 核心组件 - �Obj、常量、异常处理
├── v3-data      # 数据访问 - MyBatis Plus、数据源配置
├── v3-redis     # 缓存组件 - Redis、分布式锁
├── v3-secure    # 安全组件 - 用户认证、权限管理
├── v3-seata     # 分布式事务 - Seata集成
└── v3-web       # Web组件 - 统一配置、异常处理、基础Controller
```

## 核心功能

### 统一API响应格式
标准化所有API接口的返回格式，提供统一的成功和失败响应处理机制。

**响应格式示例：**
```json
{
  "code": 0,
  "data": {...},
  "msg": "执行成功"
}
```

**常用响应方法：**
```java
R.ok() // 默认成功响应
R.ok(data) // 带数据的成功响应
R.ok("操作成功", data) // 自定义消息
R.failed() // 默认失败响应
R.failed("错误信息") // 自定义错误信息
R.failed(data, "错误信息") // �2023-12-18 14:23:45
```

### 微服务常用功能注解
提供一键启用微服务常用功能的复合注解，包含以下功能特性：
- `@MapperScan` - MyBatis Mapper接口扫描
- `@EnableDiscoveryClient` - 启用服务发现
- `@EnableCaching` - 启用缓存支持
- `@EnableFeignClients` - 启用Feign客户端
- `@ComponentScan` - 组件扫描
- `@EnableAsync` - 启用异步处理

**使用示例：**
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

### 标准CRUD接口
通过扩展控制器提供完整的CRUD接口支持

**基础查询控制器：**
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

**完整CRUD控制器：**
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

### CQRS模式支持
支持CQRS（命令查询职责分离）模式，提供统一的MessageBus（消息总线）处理机制

**消息基类示例：**
```java
public class CreateUserCommand extends Message implements Message.Command {
    private final String username;
    private final String email;

    public CreateUserCommand(String username, String email) {
        super("CREATE_USER_COMMAND");
        this.username = username;
        this.email = email;
    }
}
```

**消息处理器示例：**
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

**控制器中使用示例：**
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
            request.getEmail(),
            request.getPassword()
        );
        UserDto user = messageBus.send(command);
        return R.success(user);
    }

    @GetMapping("/{id}")
    public R<UserDto> getUserById(@PathVariable Long id) {
        GetUserByIdQuery query = new GetUserByIdQuery(id);
        UserDto user = messageBus.send(query);
        return R.success(user);
    }
}
```