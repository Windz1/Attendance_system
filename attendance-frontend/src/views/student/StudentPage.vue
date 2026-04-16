<template>
  <el-row :gutter="16">
    <el-col :span="5">
      <el-card class="left-card">
        <el-input v-model="classKeyword" placeholder="请输入班级名称" clearable>
          <template #prefix>🔍</template>
        </el-input>
        <div class="class-list">
          <div class="class-item" :class="{ active: filters.classId === undefined }" @click="pickClass(undefined)">
            全部班级
          </div>
          <div
            v-for="c in filteredClasses"
            :key="c.id"
            class="class-item"
            :class="{ active: filters.classId === c.id }"
            @click="pickClass(c.id)"
          >
            {{ c.className }}
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="19">
      <el-card>
        <el-alert
          :title="retentionHint"
          type="warning"
          :closable="false"
          style="margin-bottom: 10px"
        />
        <div class="search-grid">
          <el-input v-model="filters.studentNo" placeholder="请输入学号" clearable />
          <el-input v-model="filters.studentName" placeholder="请输入学生姓名" clearable />
          <el-input v-model="filters.phone" placeholder="请输入手机号" clearable />
          <el-select v-model="filters.status" placeholder="学生状态" clearable>
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
          <el-date-picker
            v-model="filters.createRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
          <div class="search-actions">
            <el-button type="primary" @click="load">搜索</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </el-card>

      <el-card style="margin-top: 12px">
        <template #header>
          <div class="toolbar-header">
            <div>
              <el-button type="primary" @click="openDialog()">新增学生</el-button>
              <el-upload :show-file-list="false" :http-request="uploadFile" style="display:inline-block;margin-left:8px">
                <el-button type="success">导入xlsx</el-button>
              </el-upload>
              <el-button type="danger" plain style="margin-left:8px" @click="openTurnoverDialog">批量删除</el-button>
              <el-select v-model="importMode" style="width:160px;margin-left:8px">
                <el-option :value="1" label="导入跳过重复" />
                <el-option :value="2" label="导入覆盖更新" />
              </el-select>
            </div>
            <div>
              <el-button circle @click="load">⟳</el-button>
            </div>
          </div>
        </template>

        <el-table :data="displayList" border>
          <el-table-column type="selection" width="46" />
          <el-table-column prop="studentNo" label="学号" min-width="120" />
          <el-table-column prop="studentName" label="学生姓名" min-width="110" />
          <el-table-column label="班级" min-width="120">
            <template #default="{ row }">{{ classNameMap[row.classId] || `ID:${row.classId}` }}</template>
          </el-table-column>
          <el-table-column label="手机号" min-width="120">
            <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                @change="(v) => toggleStatus(row, v)"
              />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学生' : '新增学生'" width="620px">
    <el-form :model="form" label-width="90px">
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="学号" required><el-input v-model="form.studentNo" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="form.studentName" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="性别"><el-select v-model="form.gender"><el-option :value="1" label="男" /><el-option :value="2" label="女" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="班级" required><el-select v-model="form.classId" filterable><el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" inline-prompt active-text="启用" inactive-text="停用" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="专业"><el-input v-model="form.major" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="年级"><el-input v-model="form.grade" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="turnoverDialogVisible" title="批量删除（换届）" width="520px">
    <el-alert
      title="此操作不可恢复，请谨慎勾选。"
      type="error"
      :closable="false"
      style="margin-bottom: 12px"
    />
    <el-checkbox-group v-model="turnoverTargets">
      <el-checkbox value="STUDENT_DATA">学生信息</el-checkbox>
      <el-checkbox value="CLASS_DATA">班级信息（需同时勾选学生）</el-checkbox>
      <el-checkbox value="PLAN_DATA">点名计划（将同时清理任务与记录）</el-checkbox>
      <el-checkbox value="TASK_DATA">点名任务（将同时清理记录）</el-checkbox>
      <el-checkbox value="RECORD_DATA">考勤记录</el-checkbox>
      <el-checkbox value="IMPORT_LOG_DATA">导入日志</el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="turnoverDialogVisible = false">取消</el-button>
      <el-button type="danger" @click="turnoverReset">确认删除</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listClassesApi } from '../../api/class'
import { deleteStudentApi, importStudentsApi, listStudentsApi, saveStudentApi } from '../../api/student'
import { recordRetentionPolicyApi, turnoverResetApi } from '../../api/system'

