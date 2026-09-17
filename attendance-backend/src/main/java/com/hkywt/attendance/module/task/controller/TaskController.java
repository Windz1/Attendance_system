package com.hkywt.attendance.module.task.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.task.dto.TaskSubmitRequest;
import com.hkywt.attendance.module.task.service.TaskService;
import com.hkywt.attendance.module.task.vo.TaskDetailVO;
import com.hkywt.attendance.module.task.vo.TaskListVO;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
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

    @PostMapping("/class-photo/upload")
    public ApiResult<String> uploadClassPhoto(@RequestParam("taskId") Long taskId,
                                              @RequestParam("file") MultipartFile file) {
        return ApiResult.success(taskService.uploadClassPhoto(taskId, file));
    }

    @GetMapping("/class-photo/{year}/{month}/{filename:.+}")
    public ResponseEntity<Resource> classPhoto(@PathVariable String year,
                                               @PathVariable String month,
                                               @PathVariable String filename) {
        Resource resource = taskService.loadClassPhoto(year, month, filename);
        String mediaTypeValue = URLConnection.guessContentTypeFromName(filename);
        MediaType mediaType = mediaTypeValue == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(mediaTypeValue);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }

    @PostMapping("/submit")
    public ApiResult<Void> submit(@Valid @RequestBody TaskSubmitRequest request) {
        taskService.submitTask(request);
        return ApiResult.success();
    }
}
