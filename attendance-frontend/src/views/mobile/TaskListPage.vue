<template>
  <div class="m-page">
    <div class="m-header">待点名任务</div>
    <el-empty v-if="!loading && tasks.length === 0" description="暂无待点名任务（请检查班级绑定或计划是否已生成）" />
    <el-card v-for="item in tasks" :key="item.id" class="m-card" shadow="hover" @click="goCheckin(item)">
      <el-tag v-if="item.taskStatus === 3" type="success" size="small" class="submitted-tag">已提交（可修改）</el-tag>
      <el-tag v-else-if="item.taskStatus === 4" type="warning" size="small" class="submitted-tag">已超时</el-tag>
      <div class="class-title">{{ item.className || `ID:${item.classId}` }}</div>
      <div class="session-line">类型：{{ item.sessionLabel || '-' }}<span v-if="item.planName">（{{ item.planName }}）</span></div>
      <div>日期：{{ item.taskDate }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMyPendingTasksApi } from '../../api/task'
import { myClassesApi } from '../../api/class'

const router = useRouter()
const tasks = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const taskList = await listMyPendingTasksApi()
    let classNameMap = new Map()
    try {
      const myClasses = await myClassesApi()
      classNameMap = new Map((myClasses || []).map((item) => [item.id, item.className]))
    } catch (_) {
      // 班级接口异常时，任务列表仍可展示（回退为ID）
    }
    tasks.value = (taskList || []).map((item) => ({
      ...item,
      className: classNameMap.get(item.classId) || ''
    }))
  } catch (e) {
    tasks.value = []
    ElMessage.error('加载待点名任务失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
const goCheckin = (item) => router.push({ path: `/m/checkin/${item.id}` })
onMounted(load)
</script>

<style scoped>
.class-title {
  font-size: 22px;
  line-height: 1.25;
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}

.submitted-tag {
  float: right;
  margin-top: -2px;
}

.session-line {
  margin-bottom: 4px;
  font-size: 13px;
  color: #5f6f89;
}
</style>
