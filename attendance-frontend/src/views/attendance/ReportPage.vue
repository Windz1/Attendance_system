<template>
  <el-card>
    <template #header><span>统计分析</span></template>

    <div class="toolbar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
      <el-select v-model="classId" clearable placeholder="按班级筛选" style="width: 220px">
        <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="load">刷新统计</el-button>
    </div>

    <el-empty v-if="chartRows.length === 0" description="当前筛选区间无统计数据" />

    <template v-else>
      <div class="summary-grid">
        <el-card shadow="never">
          <div class="summary-title">总记录数</div>
          <div class="summary-value">{{ totalCount }}</div>
        </el-card>
        <el-card shadow="never">
          <div class="summary-title">请假率</div>
          <div class="summary-value">{{ leaveRate }}%</div>
        </el-card>
        <el-card shadow="never">
          <div class="summary-title">迟到率</div>
          <div class="summary-value">{{ lateRate }}%</div>
        </el-card>
        <el-card shadow="never">
          <div class="summary-title">旷课率</div>
          <div class="summary-value">{{ absentRate }}%</div>
        </el-card>
      </div>

      <el-row :gutter="16" style="margin-top: 12px">
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>各班考勤总量（柱状图）</span></template>
            <div class="bar-chart">
              <div v-for="row in chartRows" :key="`total-${row.classId}`" class="bar-row">
                <div class="bar-label">{{ row.className }}</div>
                <div class="bar-track">
                  <div class="bar-fill total" :style="{ width: `${ratio(row.total, maxClassTotal)}%` }"></div>
                </div>
                <div class="bar-value">{{ row.total }}</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>考勤状态占比（环形图）</span></template>
            <div class="pie-wrap">
              <div class="pie" :style="{ background: pieGradient }">
                <div class="pie-inner">
                  <div class="pie-total">{{ totalCount }}</div>
                  <div class="pie-label">总数</div>
                </div>
              </div>
              <div class="legend">
                <div v-for="s in statusLegend" :key="s.code" class="legend-item">
                  <span class="dot" :style="{ background: s.color }"></span>
                  <span>{{ s.label }}</span>
                  <span class="legend-value">{{ totalByStatus[s.code] || 0 }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="chart-card" style="margin-top: 12px">
        <template #header><span>各班状态构成（堆叠图）</span></template>
        <div class="stack-chart">
          <div v-for="row in chartRows" :key="`stack-${row.classId}`" class="stack-row">
            <div class="stack-label">{{ row.className }}</div>
            <div class="stack-bar">
              <div
                v-for="s in statusLegend"
                :key="`${row.classId}-${s.code}`"
                class="stack-segment"
                :style="{ width: `${ratio(row[s.key], row.total)}%`, background: s.color }"
                :title="`${s.label}: ${row[s.key]}`"
              ></div>
            </div>
            <div class="stack-total">{{ row.total }}</div>
          </div>
        </div>
      </el-card>
    </template>
  </el-card>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { listClassesApi } from '../../api/class'
import { classStatusReportApi } from '../../api/report'

const classId = ref(undefined)
const classes = ref([])
const raw = ref([])
const dateRange = ref([
  dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])

const statusLegend = [
  { code: 1, key: 'present', label: '出勤', color: '#67C23A' },
  { code: 2, key: 'leave', label: '请假', color: '#E6A23C' },
  { code: 3, key: 'late', label: '迟到', color: '#409EFF' },
  { code: 4, key: 'absent', label: '旷课', color: '#F56C6C' }
]

const classNameMap = computed(() => {
  const map = {}
  classes.value.forEach((c) => { map[c.id] = c.className })
  return map
})

const chartRows = computed(() => {
  const bucket = {}
  raw.value.forEach((item) => {
    const id = item.classId
    if (!bucket[id]) {
      bucket[id] = {
        classId: id,
        className: classNameMap.value[id] || `班级ID:${id}`,
        present: 0,
        leave: 0,
        late: 0,
        absent: 0,
        total: 0
      }
    }
    const count = Number(item.count || 0)
    if (item.attendanceStatus === 1) bucket[id].present += count
    if (item.attendanceStatus === 2) bucket[id].leave += count
    if (item.attendanceStatus === 3) bucket[id].late += count
    if (item.attendanceStatus === 4) bucket[id].absent += count
    bucket[id].total += count
  })
  return Object.values(bucket).sort((a, b) => b.total - a.total)
})

const totalByStatus = computed(() => ({
  1: chartRows.value.reduce((s, v) => s + v.present, 0),
  2: chartRows.value.reduce((s, v) => s + v.leave, 0),
  3: chartRows.value.reduce((s, v) => s + v.late, 0),
  4: chartRows.value.reduce((s, v) => s + v.absent, 0)
}))

const totalCount = computed(() => Object.values(totalByStatus.value).reduce((s, v) => s + v, 0))
const maxClassTotal = computed(() => Math.max(...chartRows.value.map((v) => v.total), 1))

const leaveRate = computed(() => pct(totalByStatus.value[2], totalCount.value))
const lateRate = computed(() => pct(totalByStatus.value[3], totalCount.value))
const absentRate = computed(() => pct(totalByStatus.value[4], totalCount.value))

const pieGradient = computed(() => {
  const total = totalCount.value || 1
  const seq = [
    { value: totalByStatus.value[1], color: '#67C23A' },
    { value: totalByStatus.value[2], color: '#E6A23C' },
    { value: totalByStatus.value[3], color: '#409EFF' },
    { value: totalByStatus.value[4], color: '#F56C6C' }
  ]
  let start = 0
  const parts = seq.map((item) => {
    const end = start + (item.value / total) * 100
    const seg = `${item.color} ${start}% ${end}%`
    start = end
    return seg
  })
  return `conic-gradient(${parts.join(',')})`
})

const ratio = (val, base) => {
  if (!base) return 0
  return Math.max(2, Math.min(100, (Number(val || 0) / base) * 100))
}

const pct = (val, total) => {
  if (!total) return '0.0'
  return ((val / total) * 100).toFixed(1)
}

const load = async () => {
  const [startDate, endDate] = dateRange.value || []
  raw.value = await classStatusReportApi({
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    classId: classId.value || undefined
  })
}

onMounted(async () => {
  classes.value = await listClassesApi()
  await load()
})
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.summary-title {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.chart-card :deep(.el-card__header) {
  padding: 12px 16px;
}

.bar-row,
.stack-row {
  display: grid;
  grid-template-columns: 110px 1fr 56px;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.bar-label,
.stack-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-track,
.stack-bar {
  width: 100%;
  height: 16px;
  background: #eef2f7;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
}

.bar-fill.total {
  height: 100%;
  border-radius: 8px;
  background: linear-gradient(90deg, #409eff, #67c23a);
}

.bar-value,
.stack-total {
  text-align: right;
  color: #606266;
}

.pie-wrap {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 20px;
}

.pie {
  width: 220px;
  height: 220px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pie-inner {
  width: 132px;
  height: 132px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  box-shadow: inset 0 0 0 1px #f2f4f8;
}

.pie-total {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.pie-label {
  font-size: 12px;
  color: #909399;
}

.legend {
  min-width: 190px;
}

.legend-item {
  display: grid;
  grid-template-columns: 14px 1fr auto;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.legend-value {
  color: #606266;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
