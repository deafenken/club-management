package com.club.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.entity.*;
import com.club.enums.NotificationTypeEnum;
import com.club.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 通知服务 — 发送/查询/已读/撤回/偏好/日志/定时清理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysNotificationService extends ServiceImpl<SysNotificationMapper, SysNotification> {

    private final SysNotificationLogMapper logMapper;
    private final SysNotificationPreferenceMapper preferenceMapper;
    private final UserMapper userMapper;

    /** 未读数量本地缓存：userId → count（减少COUNT查询） */
    private final ConcurrentHashMap<Long, Integer> unreadCache = new ConcurrentHashMap<>();
    /** 缓存过期时间戳：userId → 过期毫秒 */
    private final ConcurrentHashMap<Long, Long> cacheExpiry = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30_000; // 30秒

    // ============================================================
    //  发送通知（核心方法，其他所有推送走这里）
    // ============================================================

    /**
     * 给单个用户发送通知，含防重复逻辑（同user+type+businessId在5分钟内不重复）
     */
    public SysNotification notify(Long userId, String title, String content,
                                   NotificationTypeEnum type, Long businessId) {
        // 检查用户偏好
        if (!isPreferenceEnabled(userId, type.getCode())) return null;
        // 防重复：5分钟内同user+type+businessId不重复
        LocalDateTime fiveMinAgo = LocalDateTime.now().minusMinutes(5);
        long dupCount = count(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getBusinessType, type.getCode())
                .eq(businessId != null, SysNotification::getBusinessId, businessId)
                .eq(SysNotification::getIsDelete, 0)
                .gt(SysNotification::getCreateTime, fiveMinAgo));
        if (dupCount > 0) return null;

        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setBusinessType(type.getCode());
        n.setType(type.getCategory());
        n.setBusinessId(businessId);
        n.setIsRead(0);
        n.setIsDelete(0);
        n.setIsRevoked(0);
        n.setCreateTime(LocalDateTime.now());
        save(n);

        // 更新缓存
        unreadCache.merge(userId, 1, Integer::sum);
        return n;
    }

    /** 兼容旧调用（无businessId） */
    public SysNotification notify(Long userId, String title, String content, String typeCode) {
        return notify(userId, title, content, NotificationTypeEnum.fromCode(typeCode), null);
    }

    /**
     * 批量发送通知，分批插入+事务保证原子性
     */
    @Transactional
    public void notifyBatch(List<Long> userIds, String title, String content,
                             NotificationTypeEnum type, Long businessId) {
        if (userIds == null || userIds.isEmpty()) return;
        final int BATCH_SIZE = 200;
        for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
            List<Long> batch = userIds.subList(i, Math.min(i + BATCH_SIZE, userIds.size()));
            List<SysNotification> list = new ArrayList<>();
            for (Long uid : batch) {
                if (!isPreferenceEnabled(uid, type.getCode())) continue;
                // 防重复
                LocalDateTime fiveMinAgo = LocalDateTime.now().minusMinutes(5);
                long dupCount = count(new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, uid)
                        .eq(SysNotification::getBusinessType, type.getCode())
                        .eq(businessId != null, SysNotification::getBusinessId, businessId)
                        .eq(SysNotification::getIsDelete, 0)
                        .gt(SysNotification::getCreateTime, fiveMinAgo));
                if (dupCount > 0) continue;

                SysNotification n = new SysNotification();
                n.setUserId(uid);
                n.setTitle(title);
                n.setContent(content);
                n.setBusinessType(type.getCode());
                n.setType(type.getCategory());
                n.setBusinessId(businessId);
                n.setIsRead(0);
                n.setIsDelete(0);
                n.setIsRevoked(0);
                n.setCreateTime(LocalDateTime.now());
                list.add(n);
                unreadCache.merge(uid, 1, Integer::sum);
            }
            if (!list.isEmpty()) saveBatch(list);
        }
    }

    /** 兼容旧批量调用 */
    public void notifyBatch(List<Long> userIds, String title, String content, String typeCode) {
        notifyBatch(userIds, title, content, NotificationTypeEnum.fromCode(typeCode), null);
    }

    // ============================================================
    //  查询
    // ============================================================

    /** 获取未读数量（优先读缓存） */
    public int unreadCount(Long userId) {
        Long expiry = cacheExpiry.get(userId);
        if (expiry != null && System.currentTimeMillis() < expiry) {
            Integer cached = unreadCache.get(userId);
            if (cached != null) return cached;
        }
        int count = (int) count(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0)
                .eq(SysNotification::getIsDelete, 0));
        unreadCache.put(userId, count);
        cacheExpiry.put(userId, System.currentTimeMillis() + CACHE_TTL_MS);
        return count;
    }

    /** 分页获取通知列表（支持类型筛选） */
    public List<SysNotification> listByUser(Long userId, int page, int size, String filterType) {
        var w = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsDelete, 0);
        if (filterType != null && !filterType.isEmpty() && !"ALL".equals(filterType)) {
            w.eq(SysNotification::getType, filterType);
        }
        w.orderByDesc(SysNotification::getCreateTime);
        // pageSize上限200
        int safeSize = Math.min(size, 200);
        return list(w.last("LIMIT " + ((page - 1) * safeSize) + "," + safeSize));
    }

    /** 获取总数（带筛选） */
    public int countByUser(Long userId, String filterType) {
        var w = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsDelete, 0);
        if (filterType != null && !filterType.isEmpty() && !"ALL".equals(filterType)) {
            w.eq(SysNotification::getType, filterType);
        }
        return (int) count(w);
    }

    // ============================================================
    //  已读操作
    // ============================================================

    /** 标记单条已读（含权限校验） */
    public void markRead(Long id, Long userId) {
        SysNotification n = getById(id);
        if (n == null || !n.getUserId().equals(userId)) return; // 越权拦截
        if (n.getIsRead() == 1) return; // 幂等
        n.setIsRead(1);
        n.setReadTime(LocalDateTime.now());
        updateById(n);
        // 更新缓存
        unreadCache.merge(userId, 1, (old, inc) -> Math.max(0, old - 1));
        // 操作日志
        logAction(id, userId, "READ", "标记已读");
    }

    /** 批量已读（按ID列表，单次最大200条） */
    public void markReadBatch(List<Long> ids, Long userId) {
        if (ids == null || ids.size() > 200) return;
        int count = 0;
        for (Long id : ids) {
            SysNotification n = getById(id);
            if (n == null || !n.getUserId().equals(userId) || n.getIsRead() == 1) continue;
            n.setIsRead(1);
            n.setReadTime(LocalDateTime.now());
            updateById(n);
            count++;
        }
        if (count > 0) {
            unreadCache.merge(userId, count, (old, inc) -> Math.max(0, old - inc));
            logAction(null, userId, "MARK_READ_BATCH", "批量已读" + count + "条");
        }
    }

    /** 全部已读（分页处理，每次200条防锁表） */
    public int markAllRead(Long userId) {
        int total = 0;
        final int BATCH = 200;
        while (true) {
            List<SysNotification> batch = list(new LambdaQueryWrapper<SysNotification>()
                    .eq(SysNotification::getUserId, userId)
                    .eq(SysNotification::getIsRead, 0)
                    .eq(SysNotification::getIsDelete, 0)
                    .last("LIMIT " + BATCH));
            if (batch.isEmpty()) break;
            for (SysNotification n : batch) {
                n.setIsRead(1);
                n.setReadTime(LocalDateTime.now());
            }
            updateBatchById(batch);
            total += batch.size();
        }
        unreadCache.put(userId, 0);
        cacheExpiry.put(userId, System.currentTimeMillis() + CACHE_TTL_MS);
        if (total > 0) logAction(null, userId, "MARK_ALL", "全部已读" + total + "条");
        return total;
    }

    // ============================================================
    //  撤回
    // ============================================================

    /** 撤回通知（仅管理员可调，标记is_revoked=1） */
    public boolean revoke(Long id) {
        SysNotification n = getById(id);
        if (n == null || n.getIsDelete() == 1) return false;
        n.setIsRevoked(1);
        updateById(n);
        // 未读→已读（撤回后不显示未读角标）
        if (n.getIsRead() == 0) {
            n.setIsRead(1);
            n.setReadTime(LocalDateTime.now());
            updateById(n);
            unreadCache.merge(n.getUserId(), 1, (old, inc) -> Math.max(0, old - 1));
        }
        logAction(id, n.getUserId(), "RECALL", "撤回通知");
        return true;
    }

    // ============================================================
    //  清空（逻辑删除）
    // ============================================================

    /** 一键清空当前用户所有通知（逻辑删除） */
    public int clearAll(Long userId) {
        int count = (int) count(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsDelete, 0));
        if (count == 0) return 0;
        // 逻辑删除
        LambdaUpdateWrapper<SysNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysNotification::getUserId, userId).set(SysNotification::getIsDelete, 1);
        update(uw);
        unreadCache.put(userId, 0);
        cacheExpiry.put(userId, System.currentTimeMillis() + CACHE_TTL_MS);
        logAction(null, userId, "CLEAR_ALL", "清空" + count + "条通知");
        return count;
    }

    // ============================================================
    //  偏好设置
    // ============================================================

    /** 检查用户是否启用某类通知 */
    public boolean isPreferenceEnabled(Long userId, String typeCode) {
        SysNotificationPreference pref = preferenceMapper.selectOne(
                new LambdaQueryWrapper<SysNotificationPreference>()
                        .eq(SysNotificationPreference::getUserId, userId)
                        .eq(SysNotificationPreference::getPreferType, typeCode));
        // 默认启用（无记录=启用）
        return pref == null || pref.getEnabled() == 1;
    }

    /** 获取用户偏好列表 */
    public List<SysNotificationPreference> getPreferences(Long userId) {
        // 确保所有类型都有记录
        List<SysNotificationPreference> existing = preferenceMapper.selectList(
                new LambdaQueryWrapper<SysNotificationPreference>()
                        .eq(SysNotificationPreference::getUserId, userId));
        Set<String> existingTypes = existing.stream()
                .map(SysNotificationPreference::getPreferType).collect(Collectors.toSet());
        for (NotificationTypeEnum type : NotificationTypeEnum.values()) {
            if (!existingTypes.contains(type.getCode())) {
                SysNotificationPreference pref = new SysNotificationPreference();
                pref.setUserId(userId);
                pref.setPreferType(type.getCode());
                pref.setEnabled(1);
                preferenceMapper.insert(pref);
                existing.add(pref);
            }
        }
        return existing;
    }

    /** 更新单条偏好 */
    public void updatePreference(Long userId, String typeCode, boolean enabled) {
        SysNotificationPreference pref = preferenceMapper.selectOne(
                new LambdaQueryWrapper<SysNotificationPreference>()
                        .eq(SysNotificationPreference::getUserId, userId)
                        .eq(SysNotificationPreference::getPreferType, typeCode));
        if (pref == null) {
            pref = new SysNotificationPreference();
            pref.setUserId(userId);
            pref.setPreferType(typeCode);
        }
        pref.setEnabled(enabled ? 1 : 0);
        if (pref.getId() != null) {
            preferenceMapper.updateById(pref);
        } else {
            preferenceMapper.insert(pref);
        }
    }

    // ============================================================
    //  操作日志
    // ============================================================

    private void logAction(Long notificationId, Long userId, String action, String detail) {
        try {
            SysNotificationLog logEntry = new SysNotificationLog();
            logEntry.setNotificationId(notificationId);
            logEntry.setUserId(userId);
            logEntry.setAction(action);
            logEntry.setDetail(detail);
            logEntry.setCreateTime(LocalDateTime.now());
            logMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("通知操作日志写入失败: {}", e.getMessage());
        }
    }

    // ============================================================
    //  定时清理任务（每天凌晨3点执行）
    // ============================================================

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldNotifications() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        LambdaUpdateWrapper<SysNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysNotification::getIsRead, 1)
          .eq(SysNotification::getIsDelete, 0)
          .lt(SysNotification::getCreateTime, threeMonthsAgo)
          .set(SysNotification::getIsDelete, 1);
        boolean updated = update(uw);
        if (updated) {
            log.info("定时任务：已清理3个月前已读通知");
        }
    }

    // ============================================================
    //  级联删除（业务删除时调用）
    // ============================================================

    /** 业务删除时同步逻辑删除关联通知 */
    public void deleteByBusiness(String businessType, Long businessId) {
        LambdaUpdateWrapper<SysNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysNotification::getBusinessType, businessType)
          .eq(SysNotification::getBusinessId, businessId)
          .set(SysNotification::getIsDelete, 1);
        update(uw);
    }
}
