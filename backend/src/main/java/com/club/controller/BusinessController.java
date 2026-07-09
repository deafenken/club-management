package com.club.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.club.common.Result;
import com.club.entity.*;
import com.club.enums.NotificationTypeEnum;
import com.club.mapper.*;
import com.club.service.SysNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BusinessController {

    private final ClubMapper clubMapper;
    private final ClubMemberMapper clubMemberMapper;
    private final ActivityMapper activityMapper;
    private final ActivityEnrollMapper enrollMapper;
    private final ActivityCheckinMapper checkinMapper;
    private final VenueMapper venueMapper;
    private final VenueBookingMapper bookingMapper;
    private final ResourceItemMapper itemMapper;
    private final ResourceBorrowMapper borrowMapper;
    private final FundRecordMapper fundMapper;
    private final SysAnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final SysNotificationService notificationService;

    // ====== 社团 ======
    @GetMapping("/clubs")
    public Result<?> listClubs(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        var w = new LambdaQueryWrapper<Club>();
        if (status != null) w.eq(Club::getStatus, status);
        else w.eq(Club::getStatus, 1); // 默认只返回已通过的社团
        if (keyword != null && !keyword.isEmpty()) w.like(Club::getName, keyword);
        Page<Club> pageResult = clubMapper.selectPage(new Page<>(page, size), w);

        // 补充社长姓名
        List<Map<String, Object>> records = new ArrayList<>();
        for (Club club : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", club.getId());
            item.put("name", club.getName());
            item.put("category", club.getCategory());
            item.put("description", club.getDescription());
            item.put("memberCount", club.getMemberCount());
            item.put("status", club.getStatus());
            item.put("presidentId", club.getPresidentId());
            item.put("createTime", club.getCreateTime());
            if (club.getPresidentId() != null) {
                User president = userMapper.selectById(club.getPresidentId());
                item.put("presidentName", president != null ? president.getRealName() : "未知");
            } else {
                item.put("presidentName", "待指定");
            }
            records.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        return Result.ok(result);
    }

    @PostMapping("/club")
    @Transactional
    public Result<?> createClub(@RequestBody Club club) {
        Long userId = getCurrentUserId();
        club.setPresidentId(userId);
        club.setStatus(0); club.setMemberCount(1);
        clubMapper.insert(club);
        // 创建者自动成为社长
        ClubMember president = new ClubMember();
        president.setClubId(club.getId()); president.setUserId(userId);
        president.setRole("PRESIDENT"); president.setStatus(1);
        president.setJoinTime(LocalDateTime.now());
        clubMemberMapper.insert(president);
        return Result.ok("社团创建成功，等待管理员审批");
    }

    @PutMapping("/club/approve")
    public Result<?> approveClub(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        Club club = clubMapper.selectById(Long.valueOf(body.get("id").toString()));
        club.setStatus(Integer.valueOf(body.get("status").toString()));
        clubMapper.updateById(club);
        // 通知社长审批结果
        String result = club.getStatus() == 1 ? "通过" : "驳回";
        notificationService.notify(club.getPresidentId(), "社团审批结果",
                "您的社团「" + club.getName() + "」已" + result, "CLUB_APPROVAL");
        return Result.ok("操作成功");
    }

    @PostMapping("/club/join")
    public Result<?> joinClub(@RequestBody Map<String, Object> body) {
        Long clubId = Long.valueOf(body.get("clubId").toString());
        Long userId = getCurrentUserId();
        if (clubMemberMapper.selectCount(new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getClubId, clubId).eq(ClubMember::getUserId, userId)) > 0)
            return Result.fail("已申请过该社团");
        ClubMember member = new ClubMember();
        member.setClubId(clubId); member.setUserId(userId);
        member.setRole("MEMBER"); member.setStatus(0);
        member.setJoinTime(LocalDateTime.now());
        clubMemberMapper.insert(member);
        // 通知社长有新成员申请
        Club club = clubMapper.selectById(clubId);
        User applicant = userMapper.selectById(userId);
        String applicantName = applicant != null ? applicant.getRealName() : "有人";
        notificationService.notify(club.getPresidentId(), "新成员申请",
                applicantName + " 申请加入您的社团「" + club.getName() + "」", "JOIN_REQUEST");
        return Result.ok("申请已提交，等待社长审批");
    }

    // ====== 社长管理 ======
    @GetMapping("/my-clubs")
    public Result<?> myClubs() {
        Long userId = getCurrentUserId();
        List<Club> clubs = clubMapper.selectList(
            new LambdaQueryWrapper<Club>().eq(Club::getPresidentId, userId));
        return Result.ok(clubs);
    }

    @GetMapping("/club/{clubId}/applications")
    public Result<?> getApplications(@PathVariable Long clubId) {
        Long userId = getCurrentUserId();
        Club club = clubMapper.selectById(clubId);
        if (club == null || !userId.equals(club.getPresidentId())) {
            return Result.fail(403, "仅社长可查看申请列表");
        }
        // 查询待审批的成员申请 (status=0)
        List<ClubMember> applications = clubMemberMapper.selectList(
            new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getClubId, clubId)
                .eq(ClubMember::getStatus, 0));
        // 补充申请人姓名
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClubMember cm : applications) {
            User applicant = userMapper.selectById(cm.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", cm.getId());
            item.put("clubId", cm.getClubId());
            item.put("userId", cm.getUserId());
            item.put("role", cm.getRole());
            item.put("status", cm.getStatus());
            item.put("joinTime", cm.getJoinTime());
            item.put("applicantName", applicant != null ? applicant.getRealName() : "未知用户");
            item.put("college", applicant != null ? applicant.getCollege() : "");
            result.add(item);
        }
        return Result.ok(result);
    }

    @PutMapping("/club/member/approve")
    public Result<?> approveMember(@RequestBody Map<String, Object> body) {
        Long memberId = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString()); // 1=通过, 2=拒绝
        ClubMember member = clubMemberMapper.selectById(memberId);
        if (member == null) return Result.fail("申请记录不存在");
        if (member.getStatus() != 0) return Result.fail("该申请已处理过");
        Club club = clubMapper.selectById(member.getClubId());
        Long userId = getCurrentUserId();
        if (!userId.equals(club.getPresidentId())) return Result.fail(403, "仅社长可审批");

        member.setStatus(status);
        clubMemberMapper.updateById(member);
        if (status == 1) {
            club.setMemberCount(club.getMemberCount() + 1);
            clubMapper.updateById(club);
        }
        // 通知申请人审批结果
        String result = status == 1 ? "通过" : "拒绝";
        notificationService.notify(member.getUserId(), "入社申请结果",
                "您申请加入「" + club.getName() + "」的申请已" + result, "MEMBER_APPROVAL");
        return Result.ok(status == 1 ? "已通过" : "已拒绝");
    }

    // ====== 活动 ======
    @GetMapping("/activities")
    public Result<?> listActivities(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String status) {
        var w = new LambdaQueryWrapper<Activity>();
        if (status != null && !status.isEmpty()) w.eq(Activity::getStatus, status);
        w.orderByDesc(Activity::getCreateTime);
        return Result.ok(activityMapper.selectPage(new Page<>(page, size), w));
    }

    @PostMapping("/activity")
    public Result<?> createActivity(@RequestBody Activity act) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"PRESIDENT".equals(role))
            return Result.fail(403, "仅管理员和社长可发布活动");
        // 验证社团存在且社长有权为该社团发布活动
        Club club = clubMapper.selectById(act.getClubId());
        if (club == null) return Result.fail("社团不存在");
        if ("PRESIDENT".equals(role) && !userId.equals(club.getPresidentId()))
            return Result.fail(403, "您不是该社团的社长");
        // 管理员直接通过，社长需审批
        act.setCreatedBy(userId);
        act.setStatus("ADMIN".equals(role) ? "APPROVED" : "PENDING");
        // 直接通过的活动立即生成签到码，否则永远无法签到
        if ("APPROVED".equals(act.getStatus())) act.setCheckinCode(RandomUtil.randomNumbers(6));
        act.setEnrolledCount(0); activityMapper.insert(act);
        // 社长创建活动时通知管理员审批
        if ("PENDING".equals(act.getStatus())) {
            notifyAdmins("新活动待审批", "社团「" + club.getName() + "」发布了新活动「" + act.getTitle() + "」，请审批", "ACTIVITY_PENDING");
        }
        return Result.ok("ADMIN".equals(role) ? "活动发布成功" : "活动已提交，等待管理员审批");
    }

    @PutMapping("/activity/approve")
    public Result<?> approveActivity(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        Activity act = activityMapper.selectById(Long.valueOf(body.get("id").toString()));
        boolean ok = "approve".equals(body.get("action"));
        act.setStatus(ok ? "APPROVED" : "REJECTED");
        if (ok) act.setCheckinCode(RandomUtil.randomNumbers(6));
        activityMapper.updateById(act);
        // 通知活动创建者
        String result = ok ? "通过" : "驳回";
        notificationService.notify(act.getCreatedBy(), "活动审批结果",
                "您的活动「" + act.getTitle() + "」已" + result, "ACTIVITY_APPROVAL");
        Map<String, Object> extra = new HashMap<>();
        extra.put("activityName", act.getTitle());
        extra.put("approved", ok);
        return Result.ok(ok ? Map.of("msg", "审批通过", "activityName", act.getTitle(), "suggestAnnounce", true) : "已驳回");
    }

    @PostMapping("/activity/enroll")
    @Transactional
    public Result<?> enroll(@RequestBody Map<String, Object> body) {
        Long aid = Long.valueOf(body.get("activityId").toString());
        Long uid = getCurrentUserId();
        Activity act = activityMapper.selectById(aid);
        if (act == null) return Result.fail("活动不存在");
        if (!"APPROVED".equals(act.getStatus())) return Result.fail("活动暂未开放报名");
        if (act.getMaxParticipants() != null && act.getMaxParticipants() > 0
                && act.getEnrolledCount() != null && act.getEnrolledCount() >= act.getMaxParticipants())
            return Result.fail("报名人数已满");
        if (enrollMapper.selectCount(new LambdaQueryWrapper<ActivityEnroll>()
                .eq(ActivityEnroll::getActivityId, aid).eq(ActivityEnroll::getUserId, uid)) > 0)
            return Result.fail("已报名");
        ActivityEnroll e = new ActivityEnroll(); e.setActivityId(aid); e.setUserId(uid);
        enrollMapper.insert(e);
        act.setEnrolledCount(act.getEnrolledCount() + 1); activityMapper.updateById(act);
        return Result.ok("报名成功");
    }

    @PostMapping("/activity/checkin")
    public Result<?> checkin(@RequestBody Map<String, Object> body) {
        Long aid = Long.valueOf(body.get("activityId").toString());
        Long uid = getCurrentUserId();
        Activity act = activityMapper.selectById(aid);
        if (act == null) return Result.fail("活动不存在");
        if (!"APPROVED".equals(act.getStatus()) && !"ONGOING".equals(act.getStatus()))
            return Result.fail("活动暂未开放签到");
        Object codeObj = body.get("code");
        if (codeObj == null) return Result.fail("请输入签到码");
        if (!codeObj.toString().equals(act.getCheckinCode())) return Result.fail("签到码错误");
        // 验证是否已报名
        if (enrollMapper.selectCount(new LambdaQueryWrapper<ActivityEnroll>()
                .eq(ActivityEnroll::getActivityId, aid).eq(ActivityEnroll::getUserId, uid)) == 0)
            return Result.fail("您未报名该活动");
        if (checkinMapper.selectCount(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, aid).eq(ActivityCheckin::getUserId, uid)) > 0)
            return Result.fail("已签到");
        ActivityCheckin ck = new ActivityCheckin(); ck.setActivityId(aid); ck.setUserId(uid);
        checkinMapper.insert(ck);
        // 更新报名状态为已签到
        ActivityEnroll enroll = enrollMapper.selectOne(new LambdaQueryWrapper<ActivityEnroll>()
                .eq(ActivityEnroll::getActivityId, aid).eq(ActivityEnroll::getUserId, uid));
        if (enroll != null) { enroll.setStatus("CHECKED_IN"); enrollMapper.updateById(enroll); }
        return Result.ok("签到成功");
    }

    // ====== 场地 ======
    @GetMapping("/venues")
    public Result<?> listVenues() {
        return Result.ok(venueMapper.selectList(null));
    }

    @PostMapping("/venue/booking")
    public Result<?> bookVenue(@RequestBody VenueBooking booking) {
        booking.setBookingUserId(getCurrentUserId()); // 强制使用当前登录用户
        long count = bookingMapper.selectCount(new LambdaQueryWrapper<VenueBooking>()
                .eq(VenueBooking::getVenueId, booking.getVenueId())
                .eq(VenueBooking::getBookingDate, booking.getBookingDate())
                .eq(VenueBooking::getStatus, "APPROVED")
                .lt(VenueBooking::getStartTime, booking.getEndTime())
                .gt(VenueBooking::getEndTime, booking.getStartTime()));
        if (count > 0) return Result.fail("该时段已被占用");
        if (booking.getStartTime() != null && booking.getEndTime() != null
                && !booking.getStartTime().isBefore(booking.getEndTime()))
            return Result.fail("结束时间必须晚于开始时间");
        booking.setStatus("PENDING"); bookingMapper.insert(booking);
        // 通知管理员有新场地预约
        Venue v = venueMapper.selectById(booking.getVenueId());
        String vn = v != null ? v.getName() : "场地";
        notifyAdmins("新场地预约待审批", "用户预约了「" + vn + "」(" + booking.getBookingDate() + ")，请审批", "VENUE_PENDING");
        return Result.ok("场地预约申请已提交");
    }

    @PutMapping("/venue/booking/approve")
    public Result<?> approveVenue(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        VenueBooking b = bookingMapper.selectById(Long.valueOf(body.get("id").toString()));
        if (b == null) return Result.fail("预约记录不存在");
        boolean approveVenue = Boolean.parseBoolean(body.get("approve").toString());
        // 批准前再次校验时段冲突，避免两个重叠预约都被批准（双重预订）
        if (approveVenue) {
            long conflict = bookingMapper.selectCount(new LambdaQueryWrapper<VenueBooking>()
                    .eq(VenueBooking::getVenueId, b.getVenueId())
                    .eq(VenueBooking::getBookingDate, b.getBookingDate())
                    .eq(VenueBooking::getStatus, "APPROVED")
                    .ne(VenueBooking::getId, b.getId())
                    .lt(VenueBooking::getStartTime, b.getEndTime())
                    .gt(VenueBooking::getEndTime, b.getStartTime()));
            if (conflict > 0) return Result.fail("该时段已有其他预约通过，无法重复批准");
        }
        b.setStatus(approveVenue ? "APPROVED" : "REJECTED");
        bookingMapper.updateById(b);
        // 通知预约人
        Venue venue = venueMapper.selectById(b.getVenueId());
        String venueName = venue != null ? venue.getName() : "场地";
        String result = "APPROVED".equals(b.getStatus()) ? "通过" : "驳回";
        notificationService.notify(b.getBookingUserId(), "场地预约结果",
                "您预约的「" + venueName + "」（" + b.getBookingDate() + "）已" + result, "VENUE_APPROVAL");
        return Result.ok("操作成功");
    }

    @GetMapping("/venue/bookings")
    public Result<?> listVenueBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        var w = new LambdaQueryWrapper<VenueBooking>();
        if (status != null && !status.isEmpty()) w.eq(VenueBooking::getStatus, status);
        // 角色数据隔离：ADMIN看全部，其他人只看自己的预约
        if (!"ADMIN".equals(getCurrentUserRole())) {
            w.eq(VenueBooking::getBookingUserId, getCurrentUserId());
        }
        w.orderByDesc(VenueBooking::getCreateTime);
        Page<VenueBooking> pageResult = bookingMapper.selectPage(new Page<>(page, size), w);
        // 补充场地名和预约人信息
        List<Map<String, Object>> records = new ArrayList<>();
        for (VenueBooking b : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("venueId", b.getVenueId());
            item.put("bookingDate", b.getBookingDate());
            item.put("startTime", b.getStartTime());
            item.put("endTime", b.getEndTime());
            item.put("purpose", b.getPurpose());
            item.put("status", b.getStatus());
            Venue v = venueMapper.selectById(b.getVenueId());
            item.put("venueName", v != null ? v.getName() : "未知");
            User u = userMapper.selectById(b.getBookingUserId());
            item.put("userName", u != null ? u.getRealName() : "未知");
            records.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        return Result.ok(result);
    }

    // ====== 物资 ======
    @GetMapping("/resources")
    public Result<?> listResources() {
        return Result.ok(itemMapper.selectList(null));
    }

    @PostMapping("/resource/borrow")
    @Transactional
    public Result<?> borrowResource(@RequestBody ResourceBorrow borrow) {
        borrow.setUserId(getCurrentUserId()); // 强制使用当前登录用户
        ResourceItem item = itemMapper.selectById(borrow.getItemId());
        if (item == null) return Result.fail("物资不存在");
        if (borrow.getQuantity() == null || borrow.getQuantity() <= 0) return Result.fail("借用数量无效");
        if (item.getAvailable() < borrow.getQuantity()) return Result.fail("库存不足");
        borrow.setStatus("PENDING"); borrowMapper.insert(borrow);
        item.setAvailable(item.getAvailable() - borrow.getQuantity()); itemMapper.updateById(item);
        // 通知管理员有新物资借用申请
        notifyAdmins("新物资借用待审批", "用户申请借用「" + item.getName() + "」×" + borrow.getQuantity() + "，请审批", "RESOURCE_PENDING");
        return Result.ok("借用申请已提交，等待管理员审批");
    }

    @GetMapping("/resource/borrows")
    public Result<?> listResourceBorrows(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        var w = new LambdaQueryWrapper<ResourceBorrow>();
        if (status != null && !status.isEmpty()) w.eq(ResourceBorrow::getStatus, status);
        // 角色数据隔离：ADMIN看全部，其他人只看自己的借用
        if (!"ADMIN".equals(getCurrentUserRole())) {
            w.eq(ResourceBorrow::getUserId, getCurrentUserId());
        }
        w.orderByDesc(ResourceBorrow::getBorrowDate);
        Page<ResourceBorrow> pageResult = borrowMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> records = new ArrayList<>();
        for (ResourceBorrow b : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", b.getId());
            item.put("itemId", b.getItemId());
            item.put("userId", b.getUserId());
            item.put("quantity", b.getQuantity());
            item.put("borrowDate", b.getBorrowDate());
            item.put("planReturnDate", b.getPlanReturnDate());
            item.put("status", b.getStatus());
            ResourceItem ri = itemMapper.selectById(b.getItemId());
            item.put("itemName", ri != null ? ri.getName() : "未知");
            User u = userMapper.selectById(b.getUserId());
            item.put("userName", u != null ? u.getRealName() : "未知");
            records.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        return Result.ok(result);
    }

    @PutMapping("/resource/borrow/approve")
    public Result<?> approveResourceBorrow(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        ResourceBorrow b = borrowMapper.selectById(Long.valueOf(body.get("id").toString()));
        if (!"PENDING".equals(b.getStatus())) return Result.fail("该申请已处理过");
        boolean approve = Boolean.parseBoolean(body.get("approve").toString());
        b.setStatus(approve ? "APPROVED" : "REJECTED");
        borrowMapper.updateById(b);
        if (!approve) {
            // 拒绝时归还库存
            ResourceItem item = itemMapper.selectById(b.getItemId());
            item.setAvailable(item.getAvailable() + b.getQuantity());
            itemMapper.updateById(item);
        }
        // 通知借用人
        ResourceItem ri = itemMapper.selectById(b.getItemId());
        String itemName = ri != null ? ri.getName() : "物资";
        String result = approve ? "通过" : "驳回";
        notificationService.notify(b.getUserId(), "物资借用结果",
                "您借用「" + itemName + "」×" + b.getQuantity() + "的申请已" + result, "RESOURCE_APPROVAL");
        return Result.ok(approve ? "借用已通过" : "已拒绝，库存已归还");
    }

    /** 归还物资：恢复库存 */
    @PutMapping("/resource/borrow/return")
    public Result<?> returnResource(@RequestBody Map<String, Object> body) {
        Long borrowId = Long.valueOf(body.get("id").toString());
        ResourceBorrow b = borrowMapper.selectById(borrowId);
        if (b == null) return Result.fail("借用记录不存在");
        if (b.getUserId() == null || (!b.getUserId().equals(getCurrentUserId())
                && !"ADMIN".equals(getCurrentUserRole())))
            return Result.fail(403, "无权归还他人借用");
        if (!"APPROVED".equals(b.getStatus()) && !"BORROWING".equals(b.getStatus()))
            return Result.fail("当前状态不允许归还");
        b.setStatus("RETURNED");
        b.setActualReturnDate(java.time.LocalDate.now());
        borrowMapper.updateById(b);
        // 恢复库存
        ResourceItem item = itemMapper.selectById(b.getItemId());
        item.setAvailable(item.getAvailable() + b.getQuantity());
        itemMapper.updateById(item);
        return Result.ok("归还成功，库存已恢复");
    }

    // ====== 经费 ======
    @GetMapping("/funds")
    public Result<?> listFunds(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        var w = new LambdaQueryWrapper<FundRecord>();
        if (status != null && !status.isEmpty()) w.eq(FundRecord::getStatus, status);
        // 角色数据隔离：ADMIN看全部，其他人只看自己管理的社团
        String role = getCurrentUserRole();
        if (!"ADMIN".equals(role)) {
            Long userId = getCurrentUserId();
            List<Long> myClubIds = clubMapper.selectList(
                new LambdaQueryWrapper<Club>().eq(Club::getPresidentId, userId))
                .stream().map(Club::getId).toList();
            if (myClubIds.isEmpty()) {
                // 学生可能只是成员不是社长，查成员表
                List<Long> memberClubIds = clubMemberMapper.selectList(
                    new LambdaQueryWrapper<ClubMember>()
                        .eq(ClubMember::getUserId, userId)
                        .eq(ClubMember::getStatus, 1))
                    .stream().map(ClubMember::getClubId).toList();
                myClubIds = memberClubIds;
            }
            if (myClubIds.isEmpty()) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("records", Collections.emptyList());
                emptyResult.put("total", 0L);
                return Result.ok(emptyResult);
            }
            w.in(FundRecord::getClubId, myClubIds);
        }
        w.orderByDesc(FundRecord::getCreateTime);
        Page<FundRecord> pageResult = fundMapper.selectPage(new Page<>(page, size), w);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return Result.ok(result);
    }
    @PostMapping("/fund")
    public Result<?> addFund(@RequestBody FundRecord fund) {
        String role = getCurrentUserRole();
        Long uid = getCurrentUserId();
        if (fund.getClubId() == null) return Result.fail("请选择社团");
        if (!"ADMIN".equals(role)) {
            Club club = clubMapper.selectById(fund.getClubId());
            if (club == null) return Result.fail("社团不存在");
            boolean isPresident = uid != null && uid.equals(club.getPresidentId());
            boolean isMember = clubMemberMapper.selectCount(new LambdaQueryWrapper<ClubMember>()
                    .eq(ClubMember::getClubId, fund.getClubId())
                    .eq(ClubMember::getUserId, uid)
                    .eq(ClubMember::getStatus, 1)) > 0;
            if (!isPresident && !isMember) return Result.fail(403, "仅本社团成员可提交经费申请");
        }
        fund.setApplicantId(uid);
        fund.setStatus("PENDING"); fundMapper.insert(fund);
        // 通知管理员有新经费申请
        Club fc = clubMapper.selectById(fund.getClubId());
        String fcn = fc != null ? fc.getName() : "社团";
        notifyAdmins("新经费申请待审批", "社团「" + fcn + "」提交了经费申请（¥" + fund.getAmount() + "），请审批", "FUND_PENDING");
        return Result.ok("经费申请已提交");
    }

    @PutMapping("/fund/approve")
    public Result<?> approveFund(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        FundRecord f = fundMapper.selectById(Long.valueOf(body.get("id").toString()));
        f.setStatus(Boolean.parseBoolean(body.get("approve").toString()) ? "APPROVED" : "REJECTED");
        fundMapper.updateById(f);
        // 通知社团社长
        Club fundClub = clubMapper.selectById(f.getClubId());
        String result = "APPROVED".equals(f.getStatus()) ? "通过" : "驳回";
        if (fundClub != null && fundClub.getPresidentId() != null) {
            notificationService.notify(fundClub.getPresidentId(), "经费申请结果",
                    "社团「" + fundClub.getName() + "」的经费申请（¥" + f.getAmount() + "）已" + result, "FUND_APPROVAL");
        }
        return Result.ok("操作成功");
    }

    // ====== 公告 ======
    @GetMapping("/announcements")
    public Result<?> listAnnouncements() {
        return Result.ok(announcementMapper.selectList(
            new LambdaQueryWrapper<SysAnnouncement>().orderByDesc(SysAnnouncement::getIsTop)
                .orderByDesc(SysAnnouncement::getCreateTime)));
    }

    @PostMapping("/announcement")
    public Result<?> createAnnouncement(@RequestBody SysAnnouncement announcement) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        announcement.setIsTop(0);
        announcement.setCreateTime(LocalDateTime.now());
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long) announcement.setCreatedBy((Long) principal);
        announcementMapper.insert(announcement);
        // 按目标范围推送：非 ALL 时仅推送对应角色
        String target = announcement.getTarget();
        List<User> recipients = (target == null || target.isEmpty() || "ALL".equals(target))
                ? userMapper.selectList(null)
                : userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, target));
        List<Long> userIds = recipients.stream().map(User::getId).toList();
        notificationService.notifyBatch(userIds, "新公告",
                "管理员发布了新公告：「" + announcement.getTitle() + "」", "ANNOUNCEMENT");
        return Result.ok("公告发布成功");
    }

    @PutMapping("/announcement/{id}/top")
    public Result<?> toggleAnnouncementTop(@PathVariable Long id) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        SysAnnouncement a = announcementMapper.selectById(id);
        if (a == null) return Result.fail("公告不存在");
        a.setIsTop(a.getIsTop() == 1 ? 0 : 1);
        announcementMapper.updateById(a);
        return Result.ok(a.getIsTop() == 1 ? "已置顶" : "已取消置顶");
    }

    // ====== 数据统计看板 ======
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("clubCount", clubMapper.selectCount(null));
        data.put("activityCount", activityMapper.selectCount(null));
        data.put("venueCount", venueMapper.selectCount(null));
        data.put("userCount", userMapper.selectCount(null));

        String role = getCurrentUserRole();
        Long userId = getCurrentUserId();

        // 角色过滤：ADMIN 看全部待审批，其他人看 0
        List<Map<String, Object>> pendingItems = new ArrayList<>();
        if ("ADMIN".equals(role)) {
            long pendingClubs = clubMapper.selectCount(new LambdaQueryWrapper<Club>().eq(Club::getStatus, 0));
            long pendingActivities = activityMapper.selectCount(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "PENDING"));
            long pendingVenues = bookingMapper.selectCount(new LambdaQueryWrapper<VenueBooking>().eq(VenueBooking::getStatus, "PENDING"));
            long pendingResources = borrowMapper.selectCount(new LambdaQueryWrapper<ResourceBorrow>().eq(ResourceBorrow::getStatus, "PENDING"));
            long pendingFunds = fundMapper.selectCount(new LambdaQueryWrapper<FundRecord>().eq(FundRecord::getStatus, "PENDING"));
            data.put("pendingApprovals", pendingClubs + pendingActivities + pendingVenues + pendingResources + pendingFunds);

            // 收集真实待办项（每类取最新3条）
            List<Club> pClubs = clubMapper.selectList(new LambdaQueryWrapper<Club>().eq(Club::getStatus, 0)
                .orderByDesc(Club::getCreateTime).last("LIMIT 3"));
            for (Club c : pClubs) {
                User applicant = userMapper.selectById(c.getPresidentId());
                pendingItems.add(Map.of(
                    "id", c.getId(), "appType", "CLUB",
                    "type", "社团审批", "name", c.getName(),
                    "applicant", "申请人: " + (applicant != null ? applicant.getRealName() : "未知"),
                    "time", timeAgo(c.getCreateTime()),
                    "color", "#fa8c16"
                ));
            }
            List<Activity> pActs = activityMapper.selectList(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus, "PENDING")
                .orderByDesc(Activity::getCreateTime).last("LIMIT 3"));
            for (Activity a : pActs) {
                Club club = clubMapper.selectById(a.getClubId());
                pendingItems.add(Map.of(
                    "id", a.getId(), "appType", "ACTIVITY",
                    "type", "活动审批", "name", a.getTitle(),
                    "applicant", "社团: " + (club != null ? club.getName() : "未知"),
                    "time", timeAgo(a.getCreateTime()),
                    "color", "#52c41a"
                ));
            }
            List<VenueBooking> pVenues = bookingMapper.selectList(new LambdaQueryWrapper<VenueBooking>().eq(VenueBooking::getStatus, "PENDING")
                .orderByDesc(VenueBooking::getCreateTime).last("LIMIT 3"));
            for (VenueBooking v : pVenues) {
                User applicant = userMapper.selectById(v.getBookingUserId());
                Venue venue = venueMapper.selectById(v.getVenueId());
                pendingItems.add(Map.of(
                    "id", v.getId(), "appType", "VENUE",
                    "type", "场地审批", "name", venue != null ? venue.getName() : "场地",
                    "applicant", "申请人: " + (applicant != null ? applicant.getRealName() : "未知"),
                    "time", timeAgo(v.getCreateTime()),
                    "color", "#1890ff"
                ));
            }
            List<ResourceBorrow> pRes = borrowMapper.selectList(new LambdaQueryWrapper<ResourceBorrow>().eq(ResourceBorrow::getStatus, "PENDING")
                .orderByDesc(ResourceBorrow::getCreateTime).last("LIMIT 3"));
            for (ResourceBorrow r : pRes) {
                User applicant = userMapper.selectById(r.getUserId());
                ResourceItem item = itemMapper.selectById(r.getItemId());
                pendingItems.add(Map.of(
                    "id", r.getId(), "appType", "RESOURCE",
                    "type", "物资审批", "name", item != null ? item.getName() : "物资",
                    "applicant", "申请人: " + (applicant != null ? applicant.getRealName() : "未知") + " x" + r.getQuantity(),
                    "time", timeAgo(r.getCreateTime()),
                    "color", "#597ef7"
                ));
            }
            List<FundRecord> pFunds = fundMapper.selectList(new LambdaQueryWrapper<FundRecord>().eq(FundRecord::getStatus, "PENDING")
                .orderByDesc(FundRecord::getCreateTime).last("LIMIT 3"));
            for (FundRecord f : pFunds) {
                Club club = clubMapper.selectById(f.getClubId());
                pendingItems.add(Map.of(
                    "id", f.getId(), "appType", "FUND",
                    "type", "经费审批", "name", club != null ? club.getName() : "经费",
                    "applicant", "¥" + (f.getAmount() != null ? f.getAmount().stripTrailingZeros().toPlainString() : "0") + " · " + (f.getCategory() != null ? f.getCategory() : ""),
                    "time", timeAgo(f.getCreateTime()),
                    "color", "#ff4d4f"
                ));
            }
        } else {
            data.put("pendingApprovals", 0);
        }
        data.put("pendingItems", pendingItems);

        data.put("activityByStatus", Arrays.asList(
            Map.of("name","草稿","value",activityMapper.selectCount(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus,"DRAFT"))),
            Map.of("name","待审批","value",activityMapper.selectCount(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus,"PENDING"))),
            Map.of("name","进行中","value",activityMapper.selectCount(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus,"ONGOING")))
        ));

        List<Map<String, Object>> rank = new ArrayList<>();
        for (Club c : clubMapper.selectList(new LambdaQueryWrapper<Club>().eq(Club::getStatus, 1))) {
            long cnt = activityMapper.selectCount(new LambdaQueryWrapper<Activity>().eq(Activity::getClubId, c.getId()));
            rank.add(Map.of("name", c.getName(), "activityCount", cnt, "memberCount", c.getMemberCount()));
        }
        rank.sort((a, b) -> Long.compare((long) b.get("activityCount"), (long) a.get("activityCount")));
        data.put("clubRank", rank.subList(0, Math.min(10, rank.size())));
        return Result.ok(data);
    }

    /** 将时间转为 "X小时前" / "X天前" 等友好格式 */
    private String timeAgo(LocalDateTime t) {
        if (t == null) return "";
        long mins = java.time.Duration.between(t, LocalDateTime.now()).toMinutes();
        if (mins < 1) return "刚刚";
        if (mins < 60) return mins + "分钟前";
        if (mins < 1440) return (mins / 60) + "小时前";
        if (mins < 43200) return (mins / 1440) + "天前";
        return (mins / 43200) + "个月前";
    }

    // ====== 用户管理 ======
    @GetMapping("/users")
    public Result<?> listUsers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        var result = userMapper.selectPage(new Page<>(page, size), null);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(result);
    }

    /** 管理员切换用户启用/禁用状态 */
    @PutMapping("/user/status")
    public Result<?> toggleUserStatus(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        Long userId = Long.valueOf(body.get("userId").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");
        if ("ADMIN".equals(user.getRole()) && status == 0)
            return Result.fail("不能禁用管理员账号");
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.ok(status == 1 ? "用户已启用" : "用户已禁用");
    }

    /** 设为管理员（拉管理员）：任意管理员均可将普通用户提升为管理员 */
    @PutMapping("/user/promote")
    public Result<?> promoteUser(@RequestBody Map<String, Object> body) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        if (body.get("userId") == null) return Result.fail("缺少用户ID");
        Long userId = Long.valueOf(body.get("userId").toString());
        User target = userMapper.selectById(userId);
        if (target == null) return Result.fail("用户不存在");
        if ("ADMIN".equals(target.getRole())) return Result.fail("该用户已是管理员");
        target.setRole("ADMIN");
        userMapper.updateById(target);
        notificationService.notify(userId, "权限变更", "您已被设置为系统管理员", "SYSTEM");
        return Result.ok("已将「" + target.getRealName() + "」设为管理员");
    }

    /** 移除管理员：仅总管理员（群主）可操作；不能移除总管理员本身 */
    @PutMapping("/user/demote")
    public Result<?> demoteUser(@RequestBody Map<String, Object> body) {
        Long uid = getCurrentUserId();
        User current = uid != null ? userMapper.selectById(uid) : null;
        boolean isSuper = current != null && current.getIsSuper() != null && current.getIsSuper() == 1;
        if (!isSuper) return Result.fail(403, "仅总管理员可移除管理员");
        if (body.get("userId") == null) return Result.fail("缺少用户ID");
        Long userId = Long.valueOf(body.get("userId").toString());
        User target = userMapper.selectById(userId);
        if (target == null) return Result.fail("用户不存在");
        if (target.getIsSuper() != null && target.getIsSuper() == 1) return Result.fail("不能移除总管理员");
        if (!"ADMIN".equals(target.getRole())) return Result.fail("该用户不是管理员");
        target.setRole("STUDENT");
        userMapper.updateById(target);
        notificationService.notify(userId, "权限变更", "您的管理员权限已被移除", "SYSTEM");
        return Result.ok("已移除「" + target.getRealName() + "」的管理员权限");
    }

    // ====== 通知 ======
    /** 获取通知列表（分页+类型筛选，pageSize上限200） */
    @GetMapping("/notifications")
    public Result<?> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ALL") String filterType) {
        Long userId = getCurrentUserId();
        int safeSize = Math.min(size, 200);
        List<SysNotification> list = notificationService.listByUser(userId, page, safeSize, filterType);
        int total = notificationService.countByUser(userId, filterType);
        return Result.ok(Map.of("records", list, "total", total));
    }

    /** 未读数量 */
    @GetMapping("/notification/unread-count")
    public Result<?> unreadCount() {
        return Result.ok(Map.of("count", notificationService.unreadCount(getCurrentUserId())));
    }

    /** 标记单条已读（含越权校验） */
    @PutMapping("/notification/read/{id}")
    public Result<?> markRead(@PathVariable Long id) {
        notificationService.markRead(id, getCurrentUserId());
        return Result.ok("已读");
    }

    /** 批量已读（按ID列表，单次最大200条） */
    @PutMapping("/notification/read-batch")
    public Result<?> markReadBatch(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.get("ids");
        if (rawIds == null || rawIds.size() > 200) return Result.fail("单次最多200条");
        List<Long> ids = rawIds.stream().map(Number::longValue).toList();
        notificationService.markReadBatch(ids, userId);
        return Result.ok("已标记");
    }

    /** 全部已读 */
    @PutMapping("/notification/read-all")
    public Result<?> markAllRead() {
        int count = notificationService.markAllRead(getCurrentUserId());
        return Result.ok(Map.of("count", count, "msg", "全部已读"));
    }

    /** 一键清空当前用户所有通知（逻辑删除） */
    @DeleteMapping("/notification/clear-all")
    public Result<?> clearAll() {
        int count = notificationService.clearAll(getCurrentUserId());
        return Result.ok(Map.of("count", count, "msg", "已清空" + count + "条通知"));
    }

    /** 撤回通知（管理员） */
    @PutMapping("/notification/revoke/{id}")
    public Result<?> revokeNotification(@PathVariable Long id) {
        if (!"ADMIN".equals(getCurrentUserRole())) return Result.fail(403, "权限不足");
        boolean ok = notificationService.revoke(id);
        return ok ? Result.ok("已撤回") : Result.fail("撤回失败，通知不存在或已删除");
    }

    /** 通知类型枚举列表（前端下拉用） */
    @GetMapping("/notification/types")
    public Result<?> notificationTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        for (NotificationTypeEnum e : NotificationTypeEnum.values()) {
            types.add(Map.of("code", e.getCode(), "label", e.getLabel(), "category", e.getCategory()));
        }
        return Result.ok(types);
    }

    // ====== 通知偏好 ======
    /** 获取当前用户通知偏好 */
    @GetMapping("/notification/preferences")
    public Result<?> getPreferences() {
        Long userId = getCurrentUserId();
        List<SysNotificationPreference> prefs = notificationService.getPreferences(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysNotificationPreference p : prefs) {
            NotificationTypeEnum typeEnum = NotificationTypeEnum.fromCode(p.getPreferType());
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("typeCode", p.getPreferType());
            item.put("typeLabel", typeEnum.getLabel());
            item.put("category", typeEnum.getCategory());
            item.put("enabled", p.getEnabled() == 1);
            result.add(item);
        }
        return Result.ok(result);
    }

    /** 更新单条偏好 */
    @PutMapping("/notification/preference")
    public Result<?> updatePreference(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        String typeCode = body.get("typeCode").toString();
        if (!NotificationTypeEnum.isValid(typeCode)) return Result.fail("无效的通知类型");
        boolean enabled = Boolean.parseBoolean(body.get("enabled").toString());
        notificationService.updatePreference(userId, typeCode, enabled);
        return Result.ok("偏好已更新");
    }

    // ====== 工具方法 ======
    /** 通知所有管理员 */
    private void notifyAdmins(String title, String content, String type) {
        List<User> admins = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
        List<Long> adminIds = admins.stream().map(User::getId).toList();
        notificationService.notifyBatch(adminIds, title, content, type);
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Long ? (Long) principal : null;
    }

    private String getCurrentUserRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }
}
