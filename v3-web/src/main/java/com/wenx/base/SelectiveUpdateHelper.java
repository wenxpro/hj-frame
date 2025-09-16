package com.wenx.base;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wenx.base.domain.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 选择性更新辅助工具类
 *
 * @author wenx
 */
public class SelectiveUpdateHelper {

    private static final Logger logger = LoggerFactory.getLogger(SelectiveUpdateHelper.class);

    /**
     * 选择性更新实体对象的非空字段
     *
     * @param service MyBatis-Plus 服务接口
     * @param entity  BaseEntity 实体对象
     * @throws IllegalArgumentException 当参数无效时
     * @throws RuntimeException         当更新操作失败时
     */
    @SuppressWarnings("unchecked")
    public static void updateBySelect(IService<?> service, BaseEntity entity) {
        // 参数验证
        validateParameters(service, entity);

        logger.debug("开始选择性更新实体: {}, ID: {}", entity.getClass().getSimpleName(),
                getEntityIdValueSafely(entity));

        try {
            // 获取并验证ID值
            Object idValue = getEntityIdValue(entity);

            // 构建更新条件
            UpdateWrapper<Object> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", idValue);

            // 遍历所有字段，只更新非空字段
            boolean hasUpdateFields = processEntityFields(entity, updateWrapper);

            if (!hasUpdateFields) {
                logger.warn("实体 {} (ID: {}) 没有需要更新的字段",
                        entity.getClass().getSimpleName(), idValue);
                throw new IllegalArgumentException("没有需要更新的字段");
            }

            // 执行更新
            ((IService<Object>) service).update(updateWrapper);

            logger.info("成功更新实体: {} (ID: {})", entity.getClass().getSimpleName(), idValue);

        } catch (IllegalArgumentException e) {
            // 参数验证异常直接抛出
            logger.error("参数验证失败: {}", e.getMessage());
            throw e;
        } catch (NoSuchFieldException e) {
            logger.error("实体类 {} 缺少必要的ID字段", entity.getClass().getSimpleName(), e);
            throw new RuntimeException("实体类缺少必要的ID字段: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error("无法访问实体 {} 的字段", entity.getClass().getSimpleName(), e);
            throw new RuntimeException("无法访问实体字段，请检查字段访问权限: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("选择性更新实体 {} 失败", entity.getClass().getSimpleName(), e);
            throw new RuntimeException("选择性更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 选择性保存实体对象的非空字段
     *
     * @param service MyBatis-Plus 服务接口
     * @param entity  BaseEntity 实体对象
     * @throws IllegalArgumentException 当参数无效时
     * @throws RuntimeException         当保存操作失败时
     */
    @SuppressWarnings("unchecked")
    public static void saveBySelect(IService<?> service, BaseEntity entity) {
        // 参数验证
        validateParameters(service, entity);

        logger.debug("开始选择性保存实体: {}", entity.getClass().getSimpleName());

        try {
            // 检查是否有ID值，决定是新增还是更新
            Object idValue = getEntityIdValueSafely(entity);

            if (idValue != null) {
                // 有ID值，执行更新操作
                logger.debug("实体已有ID: {}, 执行选择性更新", idValue);
                updateBySelect(service, entity);
            } else {
                // 无ID值，执行新增操作
                logger.debug("实体无ID值，执行选择性新增");

                // 构建新增实体，只包含非空字段
                BaseEntity newEntity = createEntityWithNonNullFields(entity);

                // 执行保存操作
                ((IService<Object>) service).save(newEntity);

                logger.info("选择性保存成功: {}", entity.getClass().getSimpleName());
            }

        } catch (IllegalArgumentException e) {
            // 参数验证异常，直接抛出
            logger.error("选择性保存失败 - 参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 其他异常
            logger.error("选择性保存失败: {}", e.getMessage(), e);
            throw new RuntimeException("选择性保存操作失败", e);
        }
    }

    /**
     * 创建只包含非空字段的实体对象
     *
     * @param sourceEntity 源实体对象
     * @return 只包含非空字段的新实体对象
     * @throws RuntimeException 当创建实体失败时
     */
    @SuppressWarnings("unchecked")
    private static BaseEntity createEntityWithNonNullFields(BaseEntity sourceEntity) {
        try {
            // 创建同类型的新实体实例
            Class<?> entityClass = sourceEntity.getClass();
            BaseEntity newEntity = (BaseEntity) entityClass.getDeclaredConstructor().newInstance();

            // 获取所有字段
            List<Field> allFields = getAllFields(entityClass);

            // 遍历所有字段，复制非空字段值
            for (Field field : allFields) {
                // 跳过不需要处理的字段
                if (shouldSkipField(field)) {
                    continue;
                }

                field.setAccessible(true);
                Object fieldValue = field.get(sourceEntity);

                // 只复制非空字段
                if (fieldValue != null) {
                    field.set(newEntity, fieldValue);
                }
            }

            return newEntity;

        } catch (Exception e) {
            logger.error("创建实体对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建实体对象失败: " + sourceEntity.getClass().getSimpleName(), e);
        }
    }

    /**
     * 验证输入参数
     *
     * @param service 服务对象
     * @param entity  实体对象
     * @throws IllegalArgumentException 当参数无效时
     */
    private static void validateParameters(IService<?> service, BaseEntity entity) {
        if (service == null) {
            throw new IllegalArgumentException("服务对象不能为空");
        }
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为空");
        }
    }

    /**
     * 安全地获取实体ID值（用于日志记录）
     *
     * @param entity 实体对象
     * @return ID值，如果获取失败返回"unknown"
     */
    private static Object getEntityIdValueSafely(BaseEntity entity) {
        try {
            return getEntityIdValue(entity);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取实体的ID值
     *
     * @param entity 实体对象
     * @return ID值
     * @throws IllegalArgumentException 如果ID为空
     * @throws NoSuchFieldException     如果找不到ID字段
     * @throws IllegalAccessException   如果无法访问ID字段
     */
    private static Object getEntityIdValue(BaseEntity entity) throws NoSuchFieldException, IllegalAccessException {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        Object idValue = idField.get(entity);

        if (idValue == null) {
            throw new IllegalArgumentException("实体 ID 不能为空");
        }

        return idValue;
    }

    /**
     * 处理实体字段，将非空字段添加到更新条件中
     *
     * @param entity        实体对象
     * @param updateWrapper 更新条件包装器
     * @return 是否有需要更新的字段
     * @throws IllegalAccessException 如果无法访问字段
     */
    private static boolean processEntityFields(BaseEntity entity, UpdateWrapper<Object> updateWrapper)
            throws IllegalAccessException {
        List<Field> allFields = getAllFields(entity.getClass());
        boolean hasUpdateFields = false;

        for (Field field : allFields) {
            if (shouldSkipField(field)) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(entity);

            // 只更新非空字段
            if (value != null) {
                String columnName = getColumnName(field);
                updateWrapper.set(columnName, value);
                hasUpdateFields = true;
            }
        }

        return hasUpdateFields;
    }

    /**
     * 获取类及其父类的所有字段
     *
     * @param clazz 类对象
     * @return 所有字段列表
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> allFields = Arrays.stream(clazz.getDeclaredFields())
                .collect(Collectors.toList());

        // 递归获取父类字段
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            allFields.addAll(getAllFields(superClass));
        }

        return allFields;
    }

    /**
     * 判断是否应该跳过该字段
     *
     * @param field 字段
     * @return 是否跳过
     */
    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();

        // 跳过 ID 字段、静态字段、final字段和transient字段
        return "id".equals(field.getName()) ||
                field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableId.class) ||
                Modifier.isStatic(modifiers) ||
                Modifier.isFinal(modifiers) ||
                Modifier.isTransient(modifiers);
    }

    /**
     * 获取字段对应的数据库列名
     *
     * @param field 字段
     * @return 数据库列名
     */
    private static String getColumnName(Field field) {
        // 检查是否有 @TableField 注解指定列名
        if (field.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableField.class)) {
            com.baomidou.mybatisplus.annotation.TableField tableField =
                    field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
            if (!tableField.value().isEmpty()) {
                return tableField.value();
            }
        }

        // 默认使用字段名转下划线
        return camelToUnderscore(field.getName());
    }

    /**
     * 驼峰命名转下划线命名
     *
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}