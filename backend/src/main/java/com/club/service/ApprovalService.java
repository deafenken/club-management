package com.club.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.club.entity.*;
import com.club.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多级审批引擎 —— 统一管理5类申请的多级审批流程
 *
 * 审批流配置：
 *   CLUB      : 社团管理员初审 → 校社联复审 → 校团委终审
 *   ACTIVITY  : 社团负责人预审(PRESIDENT) → 系统管理员审批(ADMIN)
 *   VENUE     : 管理员审批 (单级)
 *   RESOURCE  : 管理员审批 (单级)
 *   FUND      : <5000元: 社长初审→管理员审核→财务老师复核
 *               ≥5000元: 额外加校级领导终审
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRecordMapper approvalRecordMapper;
    private final ClubMapper clubMapper;
    private final ClubMemberMapper clubMemberMapper;
    private final ActivityMapper activityMapper;
    private final VenueBookingMapper venueBookingMapper;
    private final ResourceBorrowMapper resourceBorrowMapper;
    private final ResourceItemMapper resourceItemMapper;
    private final VenueMapper venueMapper;
    private final FundRecordMapper fundRecordMapper;
    private final UserMapper userMapper;
    private final SysNotificationService notificationService;

    // ============== 构建审批流 ==============

    /** 根据申请类型和业务对象创建审批流节点 */
    @Transactional
    public void createApprovalFlow(String appType, Long businessId, Object businessObj) {
        List<ApprovalRecord> flow = buildFlow(appType, businessId, businessObj);
        for (ApprovalRecord node : flow) {
            approvalRecordMapper.insert(node);
        }
        log.info("[审批流] {} 创建审批流, businessId={}, 节点数={}", appType, businessId, flow.size());
    }

    private List<ApprovalRecord> buildFlow(String appType, Long businessId, Object businessObj) {
        List<ApprovalRecord> flow = new ArrayList<>();
        switch (appType) {
            case "CLUB" -> {
                flow.add(node(appType, businessId, 1, "社团管理员初审"));
                flow.add(node(appType, businessId, 2, "校社联复审"));
                flow.add(node(appType, businessId, 3, "校团委终审"));
            }
            case "ACTIVITY" -> {
                // 社长创建 → 社团负责人预审
                flow.add(node(appType, businessId, 1, "社团负责人预审"));
                flow.add(node(appType, businessId, 2, "系统管理员审批"));
            }
            case "VENUE" -> {
                flow.add(node(appType, businessId, 1, "管理员审批"));
            }
            case "RESOURCE" -> {
                flow.add(node(appType, businessId, 1, "管理员审批"));
            }
            case "FUND" -> {
                flow.add(node(appType, businessId, 1, "社团负责人初审"));
                flow.add(node(appType, businessId, 2, "系统管理员审核"));
                flow.add(node(appType, businessId, 3, "财务老师复核"));
                // 大额(>=5000)加校级领导终审
                if (businessObj instanceof FundRecord fund) {
                    if (fund.getTotalAmount() != null && fund.getTotalAmount().compareTo(new BigDecimal("5000")) >= 0) {
                        flow.add(node(appType, businessId, 4, "校级领导终审"));
                    }
                } else if (businessObj instanceof Map<?,?> map) {
                    Object amt = map.get("totalAmount");
                    if (amt != null && new BigDecimal(amt.toString()).compareTo(new BigDecimal("5000")) >= 0) {
                        flow.add(node(appType, businessId, 4, "校级领导终审"));
                    }
                }
            }
        }
        return flow;
    }

    private ApprovalRecord node(String appType, Long businessId, int stepOrder, String stepName) {
        ApprovalRecord r = new ApprovalRecord();
        r.setAppType(appType);
        r.setBusinessId(businessId);
        r.setStepOrder(stepOrder);
        r.setStepName(stepName);
        r.setStatus("PENDING");
        r.setCreateTime(LocalDateTime.now());
        return r;
    }

    // ============== 审批操作 ==============

    /**
     * 审批通过当前待审批节点
     * @return true=全部审批完成, false=还有下一级
     */
    @Transactional
    public boolean approve(String appType, Long businessId, Long approverId, String comment) {
        // 找到当前待审批的最小stepOrder节点
        ApprovalRecord current = getCurrentPendingNode(appType, businessId);
        if (current == null) return true; // 已经没有待审批节点

        current.setApproverId(approverId);
        current.setStatus("APPROVED");
        current.setComment(comment);
        current.setUpdateTime(LocalDateTime.now());
        approvalRecordMapper.updateById(current);

        // 检查是否还有下一级
        ApprovalRecord next = getNextNode(appType, businessId, current.getStepOrder());
        if (next == null) {
            // 全部完成，更新业务状态为已通过
            updateBusinessStatus(appType, businessId, true, approverId);
            notifyApplicant(appType, businessId, "通过", null);
            return true;
        }
        // 通知下一级审批人
        notifyNextApprovers(appType, businessId, next);
        return false;
    }

    /**
     * 驳回申请
     */
    @Transactional
    public void reject(String appType, Long businessId, Long approverId, String reason) {
        // 标记当前节点为已驳回
        ApprovalRecord current = getCurrentPendingNode(appType, businessId);
        if (current != null) {
            current.setApproverId(approverId);
            current.setStatus("REJECTED");
            current.setComment(reason);
            current.setUpdateTime(LocalDateTime.now());
            approvalRecordMapper.updateById(current);
        }

        // 驳回后续所有节点
        List<ApprovalRecord> allPending = approvalRecordMapper.selectList(
            new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getAppType, appType)
                .eq(ApprovalRecord::getBusinessId, businessId)
                .eq(ApprovalRecord::getStatus, "PENDING"));
        for (ApprovalRecord r : allPending) {
            r.setStatus("REJECTED");
            r.setComment("上级已驳回");
            r.setUpdateTime(LocalDateTime.now());
            approvalRecordMapper.updateById(r);
        }

        // 更新业务状态为已驳回
        updateBusinessStatus(appType, businessId, false, approverId);
        // 存入业务表驳回理由
        saveRejectReason(appType, businessId, reason);
        notifyApplicant(appType, businessId, "驳回", reason);
    }

    // ============== 统一申请中心查询 ==============

    /** 查询某用户的全部申请及审批状态 */
    public List<Map<String, Object>> getMyApplications(Long userId, String appType, String status) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (appType == null || "CLUB".equals(appType)) {
            results.addAll(queryClubApplications(userId, status));
        }
        if (appType == null || "ACTIVITY".equals(appType)) {
            results.addAll(queryActivityApplications(userId, status));
        }
        if (appType == null || "VENUE".equals(appType)) {
            results.addAll(queryVenueApplications(userId, status));
        }
        if (appType == null || "RESOURCE".equals(appType)) {
            results.addAll(queryResourceApplications(userId, status));
        }
        if (appType == null || "FUND".equals(appType)) {
            results.addAll(queryFundApplications(userId, status));
        }

        results.sort((a, b) -> {
            Object t1 = a.get("createTime");
            Object t2 = b.get("createTime");
            if (t1 == null || t2 == null) return 0;
            return t2.toString().compareTo(t1.toString());
        });
        return results;
    }

    /** 获取审批流程时间线 */
    public List<ApprovalRecord> getApprovalTimeline(String appType, Long businessId) {
        return approvalRecordMapper.selectList(
            new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getAppType, appType)
                .eq(ApprovalRecord::getBusinessId, businessId)
                .orderByAsc(ApprovalRecord::getStepOrder));
    }

    // ============== 草稿管理 ==============

    // (草稿逻辑在 ApplicationDraftService 中，这里留 API 入口)

    // ============== Private helpers ==============

    private ApprovalRecord getCurrentPendingNode(String appType, Long businessId) {
        List<ApprovalRecord> nodes = approvalRecordMapper.selectList(
            new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getAppType, appType)
                .eq(ApprovalRecord::getBusinessId, businessId)
                .eq(ApprovalRecord::getStatus, "PENDING")
                .orderByAsc(ApprovalRecord::getStepOrder));
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    private ApprovalRecord getNextNode(String appType, Long businessId, int currentStep) {
        List<ApprovalRecord> nodes = approvalRecordMapper.selectList(
            new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getAppType, appType)
                .eq(ApprovalRecord::getBusinessId, businessId)
                .gt(ApprovalRecord::getStepOrder, currentStep)
                .orderByAsc(ApprovalRecord::getStepOrder));
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    private void updateBusinessStatus(String appType, Long businessId, boolean approved, Long approverId) {
        switch (appType) {
            case "CLUB" -> {
                Club c = clubMapper.selectById(businessId);
                if (c != null) {
                    c.setStatus(approved ? 1 : 2);
                    clubMapper.updateById(c);
                }
            }
            case "ACTIVITY" -> {
                Activity a = activityMapper.selectById(businessId);
                if (a != null) {
                    a.setStatus(approved ? "APPROVED" : "REJECTED");
                    if (approved && a.getCheckinCode() == null) {
                        a.setCheckinCode(cn.hutool.core.util.RandomUtil.randomNumbers(6));
                    }
                    a.setApprovedBy(approverId);
                    activityMapper.updateById(a);
                }
            }
            case "VENUE" -> {
                VenueBooking b = venueBookingMapper.selectById(businessId);
                if (b != null) {
                    b.setStatus(approved ? "APPROVED" : "REJECTED");
                    venueBookingMapper.updateById(b);
                }
            }
            case "RESOURCE" -> {
                ResourceBorrow b = resourceBorrowMapper.selectById(businessId);
                if (b != null) {
                    b.setStatus(approved ? "APPROVED" : "REJECTED");
                    resourceBorrowMapper.updateById(b);
                    if (!approved) {
                        // 驳回时归还库存
                        ResourceItem item = resourceItemMapper.selectById(b.getItemId());
                        if (item != null) {
                            item.setAvailable(item.getAvailable() + b.getQuantity());
                            resourceItemMapper.updateById(item);
                        }
                    }
                }
            }
            case "FUND" -> {
                FundRecord f = fundRecordMapper.selectById(businessId);
                if (f != null) {
                    f.setStatus(approved ? "APPROVED" : "REJECTED");
                    fundRecordMapper.updateById(f);
                }
            }
        }
    }

    private void saveRejectReason(String appType, Long businessId, String reason) {
        switch (appType) {
            case "CLUB" -> {
                Club c = clubMapper.selectById(businessId);
                if (c != null) { c.setRejectReason(reason); clubMapper.updateById(c); }
            }
            case "FUND" -> {
                FundRecord f = fundRecordMapper.selectById(businessId);
                if (f != null) { f.setRejectReason(reason); fundRecordMapper.updateById(f); }
            }
        }
    }

    private void notifyApplicant(String appType, Long businessId, String result, String reason) {
        Long applicantId = getApplicantId(appType, businessId);
        String title = getBusinessTitle(appType, businessId);
        String content = result.equals("通过") ?
            "您的「" + title + "」申请已全部审批通过！" :
            "您的「" + title + "」申请已被驳回" + (reason != null && !reason.isEmpty() ? "，原因：" + reason : "");
        if (applicantId != null) {
            notificationService.notify(applicantId, "申请审批结果", content,
                appType + "_" + (result.equals("通过") ? "APPROVED" : "REJECTED"));
        }
    }

    private void notifyNextApprovers(String appType, Long businessId, ApprovalRecord nextNode) {
        // 通知所有管理员注意下一级审批
        List<User> admins = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
        List<Long> adminIds = admins.stream().map(User::getId).toList();
        String title = getBusinessTitle(appType, businessId);
        notificationService.notifyBatch(adminIds, "新的审批待办",
            "「" + title + "」需要" + nextNode.getStepName(), appType + "_PENDING");
    }

    private Long getApplicantId(String appType, Long businessId) {
        return switch (appType) {
            case "CLUB" -> { Club c = clubMapper.selectById(businessId); yield c != null ? c.getPresidentId() : null; }
            case "ACTIVITY" -> { Activity a = activityMapper.selectById(businessId); yield a != null ? a.getCreatedBy() : null; }
            case "VENUE" -> { VenueBooking b = venueBookingMapper.selectById(businessId); yield b != null ? b.getBookingUserId() : null; }
            case "RESOURCE" -> { ResourceBorrow b = resourceBorrowMapper.selectById(businessId); yield b != null ? b.getUserId() : null; }
            case "FUND" -> { FundRecord f = fundRecordMapper.selectById(businessId); yield f != null ? f.getApplicantId() : null; }
            default -> null;
        };
    }

    private String getBusinessTitle(String appType, Long businessId) {
        return switch (appType) {
            case "CLUB" -> { Club c = clubMapper.selectById(businessId); yield c != null ? c.getName() : "社团"; }
            case "ACTIVITY" -> { Activity a = activityMapper.selectById(businessId); yield a != null ? a.getTitle() : "活动"; }
            case "VENUE" -> { VenueBooking b = venueBookingMapper.selectById(businessId); yield "场地预约"; }
            case "RESOURCE" -> { ResourceBorrow b = resourceBorrowMapper.selectById(businessId); yield "物资借用"; }
            case "FUND" -> { FundRecord f = fundRecordMapper.selectById(businessId); yield "经费申请"; }
            default -> "申请";
        };
    }

    // ============== 申请查询辅助 ==============

    private List<Map<String, Object>> queryClubApplications(Long userId, String statusFilter) {
        List<Club> clubs = clubMapper.selectList(
            new LambdaQueryWrapper<Club>().eq(Club::getPresidentId, userId));
        if (clubs.isEmpty()) clubs = clubMapper.selectList(
            new LambdaQueryWrapper<Club>().eq(Club::getPresidentId, userId).eq(Club::getStatus, 0));
        // 也查发起人列表中的
        List<Club> all = new ArrayList<>(clubs);
        // 简化：直接查所有自己的

        return all.stream()
            .filter(c -> statusFilter == null || mapClubStatus(c.getStatus()).equals(statusFilter))
            .map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("appType", "CLUB");
                m.put("businessId", c.getId());
                m.put("title", c.getName());
                m.put("status", mapClubStatus(c.getStatus()));
                m.put("statusLabel", c.getStatus() == 0 ? "待审批" : c.getStatus() == 1 ? "已通过" : "已驳回");
                m.put("createTime", c.getCreateTime());
                m.put("rejectReason", c.getRejectReason());
                m.put("timeline", getApprovalTimeline("CLUB", c.getId()));
                return m;
            }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> queryActivityApplications(Long userId, String statusFilter) {
        List<Activity> acts = activityMapper.selectList(
            new LambdaQueryWrapper<Activity>().eq(Activity::getCreatedBy, userId));
        return acts.stream()
            .filter(a -> statusFilter == null || a.getStatus().equals(statusFilter))
            .map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("appType", "ACTIVITY");
                m.put("businessId", a.getId());
                m.put("title", a.getTitle());
                m.put("status", a.getStatus());
                m.put("statusLabel", activityStatusLabel(a.getStatus()));
                m.put("createTime", a.getCreateTime());
                m.put("timeline", getApprovalTimeline("ACTIVITY", a.getId()));
                return m;
            }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> queryVenueApplications(Long userId, String statusFilter) {
        List<VenueBooking> bookings = venueBookingMapper.selectList(
            new LambdaQueryWrapper<VenueBooking>().eq(VenueBooking::getBookingUserId, userId));
        return bookings.stream()
            .filter(b -> statusFilter == null || b.getStatus().equals(statusFilter))
            .map(b -> {
                Venue v = venueMapper.selectById(b.getVenueId());
                Map<String, Object> m = new HashMap<>();
                m.put("appType", "VENUE");
                m.put("businessId", b.getId());
                m.put("title", (v != null ? v.getName() : "场地") + "预约(" + b.getBookingDate() + ")");
                m.put("status", b.getStatus());
                m.put("createTime", b.getCreateTime());
                m.put("timeline", getApprovalTimeline("VENUE", b.getId()));
                return m;
            }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> queryResourceApplications(Long userId, String statusFilter) {
        List<ResourceBorrow> borrows = resourceBorrowMapper.selectList(
            new LambdaQueryWrapper<ResourceBorrow>().eq(ResourceBorrow::getUserId, userId));
        return borrows.stream()
            .filter(b -> statusFilter == null || b.getStatus().equals(statusFilter))
            .map(b -> {
                Map<String, Object> m = new HashMap<>();
                m.put("appType", "RESOURCE");
                m.put("businessId", b.getId());
                m.put("title", "物资借用");
                m.put("status", b.getStatus());
                m.put("createTime", b.getCreateTime());
                m.put("timeline", getApprovalTimeline("RESOURCE", b.getId()));
                return m;
            }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> queryFundApplications(Long userId, String statusFilter) {
        List<FundRecord> funds = fundRecordMapper.selectList(
            new LambdaQueryWrapper<FundRecord>().eq(FundRecord::getApplicantId, userId));
        return funds.stream()
            .filter(f -> statusFilter == null || f.getStatus().equals(statusFilter))
            .map(f -> {
                Map<String, Object> m = new HashMap<>();
                m.put("appType", "FUND");
                m.put("businessId", f.getId());
                m.put("title", "经费申请(¥" + f.getTotalAmount() + ")");
                m.put("status", f.getStatus());
                m.put("createTime", f.getCreateTime());
                m.put("rejectReason", f.getRejectReason());
                m.put("timeline", getApprovalTimeline("FUND", f.getId()));
                return m;
            }).collect(Collectors.toList());
    }

    private String mapClubStatus(Integer status) {
        return status == null ? "PENDING" : status == 0 ? "PENDING" : status == 1 ? "APPROVED" : "REJECTED";
    }

    private String activityStatusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "APPROVED" -> "报名中";
            case "REJECTED" -> "已驳回";
            case "ONGOING" -> "进行中";
            case "FINISHED" -> "已结束";
            default -> status;
        };
    }

    /** 获取待审批列表（管理员用） */
    public List<Map<String, Object>> getPendingApprovals(String appType) {
        List<ApprovalRecord> pending = approvalRecordMapper.selectList(
            new LambdaQueryWrapper<ApprovalRecord>()
                .eq(appType != null, ApprovalRecord::getAppType, appType)
                .eq(ApprovalRecord::getStatus, "PENDING")
                .orderByAsc(ApprovalRecord::getStepOrder));
        return pending.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("appType", r.getAppType());
            m.put("businessId", r.getBusinessId());
            m.put("stepOrder", r.getStepOrder());
            m.put("stepName", r.getStepName());
            m.put("title", getBusinessTitle(r.getAppType(), r.getBusinessId()));
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }
}
