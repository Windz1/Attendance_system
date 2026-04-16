package com.hkywt.attendance.module.task.service;

import com.hkywt.attendance.module.task.dto.TaskSubmitRequest;
import com.hkywt.attendance.module.task.vo.TaskDetailVO;
import com.hkywt.attendance.module.task.vo.TaskListVO;

import java.util.List;

public interface TaskService {
    List<TaskListVO> listMyPendingTasks();
    TaskDetailVO getTaskDetail(Long taskId);
    void submitTask(TaskSubmitRequest request);
}
