# MapStruct + Lombok 使用指南

## 配置说明

项目已配置 MapStruct 和 Lombok，两者可以完美配合使用。

## 使用示例

### 1. 在 Service 中使用 MapStruct Mapper

```java
@Service
@RequiredArgsConstructor
public class PlayerService {
    
    // 注入 MapStruct Mapper（Spring 会自动生成实现类）
    private final PlayerMapStructMapper playerMapStructMapper;
    
    // MyBatis Plus Mapper（用于数据库操作）
    private final com.tower.game.mapper.PlayerMapper playerMapper;
    
    /**
     * 获取玩家信息（返回 DTO）
     */
    public PlayerDTO getPlayerDTOById(Long id) {
        Player player = playerMapper.selectById(id);
        // 使用 MapStruct 转换为 DTO
        return playerMapStructMapper.toDTO(player);
    }
    
    /**
     * 获取所有玩家（返回 DTO 列表）
     */
    public List<PlayerDTO> getAllPlayerDTOs() {
        List<Player> players = playerMapper.selectList(null);
        // 批量转换
        return playerMapStructMapper.toDTOList(players);
    }
    
    /**
     * 保存玩家（从 DTO 转换）
     */
    public boolean savePlayerFromDTO(PlayerDTO playerDTO) {
        Player player = playerMapStructMapper.toEntity(playerDTO);
        // 设置密码（需要单独处理）
        player.setPassword("加密后的密码");
        return playerMapper.insert(player) > 0;
    }
}
```

### 2. MapStruct 常用注解

#### @Mapper
- `componentModel = "spring"`: 生成 Spring Bean，可以通过 `@Autowired` 注入
- `componentModel = "default"`: 使用 `INSTANCE` 静态方法访问

#### @Mapping
```java
@Mapping(target = "password", ignore = true)  // 忽略字段
@Mapping(target = "createTime", expression = "java(java.time.LocalDateTime.now())")  // 自定义表达式
@Mapping(source = "username", target = "name")  // 字段名不同时映射
```

#### @Mappings
```java
@Mappings({
    @Mapping(target = "password", ignore = true),
    @Mapping(target = "id", ignore = true)
})
PlayerDTO toDTO(Player player);
```

### 3. 字段映射规则

- **同名字段**: 自动映射
- **不同名字段**: 使用 `@Mapping(source = "xxx", target = "yyy")`
- **忽略字段**: 使用 `@Mapping(target = "xxx", ignore = true)`
- **自定义转换**: 使用 `expression` 或自定义方法

### 4. 编译后生成的文件

MapStruct 会在编译时生成实现类，位置在：
```
target/generated-sources/annotations/com/tower/game/common/mapper/PlayerMapStructMapperImpl.java
```

## 优势

1. **性能**: 编译时生成代码，运行时无反射，性能接近手写代码
2. **类型安全**: 编译时检查，避免运行时错误
3. **与 Lombok 兼容**: 完美支持 Lombok 的 `@Data`、`@Builder` 等注解
4. **IDE 支持**: 生成的代码可以在 IDE 中查看和调试

## 注意事项

1. 首次使用需要执行 `mvn clean compile` 生成实现类
2. 修改 Mapper 接口后需要重新编译
3. 如果字段名不同，需要使用 `@Mapping` 注解指定映射关系
4. 密码等敏感字段建议在 DTO 中不包含，使用 `ignore = true` 忽略