const importMode = ref(2)
const classKeyword = ref('')
const allList = ref([])
const classes = ref([])
const dialogVisible = ref(false)
const turnoverDialogVisible = ref(false)
const turnoverTargets = ref(['STUDENT_DATA', 'CLASS_DATA'])
const retentionHint = ref('考勤记录仅保留最近1个月，系统每天 03:10 自动清理。')

const filters = reactive({
  classId: undefined,
  studentNo: '',
  studentName: '',
  phone: '',
  status: undefined,
  createRange: []
})

const classNameMap = computed(() => {
  const m = {}
  classes.value.forEach((c) => { m[c.id] = c.className })
  return m
})

const filteredClasses = computed(() => {
  const k = classKeyword.value.trim().toLowerCase()
  if (!k) return classes.value
  return classes.value.filter((c) => (c.className || '').toLowerCase().includes(k))
})

const displayList = computed(() => {
  const [start, end] = filters.createRange || []
  return (allList.value || []).filter((row) => {
    if (filters.classId !== undefined && row.classId !== filters.classId) return false
    if (filters.studentNo && !(row.studentNo || '').includes(filters.studentNo.trim())) return false
    if (filters.studentName && !(row.studentName || '').includes(filters.studentName.trim())) return false
    if (filters.phone && !(row.phone || '').includes(filters.phone.trim())) return false
    if (filters.status !== undefined && row.status !== filters.status) return false
    if (start || end) {
      const t = row.createTime ? dayjs(row.createTime) : null
      if (!t || !t.isValid()) return false
      if (start && t.isBefore(dayjs(start).startOf('day'))) return false
      if (end && t.isAfter(dayjs(end).endOf('day'))) return false
    }
    return true
  })
})

const form = reactive({
  id: null,
  studentNo: '',
  studentName: '',
  gender: 1,
  classId: undefined,
  major: '',
  grade: '',
  college: '',
  phone: '',
  status: 1
})

const resetForm = () => {
  Object.assign(form, {
    id: null,
    studentNo: '',
    studentName: '',
    gender: 1,
    classId: undefined,
    major: '',
    grade: '',
    college: '',
    phone: '',
    status: 1
  })
}

const pickClass = (id) => {
  filters.classId = id
}

const load = async () => {
  const keyword = `${filters.studentNo || ''} ${filters.studentName || ''}`.trim()
  allList.value = await listStudentsApi({
    classId: filters.classId,
    keyword: keyword || undefined
  })
}

const loadClasses = async () => {
  classes.value = await listClassesApi()
}

const formatDateTime = (val) => {
  if (!val) return '-'
  const d = dayjs(val)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : '-'
}

const maskPhone = (val) => {
  if (!val || val.length < 7) return val || '-'
  return `${val.slice(0, 3)}****${val.slice(-4)}`
}

const openDialog = (row) => {
  resetForm()
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

const submit = async () => {
  await saveStudentApi({ ...form })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

const toggleStatus = async (row, enabled) => {
  const snapshot = row.status
  row.status = enabled ? 1 : 0
  try {
    await saveStudentApi({ ...row, status: row.status })
    ElMessage.success('状态已更新')
  } catch (e) {
    row.status = snapshot
    throw e
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除学生【${row.studentName}】吗？`, '提示', { type: 'warning' })
  await deleteStudentApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

const uploadFile = async (opt) => {
  const fd = new FormData()
  fd.append('file', opt.file)
  fd.append('importMode', String(importMode.value))
  const msg = await importStudentsApi(fd)
  ElMessage.success(msg)
  await Promise.all([load(), loadClasses()])
}

const openTurnoverDialog = () => {
  turnoverDialogVisible.value = true
}

const turnoverReset = async () => {
  if (!turnoverTargets.value.length) {
    ElMessage.warning('请至少勾选一项删除内容')
    return
  }
  await turnoverResetApi(turnoverTargets.value)
  ElMessage.success('已清空历史数据，请导入新学期学生')
  turnoverDialogVisible.value = false
  await Promise.all([load(), loadClasses()])
}

const resetFilters = async () => {
  filters.classId = undefined
  filters.studentNo = ''
  filters.studentName = ''
  filters.phone = ''
  filters.status = undefined
  filters.createRange = []
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadClasses()])
  try {
    const policy = await recordRetentionPolicyApi()
    retentionHint.value = `${policy.description}（${policy.cleanupTime}）`
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.left-card {
  min-height: 760px;
}

.class-list {
  margin-top: 10px;
  max-height: 680px;
  overflow: auto;
}

.class-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  color: #606266;
}

.class-item:hover {
  background: #f5f7fa;
}

.class-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 10px;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
