<template>
  <el-card>
    <template #header>
      <div class="toolbar" style="margin:0">
        <span>班级管理</span>
        <div>
          <el-button type="primary" @click="openDialog()">新增班级</el-button>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="班级名称/编码" style="width:240px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="classCode" label="班级编码" min-width="120" />
      <el-table-column prop="className" label="班级名称" min-width="120" />
      <el-table-column prop="major" label="专业" min-width="160" />
      <el-table-column prop="grade" label="年级" width="90" />
      <el-table-column prop="college" label="学院" min-width="180" />
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
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑班级' : '新增班级'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="班级编码" required><el-input v-model="form.classCode" /></el-form-item>
        <el-form-item label="班级名称" required><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" /></el-form-item>
        <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" inline-prompt active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { deleteClassApi, listClassesApi, saveClassApi } from '../../api/class'

const keyword = ref('')
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, classCode: '', className: '', major: '', grade: '', college: '', status: 1 })

const resetForm = () => {
  form.id = null
  form.classCode = ''
  form.className = ''
  form.major = ''
  form.grade = ''
  form.college = ''
  form.status = 1
}

const openDialog = (row) => {
  resetForm()
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

const load = async () => {
  list.value = await listClassesApi({ keyword: keyword.value })
}

const submit = async () => {
  await saveClassApi({ ...form })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

const toggleStatus = async (row, enabled) => {
  const snapshot = row.status
  row.status = enabled ? 1 : 0
  try {
    await saveClassApi({ ...row, status: row.status })
    ElMessage.success('状态已更新')
  } catch (e) {
    row.status = snapshot
    throw e
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除班级【${row.className}】吗？`, '提示', { type: 'warning' })
  await deleteClassApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
