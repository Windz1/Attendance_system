<template>
  <el-card>
    <template #header>
      <div class="toolbar" style="margin:0">
        <span>部员管理</span>
        <el-button type="primary" @click="openDialog()">新增部员</el-button>
        <el-button type="success" plain @click="openImportDialog">从学生导入</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="姓名/账号" style="width:220px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="realName" label="姓名" min-width="120" />
      <el-table-column prop="username" label="账号" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="status" label="状态" width="120">
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
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.userId ? '编辑部员' : '新增部员'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="账号" required><el-input v-model="form.username" :disabled="!!form.userId" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12" v-if="!form.userId">
          <el-col :span="12"><el-form-item label="初始密码" required><el-input v-model="form.password" type="password" show-password placeholder="至少10位" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12" v-else>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" inline-prompt active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="负责班级" required>
          <el-select v-model="form.classIds" multiple filterable style="width:100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="从学生导入部员" width="900px">
      <el-alert
        title="账号默认为学号，系统将为每人生成独立随机密码；负责班级自动绑定为该学生所在班级。"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <div class="toolbar">
        <el-input v-model="studentKeyword" placeholder="按学号/姓名搜索学生" style="width:220px" clearable @keyup.enter="loadStudents" />
        <el-select v-model="studentClassId" placeholder="按班级筛选" clearable style="width:220px" @change="loadStudents">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="loadStudents">查询</el-button>
      </div>
      <el-table :data="studentList" border @selection-change="onStudentSelectionChange" row-key="id" max-height="420">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="studentName" label="姓名" min-width="120" />
        <el-table-column label="班级" min-width="130">
          <template #default="{ row }">{{ classNameMap[row.classId] || `ID:${row.classId}` }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
      </el-table>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="importFromStudents">导入选中学生（{{ selectedStudentIds.length }}）</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listClassesApi } from '../../api/class'
import { listStudentsApi } from '../../api/student'
import {
  createMemberApi,
  deleteMemberApi,
  importMembersFromStudentsApi,
  listMembersApi,
  memberClassIdsApi,
  resetMemberPwdApi,
  updateMemberApi
} from '../../api/member'

const keyword = ref('')
const list = ref([])
const classes = ref([])
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const studentKeyword = ref('')
const studentClassId = ref(undefined)
const studentList = ref([])
const selectedStudentIds = ref([])
const form = reactive({ userId: null, realName: '', username: '', password: '', phone: '', status: 1, classIds: [] })

const classNameMap = computed(() => {
  const map = {}
  ;(classes.value || []).forEach((c) => {
    map[c.id] = c.className
  })
  return map
})

const resetForm = () => {
  form.userId = null
  form.realName = ''
  form.username = ''
  form.password = ''
  form.phone = ''
  form.status = 1
  form.classIds = []
}

const load = async () => { list.value = await listMembersApi({ keyword: keyword.value }) }
const loadClasses = async () => { classes.value = await listClassesApi() }
const loadStudents = async () => {
  studentList.value = await listStudentsApi({
    keyword: studentKeyword.value || undefined,
    classId: studentClassId.value
  })
}

const openDialog = async (row) => {
  resetForm()
  if (row?.id) {
    form.userId = row.id
    form.realName = row.realName
    form.username = row.username
    form.phone = row.phone
    form.status = row.status
    form.classIds = await memberClassIdsApi(row.id)
  }
  dialogVisible.value = true
}

const openImportDialog = async () => {
  selectedStudentIds.value = []
  studentKeyword.value = ''
  studentClassId.value = undefined
  importDialogVisible.value = true
  await loadStudents()
}

const onStudentSelectionChange = (rows) => {
  selectedStudentIds.value = (rows || []).map((v) => v.id)
}

const submit = async () => {
  if (!form.classIds.length) return ElMessage.warning('请选择负责班级')
  if (!form.userId && (form.password || '').length < 10) return ElMessage.warning('初始密码至少需要10位')
  if (form.userId) {
    await updateMemberApi({ userId: form.userId, realName: form.realName, phone: form.phone, status: form.status, classIds: form.classIds })
  } else {
    await createMemberApi({ realName: form.realName, username: form.username, password: form.password, phone: form.phone, status: form.status, classIds: form.classIds })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

const importFromStudents = async () => {
  if (!selectedStudentIds.value.length) {
    return ElMessage.warning('请先选择学生')
  }
  const message = await importMembersFromStudentsApi({ studentIds: selectedStudentIds.value })
  ElMessage.success(message || '导入成功')
  importDialogVisible.value = false
  await load()
}

const resetPwd = async (row) => {
  const { value } = await ElMessageBox.prompt(`请输入【${row.realName}】的新密码`, '重置密码', {
    inputType: 'password',
    inputPlaceholder: '至少10位',
    inputPattern: /^.{10,72}$/,
    inputErrorMessage: '密码长度必须为10到72位',
    confirmButtonText: '确认重置',
    cancelButtonText: '取消'
  })
  await resetMemberPwdApi(row.id, { newPassword: value })
  ElMessage.success('重置成功')
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除【${row.realName}】？`, '提示', { type: 'warning' })
  await deleteMemberApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

const toggleStatus = async (row, enabled) => {
  const snapshot = row.status
  row.status = enabled ? 1 : 0
  try {
    const classIds = await memberClassIdsApi(row.id)
    await updateMemberApi({
      userId: row.id,
      realName: row.realName,
      phone: row.phone,
      status: row.status,
      classIds
    })
    ElMessage.success('状态已更新')
  } catch (e) {
    row.status = snapshot
    throw e
  }
}

onMounted(async () => {
  await Promise.all([load(), loadClasses()])
})
</script>
