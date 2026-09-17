package com.hkywt.attendance.module.task.service;

import com.hkywt.attendance.module.task.dto.TaskSubmitRequest;
import com.hkywt.attendance.module.task.vo.TaskDetailVO;
import com.hkywt.attendance.module.task.vo.TaskListVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskService {
    List<TaskListVO> listMyPendingTasks();
    TaskDetailVO getTaskDetail(Long taskId);
    String uploadClassPhoto(Long taskId, MultipartFile file);
    Resource loadClassPhoto(String year, String month, String filename);
    void submitTask(TaskSubmitRequest request);
}
