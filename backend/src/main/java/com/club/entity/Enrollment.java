package com.club.entity;

import java.time.LocalDateTime;

//报名实体（对应 enrollment 表）
public class Enrollment {
    // 状态常量
    public static final int STATUS_PENDING  = 1;   // 待审
    public static final int STATUS_PASSED   = 2;   // 通过
    public static final int STATUS_REJECTED = 3;   // 拒绝

    private Long id;
    private Long userId;
    private Long clubId;
    private String reason;
    private Integer status;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
    private Long reviewerId;
    private String reviewRemark;

    // 仅 JOIN 查询时填充（DTO 字段，数据库表里没有）
    private String studentName;
    private String studentNo;
    public String getStudentNo(){return studentNo;}
    public void setStudentNo(String value){studentNo=value;}
    private String clubName;
    private String category;

    public Enrollment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewRemark() {
        return reviewRemark;
    }

    public void setReviewRemark(String reviewRemark) {
        this.reviewRemark = reviewRemark;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
