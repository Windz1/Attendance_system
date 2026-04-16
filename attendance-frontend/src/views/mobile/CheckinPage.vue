<template>
  <div class="m-page">
    <div class="checkin-header">
      <el-button text class="back-btn" @click="goBack">‹ 返回</el-button>
      <div>
        <div class="m-header" style="padding: 0">点名页面</div>
        <div class="header-sub">课堂签到 · 移动端</div>
      </div>
    </div>
    <el-card class="m-card" v-if="task">
      <div class="task-top-row">
        <div class="class-title">{{ task.className || `ID:${task.classId}` }}</div>
        <el-tag v-if="task?.taskStatus === 3" type="success" size="small">已提交</el-tag>
      </div>
      <div class="task-date">日期：{{ task.taskDate }}</div>
      <div class="status-summary">
        <span>出勤 {{ presentCount }}</span>
        <span>请假 {{ leaveCount }}</span>
        <span>迟到 {{ lateCount }}</span>
        <span>旷课 {{ absentCount }}</span>
      </div>
    </el-card>

    <el-card class="m-card">
      <div class="toolbar quick-actions">
        <el-button size="small" class="btn-present" @click="batchSet(1)">全设出勤</el-button>
        <el-button size="small" class="btn-leave" @click="batchSet(2)">全设请假</el-button>
        <el-button size="small" class="btn-absent" @click="batchSet(4)">全设旷课</el-button>
      </div>
      <el-empty v-if="records.length === 0" description="该班级暂无学生，请联系部长先导入学生信息" />
      <el-table :data="records" size="small" stripe class="checkin-table">
        <el-table-column label="学生">
          <template #default="{ row }">
            <div class="stu-name">{{ row.studentName }}</div>
            <div class="stu-no">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-select v-model="row.attendanceStatus" size="small" style="width:100px">
              <el-option :value="1" label="出勤" />
              <el-option :value="2" label="请假" />
              <el-option :value="3" label="迟到" />
              <el-option :value="4" label="旷课" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <el-button type="primary" class="submit-btn" :disabled="records.length===0 || submitDisabled" @click="submit">
        {{ task?.taskStatus===3 ? (canModifyAfterSubmit ? '重新提交' : '任务已提交') : '提交点名' }}
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitTaskApi, taskDetailApi } from '../../api/task'
import { myClassesApi } from '../../api/class'

const route = useRoute()
const router = useRouter()
const task = ref(null)
const records = ref([])
const canModifyAfterSubmit = ref(false)
const submitDisabled = ref(false)
const presentCount = computed(() => records.value.filter((v) => v.attendanceStatus === 1).length)
const leaveCount = computed(() => records.value.filter((v) => v.attendanceStatus === 2).length)
const lateCount = computed(() => records.value.filter((v) => v.attendanceStatus === 3).length)
const absentCount = computed(() => records.value.filter((v) => v.attendanceStatus === 4).length)

const loadDetail = async () => {
  const taskId = Number(route.params.taskId)
  if (!taskId) {
    ElMessage.error('任务ID无效，请返回重试')
    router.push('/m/tasks')
    return
  }
  const data = await taskDetailApi(taskId)
  let classNameMap = new Map()
  try {
    const myClasses = await myClassesApi()
    classNameMap = new Map((myClasses || []).map((item) => [item.id, item.className]))
  } catch (_) {
    // 班级接口异常时，详情仍可加载
  }
  task.value = data
  task.value.className = classNameMap.get(data.classId) || ''
  canModifyAfterSubmit.value = task.value?.taskStatus === 3
  submitDisabled.value = task.value?.taskStatus === 3 && !canModifyAfterSubmit.value
  records.value = (data.students || []).map((s) => ({
    studentId: s.studentId,
    studentNo: s.studentNo,
    studentName: s.studentName,
    attendanceStatus: s.attendanceStatus || 1,
    remark: s.remark || ''
  }))
}

const batchSet = (status) => {
  records.value = records.value.map((v) => ({ ...v, attendanceStatus: status }))
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/m/tasks')
}

const submit = async () => {
  await submitTaskApi({ taskId: Number(route.params.taskId), records: records.value.map(({ studentId, attendanceStatus, remark }) => ({ studentId, attendanceStatus, remark })) })
  ElMessage.success('提交成功')
  router.push('/m/tasks')
}

onMounted(loadDetail)
</script>

<style scoped>
.checkin-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.back-btn {
  padding: 0 4px;
  font-size: 14px;
  color: #4a5f86;
}

.class-title {
  font-size: 22px;
  line-height: 1.25;
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}

.header-sub {
  margin-top: -6px;
  font-size: 12px;
  color: #8292ab;
}

.task-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-date {
  margin-top: 2px;
  font-size: 13px;
  color: #61708a;
}

.status-summary {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-summary span {
  font-size: 12px;
  color: #60708a;
  background: #f0f5ff;
  border: 1px solid #dce7ff;
  border-radius: 999px;
  padding: 3px 9px;
}

.quick-actions {
  gap: 8px;
}

.btn-present {
  border-color: #70c56b;
  color: #2f7f2b;
  background: #f2fff0;
}

.btn-leave {
  border-color: #e4b34b;
  color: #9c6a00;
  background: #fff9ea;
}

.btn-absent {
  border-color: #ee8f8f;
  color: #b23030;
  background: #fff2f2;
}

.stu-name {
  font-weight: 600;
  color: #2e3b52;
}

.stu-no {
  margin-top: 2px;
  font-size: 12px;
  color: #8d9ab0;
}

.submit-btn {
  width: 100%;
  margin-top: 12px;
  height: 42px;
  font-size: 15px;
  letter-spacing: 0.2px;
}
</style>
