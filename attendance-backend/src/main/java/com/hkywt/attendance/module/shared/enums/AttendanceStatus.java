package com.hkywt.attendance.module.shared.enums;

import lombok.Getter;

@Getter
public enum AttendanceStatus {
    PRESENT(1, "出勤"),
    LEAVE(2, "请假"),
    LATE(3, "迟到"),
    ABSENT(4, "旷课");

    private final int code;
    private final String desc;

    AttendanceStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
