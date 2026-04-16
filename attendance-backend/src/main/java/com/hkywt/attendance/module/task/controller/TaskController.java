package com.hkywt.attendance.module.task.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.task.dto.TaskSubmitRequest;
import com.hkywt.attendance.module.task.service.TaskService;
import com.hkywt.attendance.module.task.vo.TaskDetailVO;
import com.hkywt.attendance.module.task.vo.TaskListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/my-pending")
    public ApiResult<List<TaskListVO>> myPending() {
        return ApiResult.success(taskService.listMyPendingTasks());
    }

    @GetMapping("/{taskId}/detail")
    public ApiResult<TaskDetailVO> detail(@PathVariable Long taskId) {
        return ApiResult.success(taskService.getTaskDetail(taskId));
    }

    @PostMapping("/submit")
    public ApiResult<Void> submit(@Valid @RequestBody TaskSubmitRequest request) {
        taskService.submitTask(request);
        return ApiResult.success();
    }
}
