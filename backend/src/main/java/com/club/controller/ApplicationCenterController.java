package com.club.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.club.common.Result;
import com.club.entity.*;
import com.club.mapper.*;
import com.club.service.ApprovalService;
import com.club.service.SysNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 统一申请中心控制器 - 5类申请的完整表单、草稿、审批流、联动、核销
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationCenterController {

    private final ApprovalService approvalService;
    private final SysNotificationService notificationService;
    private final ClubMapper clubMapper;
    private final ClubMemberMapper clubMemberMapper;
    private final ActivityMapper activityMapper;
    private final VenueMapper venueMapper;
    private final VenueBookingMapper venueBookingMapper;
    private final ResourceItemMapper resourceItemMapper;
    private final ResourceBorrowMapper resourceBorrowMapper;
    private final FundRecordMapper fundRecordMapper;
    private final UserMapper userMapper;
    private final ApplicationDraftMapper draftMapper;
    private final ActivityClosureMapper activityClosureMapper;
    private final VenueDamageRecordMapper venueDamageRecordMapper;
    private final ResourceDamageRecordMapper resourceDamageRecordMapper;

    // ======================== 1. 社团创建(完整4步) ========================

    @PostMapping("/club/full")
    public Result<?> createClubFull(@RequestBody Club club) {
        Long userId = getCurrentUserId();
        long existing = clubMapper.selectCount(
            new LambdaQueryWrapper<Club>().eq(Club::getName, club.getName()));
        if (existing > 0) return Result.fail("已存在同名社团「" + club.getName() + "」");

        club.setPresidentId(userId);
        club.setStatus(0);
        club.setMemberCount(1);
        club.setCommitmentSigned(
            club.getCommitmentSigned() != null && club.getCommitmentSigned() == 1 ? 1 : 0);
        clubMapper.insert(club);

        // 创建者自动成为社长
        ClubMember pm = new ClubMember();
        pm.setClubId(club.getId()); pm.setUserId(userId);
        pm.setRole("PRESIDENT"); pm.setStatus(1);
        pm.setJoinTime(LocalDateTime.now());
        clubMemberMapper.insert(pm);

        // 创建多级审批流
        approvalService.createApprovalFlow("CLUB", club.getId(), club);
        notifyAdmins("新社团申请待审批",
            "「" + club.getName() + "」提交了社团创建申请");
        return Result.ok(Map.of("clubId", club.getId(),
            "msg", "社团申请已提交，请等待多级审批"));
    }

    // ======================== 2. 活动发布 ========================

    @PostMapping("/activity/full")
    public Result<?> createActivityFull(@RequestBody Activity act) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"PRESIDENT".equals(role))
            return Result.fail(403, "仅管理员和社长可发布活动");

        Club club = clubMapper.selectById(act.getClubId());
        if (club == null) return Result.fail("社团不存在");
        if ("PRESIDENT".equals(role) &&
            !userId.equals(club.getPresidentId()))
            return Result.fail(403, "您不是该社团的社长");

        // 收费管控
        if (act.getIsFee() != null && act.getIsFee() == 1 &&
            (act.getFeeAmount() == null ||
             act.getFeeAmount().compareTo(BigDecimal.ZERO) <= 0))
            return Result.fail("收费活动必须填写收费标准");

        act.setCreatedBy(userId);
        act.setStatus("ADMIN".equals(role) ? "APPROVED" : "PENDING");
        // 直接通过的活动立即生成签到码，否则永远无法签到
        if ("APPROVED".equals(act.getStatus()) && act.getCheckinCode() == null)
            act.setCheckinCode(cn.hutool.core.util.RandomUtil.randomNumbers(6));
        act.setEnrolledCount(0);
        activityMapper.insert(act);

        // 联动绑定场地/物资/经费
        linkActivityBindings(act);

        if ("PENDING".equals(act.getStatus())) {
            approvalService.createApprovalFlow("ACTIVITY", act.getId(), act);
            notifyAdmins("新活动待审批",
                "社团「" + club.getName() + "」发布了活动「" + act.getTitle() + "」");
        }
        return Result.ok(Map.of("activityId", act.getId(),
            "msg", "APPROVED".equals(act.getStatus())
                ? "活动发布成功" : "已提交审批"));
    }

    private void linkActivityBindings(Activity act) {
        if (act.getLinkedVenueBookingId() != null) {
            VenueBooking vb = venueBookingMapper
                .selectById(act.getLinkedVenueBookingId());
            if (vb != null) {
                vb.setActivityId(act.getId());
                venueBookingMapper.updateById(vb);
            }
        }
        if (act.getLinkedResourceBorrowId() != null) {
            ResourceBorrow rb = resourceBorrowMapper
                .selectById(act.getLinkedResourceBorrowId());
            if (rb != null) {
                rb.setActivityId(act.getId());
                resourceBorrowMapper.updateById(rb);
            }
        }
        if (act.getLinkedFundRecordId() != null) {
            FundRecord fr = fundRecordMapper
                .selectById(act.getLinkedFundRecordId());
            if (fr != null) {
                fr.setActivityId(act.getId());
                fundRecordMapper.updateById(fr);
            }
        }
    }

    // ======================== 3. 场地预约(完整表单+冲突校验) ========================

    @PostMapping("/venue/booking/full")
    public Result<?> bookVenueFull(@RequestBody VenueBooking booking) {
        Long userId = getCurrentUserId();
        booking.setBookingUserId(userId);

        // 容量校验
        Venue venue = venueMapper.selectById(booking.getVenueId());
        if (venue != null && booking.getExpectedAttendees() != null &&
            booking.getExpectedAttendees() > 0 &&
            venue.getCapacity() != null &&
            booking.getExpectedAttendees() > venue.getCapacity())
            return Result.fail("预计人数超出场地容量(" + venue.getCapacity() + ")");

        // 时段冲突校验
        long conflict = venueBookingMapper.selectCount(
            new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, booking.getVenueId())
                .eq(VenueBooking::getBookingDate, booking.getBookingDate())
                .eq(VenueBooking::getStatus, "APPROVED")
                .lt(VenueBooking::getStartTime, booking.getEndTime())
                .gt(VenueBooking::getEndTime, booking.getStartTime()));
        if (conflict > 0) return Result.fail("该时段已被占用");

        booking.setStatus("PENDING");
        venueBookingMapper.insert(booking);
        approvalService.createApprovalFlow("VENUE", booking.getId(), booking);
        notifyAdmins("新场地预约待审批",
            "用户预约了「" + (venue != null ? venue.getName() : "场地") +
            "」(" + booking.getBookingDate() + ")");
        return Result.ok(Map.of("bookingId", booking.getId(),
            "msg", "场地预约申请已提交"));
    }

    // ======================== 4. 物资借用(完整表单+批量+库存校验) ========================

    @PostMapping("/resource/borrow/full")
    public Result<?> borrowResourceFull(@RequestBody ResourceBorrow borrow) {
        Long userId = getCurrentUserId();
        borrow.setUserId(userId);

        if (borrow.getBatchItems() != null &&
            !borrow.getBatchItems().isEmpty()) {
            // 批量物资处理
            JSONArray items = JSON.parseArray(borrow.getBatchItems());
            for (int i = 0; i < items.size(); i++) {
                JSONObject it = items.getJSONObject(i);
                Long itemId = it.getLong("itemId");
                int qty = it.getIntValue("quantity");
                ResourceItem ri = resourceItemMapper.selectById(itemId);
                if (ri == null) return Result.fail("物资不存在(ID:" + itemId + ")");
                if (ri.getAvailable() < qty)
                    return Result.fail("「" + ri.getName() +
                        "」库存不足(可用:" + ri.getAvailable() + ")");
            }
            for (int i = 0; i < items.size(); i++) {
                JSONObject it = items.getJSONObject(i);
                ResourceItem ri = resourceItemMapper
                    .selectById(it.getLong("itemId"));
                ri.setAvailable(ri.getAvailable() -
                    it.getIntValue("quantity"));
                resourceItemMapper.updateById(ri);
            }
            if (!items.isEmpty()) {
                borrow.setItemId(items.getJSONObject(0).getLong("itemId"));
                borrow.setQuantity(
                    items.getJSONObject(0).getIntValue("quantity"));
            }
        } else {
            ResourceItem ri = resourceItemMapper
                .selectById(borrow.getItemId());
            if (ri == null) return Result.fail("物资不存在");
            if (ri.getAvailable() < borrow.getQuantity())
                return Result.fail("「" + ri.getName() +
                    "」库存不足(可用:" + ri.getAvailable() + ")");
            ri.setAvailable(ri.getAvailable() - borrow.getQuantity());
            resourceItemMapper.updateById(ri);
        }

        borrow.setStatus("PENDING");
        resourceBorrowMapper.insert(borrow);
        approvalService.createApprovalFlow("RESOURCE", borrow.getId(), borrow);
        notifyAdmins("新物资借用待审批", "用户申请借用物资");
        return Result.ok(Map.of("borrowId", borrow.getId(),
            "msg", "物资借用申请已提交"));
    }

    // ======================== 5. 经费申请(完整表单+合规校验) ========================

    @PostMapping("/fund/full")
    public Result<?> applyFundFull(@RequestBody FundRecord fund) {
        Long userId = getCurrentUserId();
        fund.setApplicantId(userId);
        fund.setStatus("PENDING");

        // 餐饮娱乐拦截
        if (fund.getCategory() != null &&
            (fund.getCategory().contains("餐饮") ||
             fund.getCategory().contains("娱乐")))
            return Result.fail("餐饮娱乐类开销不符合经费使用规定，申请已拦截");

        if (fund.getTotalAmount() == null && fund.getAmount() != null)
            fund.setTotalAmount(fund.getAmount());
        if (fund.getTotalAmount() != null &&
            fund.getTotalAmount().compareTo(new BigDecimal("5000")) >= 0)
            fund.setDescription(
                (fund.getDescription() != null ? fund.getDescription() + " " : "") +
                "【大额申请-需校级领导终审】");

        fundRecordMapper.insert(fund);
        approvalService.createApprovalFlow("FUND", fund.getId(), fund);
        notifyAdmins("新经费申请待审批",
            "社团提交经费申请 ¥" +
            (fund.getTotalAmount() != null ? fund.getTotalAmount() : "0"));
        return Result.ok(Map.of("fundId", fund.getId(),
            "msg", "经费申请已提交"));
    }

    // ======================== 统一审批操作 ========================

    @PutMapping("/approve")
    public Result<?> doApprove(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole()))
            return Result.fail(403, "权限不足");
        String appType = body.get("appType").toString();
        Long businessId = Long.valueOf(body.get("businessId").toString());
        String action = body.get("action").toString();
        String comment = String.valueOf(
            body.getOrDefault("comment", ""));

        if ("approve".equals(action)) {
            boolean finished = approvalService.approve(
                appType, businessId, getCurrentUserId(), comment);
            return Result.ok(Map.of("finished", finished,
                "msg", finished ? "全部审批完成" : "当前节点已通过，进入下一级"));
        } else {
            String reason = String.valueOf(
                body.getOrDefault("reason", comment));
            approvalService.reject(
                appType, businessId, getCurrentUserId(), reason);
            return Result.ok("已驳回");
        }
    }

    @GetMapping("/approval/timeline")
    public Result<?> getTimeline(
            @RequestParam String appType,
            @RequestParam Long businessId) {
        return Result.ok(
            approvalService.getApprovalTimeline(appType, businessId));
    }

    // ======================== 统一申请中心 ========================

    @GetMapping("/my-applications")
    public Result<?> myApplications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return Result.ok(
            approvalService.getMyApplications(getCurrentUserId(), type, status));
    }

    @GetMapping("/pending-approvals")
    public Result<?> pendingApprovals(
            @RequestParam(required = false) String type) {
        if (!"ADMIN".equals(getCurrentUserRole()))
            return Result.fail(403, "权限不足");
        return Result.ok(approvalService.getPendingApprovals(type));
    }

    // ======================== 草稿管理 ========================

    @PostMapping("/draft/save")
    public Result<?> saveDraft(@RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        String appType = body.get("appType");
        String formData = body.get("formData");
        int stepIndex = Integer.parseInt(
            body.getOrDefault("stepIndex", "0"));

        ApplicationDraft draft = draftMapper.selectOne(
            new LambdaQueryWrapper<ApplicationDraft>()
                .eq(ApplicationDraft::getUserId, userId)
                .eq(ApplicationDraft::getAppType, appType));
        if (draft == null) {
            draft = new ApplicationDraft();
            draft.setUserId(userId); draft.setAppType(appType);
            draft.setCreateTime(LocalDateTime.now());
        }
        draft.setFormData(formData); draft.setStepIndex(stepIndex);
        draft.setUpdateTime(LocalDateTime.now());
        if (draft.getId() == null) draftMapper.insert(draft);
        else draftMapper.updateById(draft);
        return Result.ok("草稿已保存");
    }

    @GetMapping("/draft/load")
    public Result<?> loadDraft(@RequestParam String appType) {
        ApplicationDraft draft = draftMapper.selectOne(
            new LambdaQueryWrapper<ApplicationDraft>()
                .eq(ApplicationDraft::getUserId, getCurrentUserId())
                .eq(ApplicationDraft::getAppType, appType));
        if (draft == null) return Result.ok(null);
        return Result.ok(Map.of(
            "stepIndex", draft.getStepIndex(),
            "formData", draft.getFormData()));
    }

    // ======================== 活动结项 ========================

    @PostMapping("/activity/{id}/closure")
    public Result<?> submitClosure(
            @PathVariable Long id, @RequestBody ActivityClosure closure) {
        Activity act = activityMapper.selectById(id);
        if (act == null) return Result.fail("活动不存在");
        if (!"FINISHED".equals(act.getStatus()))
            return Result.fail("活动未结束，无法结项");
        if (activityClosureMapper.selectCount(
            new LambdaQueryWrapper<ActivityClosure>()
                .eq(ActivityClosure::getActivityId, id)) > 0)
            return Result.fail("已提交过结项");

        closure.setActivityId(id);
        closure.setSubmittedBy(getCurrentUserId());
        closure.setStatus("PENDING");
        closure.setCreateTime(LocalDateTime.now());
        activityClosureMapper.insert(closure);
        notifyAdmins("活动结项待审批", "活动#" + id + " 提交了结项报告");
        return Result.ok("结项报告已提交");
    }

    @GetMapping("/activity/{id}/closure")
    public Result<?> getClosure(@PathVariable Long id) {
        return Result.ok(activityClosureMapper.selectOne(
            new LambdaQueryWrapper<ActivityClosure>()
                .eq(ActivityClosure::getActivityId, id)));
    }

    // ======================== 场地归还验收 ========================

    @Transactional
    @PostMapping("/venue/booking/{id}/return")
    public Result<?> returnVenue(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole()))
            return Result.fail(403, "权限不足");
        VenueBooking b = venueBookingMapper.selectById(id);
        if (b == null) return Result.fail("预约记录不存在");
        b.setStatus("RETURNED");
        b.setReturnNote(
            String.valueOf(body.getOrDefault("note", "")));
        venueBookingMapper.updateById(b);

        if ("true".equals(String.valueOf(
            body.getOrDefault("hasDamage", "false")))) {
            VenueDamageRecord dmg = new VenueDamageRecord();
            dmg.setBookingId(id); dmg.setVenueId(b.getVenueId());
            dmg.setDamageDesc(String.valueOf(
                body.getOrDefault("damageDesc", "")));
            dmg.setRepairCost(new BigDecimal(
                String.valueOf(body.getOrDefault("repairCost", "0"))));
            dmg.setHandlerId(getCurrentUserId());
            dmg.setStatus("PENDING_PAY");
            dmg.setCreateTime(LocalDateTime.now());
            venueDamageRecordMapper.insert(dmg);
        }
        return Result.ok("场地验收完成");
    }

    // ======================== 物资归还验收 ========================

    @Transactional
    @PostMapping("/resource/borrow/{id}/return-check")
    public Result<?> checkReturnResource(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole()))
            return Result.fail(403, "权限不足");
        ResourceBorrow b = resourceBorrowMapper.selectById(id);
        if (b == null) return Result.fail("借用记录不存在");
        if (!"APPROVED".equals(b.getStatus()) && !"BORROWING".equals(b.getStatus()))
            return Result.fail("该借用不可验收（当前状态：" + b.getStatus() + "）");

        b.setActualReturnDate(LocalDate.now());
        b.setReturnCondition(
            String.valueOf(body.getOrDefault("condition", "GOOD")));
        b.setStatus("RETURNED");
        resourceBorrowMapper.updateById(b);

        if (!"GOOD".equals(b.getReturnCondition())) {
            ResourceDamageRecord dmg = new ResourceDamageRecord();
            dmg.setBorrowId(id); dmg.setItemId(b.getItemId());
            dmg.setDamageDesc(String.valueOf(
                body.getOrDefault("damageDesc", "")));
            dmg.setRepairCost(new BigDecimal(
                String.valueOf(body.getOrDefault("repairCost", "0"))));
            dmg.setHandlerId(getCurrentUserId());
            dmg.setStatus("PENDING_PAY");
            dmg.setCreateTime(LocalDateTime.now());
            resourceDamageRecordMapper.insert(dmg);
        }

        // 恢复库存
        restoreInventory(b);
        return Result.ok("物资验收完成");
    }

    private void restoreInventory(ResourceBorrow b) {
        if (b.getBatchItems() != null &&
            !b.getBatchItems().isEmpty()) {
            JSONArray items = JSON.parseArray(b.getBatchItems());
            for (int i = 0; i < items.size(); i++) {
                JSONObject it = items.getJSONObject(i);
                ResourceItem ri = resourceItemMapper
                    .selectById(it.getLong("itemId"));
                if (ri != null) {
                    ri.setAvailable(
                        ri.getAvailable() + it.getIntValue("quantity"));
                    resourceItemMapper.updateById(ri);
                }
            }
        } else {
            ResourceItem ri = resourceItemMapper
                .selectById(b.getItemId());
            if (ri != null) {
                ri.setAvailable(
                    ri.getAvailable() + b.getQuantity());
                resourceItemMapper.updateById(ri);
            }
        }
    }

    // ======================== 经费核销 ========================

    @PostMapping("/fund/closure")
    public Result<?> closureFund(@RequestBody Map<String, Object> body) {
        if (body.get("fundId") == null) return Result.fail("缺少经费ID");
        Long fundId = Long.valueOf(body.get("fundId").toString());
        FundRecord fund = fundRecordMapper.selectById(fundId);
        if (fund == null) return Result.fail("经费记录不存在");
        if (!"APPROVED".equals(fund.getStatus()))
            return Result.fail("经费未审批通过，无法核销");
        // 仅管理员 / 申请人 / 社长可发起核销
        Long uid = getCurrentUserId();
        boolean isAdmin = "ADMIN".equals(getCurrentUserRole());
        boolean isApplicant = fund.getApplicantId() != null && fund.getApplicantId().equals(uid);
        Club fundClub = fund.getClubId() != null ? clubMapper.selectById(fund.getClubId()) : null;
        boolean isPresident = fundClub != null && uid != null && uid.equals(fundClub.getPresidentId());
        if (!isAdmin && !isApplicant && !isPresident) return Result.fail(403, "无权操作该经费核销");
        fund.setClosureStatus("PENDING");
        fundRecordMapper.updateById(fund);
        notifyAdmins("经费核销待审批",
            "社团提交经费核销申请(¥" + fund.getTotalAmount() + ")");
        return Result.ok("核销申请已提交");
    }

    /** 活动结项审批（管理员）— 避免结项永远停留 PENDING */
    @PutMapping("/activity/closure/{id}/approve")
    public Result<?> approveClosure(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        ActivityClosure closure = activityClosureMapper.selectById(id);
        if (closure == null) return Result.fail("结项记录不存在");
        if (!"PENDING".equals(closure.getStatus())) return Result.fail("该结项已处理");
        Object action = body != null ? body.get("action") : null;
        boolean ok = action == null || "approve".equals(action.toString());
        closure.setStatus(ok ? "APPROVED" : "REJECTED");
        closure.setUpdateTime(LocalDateTime.now());
        activityClosureMapper.updateById(closure);
        if (closure.getSubmittedBy() != null)
            notificationService.notify(closure.getSubmittedBy(), "活动结项审批结果",
                "您提交的活动结项已" + (ok ? "通过" : "驳回"), "SYSTEM");
        return Result.ok(ok ? "结项已通过" : "结项已驳回");
    }

    /** 经费核销完成（管理员）— 关闭核销闭环 */
    @PutMapping("/fund/closure/{fundId}/complete")
    public Result<?> completeFundClosure(@PathVariable Long fundId) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        FundRecord fund = fundRecordMapper.selectById(fundId);
        if (fund == null) return Result.fail("经费记录不存在");
        if (!"PENDING".equals(fund.getClosureStatus())) return Result.fail("该经费无待完成的核销");
        fund.setClosureStatus("COMPLETED");
        fundRecordMapper.updateById(fund);
        if (fund.getApplicantId() != null)
            notificationService.notify(fund.getApplicantId(), "经费核销完成",
                "您的经费核销已完成", "FUND_APPROVAL");
        return Result.ok("核销已完成");
    }

    // ======================== 模板说明 ========================

    @GetMapping("/template-info/{type}")
    public Result<?> getTemplateInfo(@PathVariable String type) {
        Map<String, String> tpl = Map.of(
            "charter", "社团章程模板：组织架构、会员管理、财务制度、活动安全规范",
            "activity_plan", "活动策划模板：活动概述、流程安排、资源需求、经费预算、应急预案",
            "budget", "经费预算明细表：开销项目名称、单价、数量、小计金额、详细用途说明"
        );
        return Result.ok(Map.of("type", type,
            "info", tpl.getOrDefault(type, "模板说明")));
    }

    // ======================== 工具方法 ========================

    private Long getCurrentUserId() {
        Object p = SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        return p instanceof Long ? (Long) p : null;
    }

    private String getCurrentUserRole() {
        var a = SecurityContextHolder.getContext()
            .getAuthentication();
        if (a != null && a.getAuthorities() != null &&
            !a.getAuthorities().isEmpty())
            return a.getAuthorities().iterator().next()
                .getAuthority();
        return null;
    }

    private void notifyAdmins(String title, String content) {
        List<User> admins = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getRole, "ADMIN"));
        notificationService.notifyBatch(
            admins.stream().map(User::getId).toList(),
            title, content, "PENDING_APPROVAL");
    }
}
