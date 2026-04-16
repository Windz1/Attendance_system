<template>
  <el-card class="filter-card">
    <template #header><span>筛选条件</span></template>
    <div class="filter-grid">
      <el-input v-model="filters.planName" placeholder="计划名称" clearable />
      <el-select v-model="filters.planType" placeholder="计划类型" clearable>
        <el-option :value="1" label="早自习" />
        <el-option :value="2" label="晚自习" />
        <el-option :value="3" label="其他" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option :value="1" label="启用" />
        <el-option :value="0" label="停用" />
      </el-select>
      <el-select v-model="filters.timezone" placeholder="时区" clearable>
        <el-option label="Asia/Shanghai" value="Asia/Shanghai" />
        <el-option label="Asia/Tokyo" value="Asia/Tokyo" />
        <el-option label="UTC" value="UTC" />
      </el-select>
      <el-select v-model="filters.dateMode" placeholder="日期模式" clearable>
        <el-option :value="1" label="固定日期" />
        <el-option :value="2" label="计划日期" />
      </el-select>
      <el-date-picker
        v-model="filters.effectiveRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="有效期开始"
        end-placeholder="有效期结束"
        range-separator="-"
      />
      <div class="filter-actions">
        <el-button type="primary" @click="applyFilters">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>
  </el-card>

  <el-card style="margin-top: 12px">
    <template #header>
      <div class="toolbar-head">
        <div>
          <el-button type="primary" @click="openDialog()">新增</el-button>
          <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="batchRemove">删除</el-button>
          <el-tag type="info" style="margin-left: 8px">共 {{ displayList.length }} 条</el-tag>
        </div>
        <el-button circle @click="load">⟳</el-button>
      </div>
    </template>

    <el-table :data="displayList" border row-key="id" @selection-change="onSelectChange">
      <el-table-column type="selection" width="46" />
      <el-table-column prop="planName" label="计划名称" min-width="160" />
      <el-table-column label="计划类型" width="110">
        <template #default="{ row }">
          <el-tag effect="plain">{{ planTypeMap[row.planType] || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            inline-prompt
            active-text="启用"
            inactive-text="停用"
            @change="(v) => toggleStatus(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="timezone" label="时区" min-width="120" />
      <el-table-column label="日期模式" width="100">
        <template #default="{ row }">
          <el-tag :type="row.dateMode === 1 ? 'warning' : 'primary'" effect="light">
            {{ row.dateMode === 1 ? '固定日期' : '计划日期' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用日期" min-width="170">
        <template #default="{ row }">
          <div v-if="row.dateMode === 2" class="weekday-wrap">
            <el-tag v-for="w in splitWeekdays(row.weekdays)" :key="`${row.id}-${w}`" effect="plain" size="small">{{ weekdayText(w) }}</el-tag>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="时段" min-width="170">
        <template #default="{ row }">{{ row.windowStartTime }} ~ {{ row.windowEndTime }}</template>
      </el-table-column>
      <el-table-column label="有效期" min-width="200">
        <template #default="{ row }">
          {{ row.effectiveStartDate || '-' }} ~ {{ row.effectiveEndDate || '长期' }}
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑计划' : '新增计划'" width="760px">
    <el-form :model="form" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="计划名称" required><el-input v-model="form.planName" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="计划类型" required>
            <el-select v-model="form.planType">
              <el-option :value="1" label="早自习" />
              <el-option :value="2" label="晚自习" />
              <el-option :value="3" label="其他" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" inline-prompt active-text="启用" inactive-text="停用" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="时区">
            <el-select v-model="form.timezone">
              <el-option label="Asia/Shanghai" value="Asia/Shanghai" />
              <el-option label="Asia/Tokyo" value="Asia/Tokyo" />
              <el-option label="UTC" value="UTC" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="开始时间" required><el-time-picker v-model="form.windowStartTime" value-format="HH:mm:ss" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="结束时间" required><el-time-picker v-model="form.windowEndTime" value-format="HH:mm:ss" /></el-form-item></el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="生效开始" required><el-date-picker v-model="form.effectiveStartDate" value-format="YYYY-MM-DD" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="生效结束"><el-date-picker v-model="form.effectiveEndDate" value-format="YYYY-MM-DD" /></el-form-item></el-col>
      </el-row>

      <el-form-item label="执行星期" required>
        <el-checkbox-group v-model="weekdaysArray">
          <el-checkbox :value="1">周一</el-checkbox>
          <el-checkbox :value="2">周二</el-checkbox>
          <el-checkbox :value="3">周三</el-checkbox>
          <el-checkbox :value="4">周四</el-checkbox>
          <el-checkbox :value="5">周五</el-checkbox>
          <el-checkbox :value="6">周六</el-checkbox>
          <el-checkbox :value="7">周日</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="适用班级" required>
        <el-select v-model="form.classIds" multiple filterable style="width:100%">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="允许改提交">
        <el-switch v-model="form.allowMemberModifyAfterSubmit" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listClassesApi } from '../../api/class'
import { changePlanStatusApi, deletePlanApi, getPlanDetailApi, listPlansApi, savePlanApi } from '../../api/plan'

const planTypeMap = { 1: '早自习', 2: '晚自习', 3: '其他' }

const list = ref([])
const classes = ref([])
const dialogVisible = ref(false)
const weekdaysArray = ref([])
const selectedIds = ref([])

const filters = reactive({
  planName: '',
  planType: undefined,
  status: undefined,
  timezone: '',
  dateMode: undefined,
  effectiveRange: []
})

const applied = ref({ ...filters })

const form = reactive({
  id: null,
  planName: '',
  planType: 1,
  status: 1,
  timezone: 'Asia/Shanghai',
  dateMode: 2,
  weekdays: '1,2,3,4,5',
  windowStartTime: '07:30:00',
  windowEndTime: '08:10:00',
  effectiveStartDate: '',
  effectiveEndDate: '',
  allowMemberModifyAfterSubmit: 0,
  remark: '',
  classIds: []
})

const displayList = computed(() => {
  const f = applied.value
  const [rangeStart, rangeEnd] = f.effectiveRange || []
  return (list.value || []).filter((row) => {
    if (f.planName && !(row.planName || '').includes(f.planName.trim())) return false
    if (f.planType !== undefined && row.planType !== f.planType) return false
    if (f.status !== undefined && row.status !== f.status) return false
    if (f.timezone && row.timezone !== f.timezone) return false
    if (f.dateMode !== undefined && row.dateMode !== f.dateMode) return false
    if (rangeStart) {
      const s = row.effectiveStartDate ? dayjs(row.effectiveStartDate) : null
      if (!s || s.isBefore(dayjs(rangeStart).startOf('day'))) return false
    }
    if (rangeEnd) {
      const e = row.effectiveEndDate ? dayjs(row.effectiveEndDate) : null
      if (e && e.isAfter(dayjs(rangeEnd).endOf('day'))) return false
    }
    return true
  })
})

const resetForm = () => {
  Object.assign(form, {
    id: null,
    planName: '',
    planType: 1,
    status: 1,
    timezone: 'Asia/Shanghai',
    dateMode: 2,
    weekdays: '1,2,3,4,5',
    windowStartTime: '07:30:00',
    windowEndTime: '08:10:00',
    effectiveStartDate: '',
    effectiveEndDate: '',
    allowMemberModifyAfterSubmit: 0,
    remark: '',
    classIds: []
  })
  weekdaysArray.value = [1, 2, 3, 4, 5]
}

const load = async () => {
  list.value = await listPlansApi()
}

const loadClasses = async () => {
  classes.value = await listClassesApi()
}

const applyFilters = () => {
  applied.value = JSON.parse(JSON.stringify(filters))
}

const resetFilters = () => {
  filters.planName = ''
  filters.planType = undefined
  filters.status = undefined
  filters.timezone = ''
  filters.dateMode = undefined
  filters.effectiveRange = []
  applyFilters()
}

const splitWeekdays = (weekdays) => (weekdays || '').split(',').filter(Boolean).map((v) => Number(v))
const weekdayText = (v) => ({ 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }[v] || '-')

const onSelectChange = (rows) => {
  selectedIds.value = rows.map((v) => v.id)
}

const openDialog = async (row) => {
  resetForm()
  if (row?.id) {
    const detail = await getPlanDetailApi(row.id)
    Object.assign(form, detail.plan, { classIds: detail.classIds || [] })
    weekdaysArray.value = splitWeekdays(form.weekdays)
  }
  dialogVisible.value = true
}

const submit = async () => {
  form.weekdays = [...weekdaysArray.value].sort((a, b) => a - b).join(',')
  await savePlanApi({ ...form })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

const toggleStatus = async (row, enabled) => {
  const snapshot = row.status
  row.status = enabled ? 1 : 0
  try {
    await changePlanStatusApi(row.id, row.status)
    ElMessage.success('状态已更新')
  } catch (e) {
    row.status = snapshot
    throw e
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除计划【${row.planName}】吗？`, '提示', { type: 'warning' })
  await deletePlanApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

const batchRemove = async () => {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认批量删除已选 ${selectedIds.value.length} 条计划吗？`, '提示', { type: 'warning' })
  await Promise.all(selectedIds.value.map((id) => deletePlanApi(id)))
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadClasses()])
  applyFilters()
})
</script>

<style scoped>
.filter-card :deep(.el-card__body) {
  padding-bottom: 14px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 10px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.weekday-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

@media (max-width: 1400px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(180px, 1fr));
  }
}
</style>
