package com.hkywt.attendance.module.plan.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.plan.dto.PlanSaveRequest;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlan;
import com.hkywt.attendance.module.plan.service.PlanService;
import com.hkywt.attendance.module.plan.vo.PlanDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ApiResult<Void> save(@Valid @RequestBody PlanSaveRequest request) {
        planService.save(request);
        return ApiResult.success();
    }

    @GetMapping
    public ApiResult<List<BizAttendancePlan>> list() {
        return ApiResult.success(planService.list());
    }

    @GetMapping("/{id}")
    public ApiResult<PlanDetailVO> detail(@PathVariable Long id) {
        return ApiResult.success(planService.getDetail(id));
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        planService.changeStatus(id, status);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ApiResult.success();
    }
}
