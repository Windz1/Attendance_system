<template>
  <el-row :gutter="16">
    <el-col :span="7">
      <el-alert
        title="系统每天 03:10 自动清理 1 个月前的考勤记录。"
        type="warning"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-card>
        <template #header>
          <div class="record-filter-header">
            <span>筛选条件</span>
            <el-button link @click="reset">清空</el-button>
          </div>
        </template>

        <el-form label-position="top">
          <el-form-item label="统计区间">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="点名计划">
            <el-select v-model="filters.planId" clearable filterable placeholder="请选择需要导出的计划" style="width: 100%">
              <el-option v-for="p in plans" :key="p.id" :label="p.planName" :value="p.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="计划标题关键词">
            <el-input v-model="filters.keyword" clearable placeholder="支持模糊匹配，如“早自习”" />
          </el-form-item>

          <el-form-item label="班级">
            <el-select v-model="filters.classId" clearable filterable placeholder="按班级筛选" style="width: 100%">
              <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="考勤状态">
            <el-select v-model="filters.status" clearable placeholder="全部" style="width: 100%">
              <el-option label="出勤" :value="1" />
              <el-option label="请假" :value="2" />
              <el-option label="迟到" :value="3" />
              <el-option label="旷课" :value="4" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="filters.leaveOnly">仅显示请假学生</el-checkbox>
          </el-form-item>

          <el-form-item label="排序字段">
            <el-select v-model="filters.sortField" style="width: 100%">
              <el-option label="提交时间" value="submittedAt" />
              <el-option label="窗口开始" value="windowStartAt" />
              <el-option label="窗口结束" value="windowEndAt" />
              <el-option label="学生姓名" value="studentName" />
              <el-option label="学号" value="studentNo" />
              <el-option label="考勤状态" value="attendanceStatus" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-radio-group v-model="filters.sortOrder">
              <el-radio-button label="desc">降序</el-radio-button>
              <el-radio-button label="asc">升序</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" style="width: 100%" @click="load">应用筛选</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :span="17">
      <el-card>
        <template #header>
          <div class="record-toolbar">
            <div>
              <el-button type="warning" @click="onExport">导出筛选结果</el-button>
              <el-tag style="margin-left: 8px" type="info">匹配 {{ list.length }} 条记录</el-tag>
            </div>
            <el-button @click="load">刷新</el-button>
          </div>
        </template>
        <el-table :data="list" border height="680">
          <el-table-column prop="planName" label="计划名称" min-width="160" />
          <el-table-column prop="className" label="班级" min-width="120" />
          <el-table-column prop="windowStartAt" label="窗口开始" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.windowStartAt) }}</template>
          </el-table-column>
          <el-table-column prop="windowEndAt" label="窗口结束" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.windowEndAt) }}</template>
          </el-table-column>
          <el-table-column prop="submittedAt" label="提交时间" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
          </el-table-column>
          <el-table-column prop="studentName" label="学生姓名" min-width="110" />
          <el-table-column prop="studentNo" label="学号" min-width="120" />
          <el-table-column prop="attendanceStatusText" label="考勤状态" min-width="100" />
          <el-table-column prop="leaveStatus" label="请假状态" min-width="100" />
          <el-table-column prop="operatorName" label="点名员" min-width="100" />
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'
import { listClassesApi } from '../../api/class'
import { listPlansApi } from '../../api/plan'
import { exportRecordsApi, listRecordsApi } from '../../api/record'

const list = ref([])
const plans = ref([])
const classes = ref([])
const timeRange = ref([
  dayjs().subtract(6, 'day').startOf('day').format('YYYY-MM-DD HH:mm:ss'),
  dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')
])

const filters = reactive({
  planId: undefined,
  keyword: '',
  classId: undefined,
  status: undefined,
  leaveOnly: false,
  sortField: 'submittedAt',
  sortOrder: 'desc'
})

const buildParams = () => {
  const [startTime, endTime] = timeRange.value || []
  return {
    startTime: startTime || undefined,
    endTime: endTime || undefined,
    planId: filters.planId || undefined,
    keyword: filters.keyword || undefined,
    classId: filters.classId || undefined,
    status: filters.status || undefined,
    leaveOnly: filters.leaveOnly || undefined,
    sortField: filters.sortField,
    sortOrder: filters.sortOrder
  }
}

const formatDateTime = (val) => {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

const load = async () => {
  list.value = await listRecordsApi(buildParams())
}

const onExport = async () => {
  const blob = await exportRecordsApi(buildParams())
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'attendance_records.xlsx'
  a.click()
  window.URL.revokeObjectURL(url)
}

const reset = async () => {
  timeRange.value = [
    dayjs().subtract(6, 'day').startOf('day').format('YYYY-MM-DD HH:mm:ss'),
    dayjs().endOf('day').format('YYYY-MM-DD HH:mm:ss')
  ]
  filters.planId = undefined
  filters.keyword = ''
  filters.classId = undefined
  filters.status = undefined
  filters.leaveOnly = false
  filters.sortField = 'submittedAt'
  filters.sortOrder = 'desc'
  await load()
}

onMounted(async () => {
  const [planList, classList] = await Promise.all([listPlansApi(), listClassesApi()])
  plans.value = planList || []
  classes.value = classList || []
  await load()
})
</script>

<style scoped>
.record-filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
