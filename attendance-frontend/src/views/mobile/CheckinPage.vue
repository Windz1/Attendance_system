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
      <div class="class-photo-card">
        <div class="class-photo-title">班级照片 <span class="required">*</span></div>
        <input
          ref="captureInputRef"
          type="file"
          accept="image/*"
          capture="environment"
          class="hidden-file-input"
          @change="onCaptureChange"
        />
        <el-button type="primary" plain :loading="classPhotoUploading" @click="openCamera">
          拍照并上传（{{ classPhotoUrls.length }}/9）
        </el-button>
        <div v-if="classPhotoUrls.length > 0" class="photo-grid">
          <div v-for="(url, idx) in classPhotoUrls" :key="`${url}-${idx}`" class="photo-item">
            <el-image :src="classPhotoPreviewUrls[idx]" :preview-src-list="classPhotoPreviewUrls" :initial-index="idx" fit="cover" class="photo-preview" />
            <el-button text type="danger" class="photo-remove" @click="removePhoto(idx)">删除</el-button>
          </div>
        </div>
        <div v-else class="photo-tip">请点击拍照按钮，拍摄班级照片（自动加拍摄时间水印）。</div>
      </div>
      <el-button type="primary" class="submit-btn" :disabled="records.length===0 || submitDisabled || classPhotoUploading" @click="submit">
        {{ task?.taskStatus===3 ? (canModifyAfterSubmit ? '重新提交' : '任务已提交') : '提交点名' }}
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loadClassPhotoApi, submitTaskApi, taskDetailApi, uploadClassPhotoApi } from '../../api/task'
import { myClassesApi } from '../../api/class'

const route = useRoute()
const router = useRouter()
const task = ref(null)
const records = ref([])
const canModifyAfterSubmit = ref(false)
const submitDisabled = ref(false)
const classPhotoUrls = ref([])
const classPhotoPreviewUrls = ref([])
const classPhotoUploading = ref(false)
const captureInputRef = ref(null)
const MAX_LONG_EDGE = 1920
const TARGET_MAX_BYTES = 850 * 1024
const JPEG_QUALITIES = [0.9, 0.86, 0.82, 0.78, 0.74]
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
  classPhotoUrls.value = Array.isArray(data.classPhotoUrls)
    ? data.classPhotoUrls
    : (data.classPhotoUrl ? [data.classPhotoUrl] : [])
  await loadExistingPhotoPreviews()
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

const beforePhotoUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!isImage) {
    ElMessage.error('仅支持 JPG/PNG/WEBP 图片')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片不能超过 10MB')
    return false
  }
  return true
}

const openCamera = () => {
  if (classPhotoUrls.value.length >= 9) {
    ElMessage.warning('最多上传 9 张照片')
    return
  }
  captureInputRef.value?.click()
}

const onCaptureChange = async (event) => {
  const file = event.target?.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!beforePhotoUpload(file)) return
  if (classPhotoUrls.value.length >= 9) {
    ElMessage.warning('最多上传 9 张照片')
    return
  }
  try {
    const capturedAt = formatNow(new Date())
    const watermarkedFile = await addTimestampWatermark(file, capturedAt)
    await uploadPhoto(watermarkedFile)
  } catch (e) {
    ElMessage.error('拍照处理失败，请重试')
  }
}

const uploadPhoto = async (file) => {
  classPhotoUploading.value = true
  try {
    const url = await uploadClassPhotoApi(Number(route.params.taskId), file)
    classPhotoUrls.value = [...classPhotoUrls.value, url]
    classPhotoPreviewUrls.value = [...classPhotoPreviewUrls.value, URL.createObjectURL(file)]
    ElMessage.success('拍照上传成功')
  } finally {
    classPhotoUploading.value = false
  }
}

const removePhoto = (idx) => {
  revokeObjectUrl(classPhotoPreviewUrls.value[idx])
  classPhotoUrls.value = classPhotoUrls.value.filter((_, index) => index !== idx)
  classPhotoPreviewUrls.value = classPhotoPreviewUrls.value.filter((_, index) => index !== idx)
}

const loadExistingPhotoPreviews = async () => {
  classPhotoPreviewUrls.value.forEach(revokeObjectUrl)
  classPhotoPreviewUrls.value = await Promise.all(classPhotoUrls.value.map(async (url) => {
    try {
      const blob = await loadClassPhotoApi(url)
      return URL.createObjectURL(blob)
    } catch (_) {
      return ''
    }
  }))
}

const revokeObjectUrl = (url) => {
  if (url?.startsWith('blob:')) URL.revokeObjectURL(url)
}

const formatNow = (date) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const readFileAsDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })

const loadImage = (src) =>
  new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })

const canvasToJpegBlob = (canvas, quality) =>
  new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality))

const exportCompressedBlob = async (canvas) => {
  let workCanvas = canvas
  for (let round = 0; round < 3; round += 1) {
    for (const quality of JPEG_QUALITIES) {
      const blob = await canvasToJpegBlob(workCanvas, quality)
      if (blob && blob.size <= TARGET_MAX_BYTES) return blob
    }
    const nextWidth = Math.max(960, Math.round(workCanvas.width * 0.9))
    const nextHeight = Math.max(540, Math.round(workCanvas.height * 0.9))
    if (nextWidth === workCanvas.width || nextHeight === workCanvas.height) break
    const nextCanvas = document.createElement('canvas')
    nextCanvas.width = nextWidth
    nextCanvas.height = nextHeight
    const nextCtx = nextCanvas.getContext('2d')
    nextCtx.imageSmoothingEnabled = true
    nextCtx.imageSmoothingQuality = 'high'
    nextCtx.drawImage(workCanvas, 0, 0, nextWidth, nextHeight)
    workCanvas = nextCanvas
  }
  return canvasToJpegBlob(workCanvas, JPEG_QUALITIES[JPEG_QUALITIES.length - 1])
}

const addTimestampWatermark = async (file, timestamp) => {
  const src = await readFileAsDataUrl(file)
  const img = await loadImage(src)

  const longEdge = Math.max(img.width, img.height)
  const scale = longEdge > MAX_LONG_EDGE ? MAX_LONG_EDGE / longEdge : 1
  const outputWidth = Math.max(1, Math.round(img.width * scale))
  const outputHeight = Math.max(1, Math.round(img.height * scale))

  const canvas = document.createElement('canvas')
  canvas.width = outputWidth
  canvas.height = outputHeight
  const ctx = canvas.getContext('2d')
  ctx.imageSmoothingEnabled = true
  ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

  const fontSize = Math.max(18, Math.round(canvas.width * 0.03))
  ctx.font = `600 ${fontSize}px sans-serif`
  const text = `拍摄时间 ${timestamp}`
  const padding = Math.round(fontSize * 0.6)
  const textWidth = ctx.measureText(text).width
  const boxWidth = textWidth + padding * 2
  const boxHeight = fontSize + padding
  const x = canvas.width - boxWidth - Math.round(canvas.width * 0.03)
  const y = canvas.height - Math.round(canvas.height * 0.04)

  ctx.fillStyle = 'rgba(0, 0, 0, 0.48)'
  ctx.fillRect(x, y - boxHeight, boxWidth, boxHeight)
  ctx.fillStyle = '#ffffff'
  ctx.fillText(text, x + padding, y - Math.round(padding * 0.45))

  const blob = await exportCompressedBlob(canvas)
  if (!blob) throw new Error('图片处理失败')
  return new File([blob], `class-photo-${Date.now()}.jpg`, { type: 'image/jpeg' })
}

const submit = async () => {
  if (!classPhotoUrls.value.length) {
    ElMessage.error('请先拍摄并上传至少一张班级照片')
    return
  }
  await submitTaskApi({
    taskId: Number(route.params.taskId),
    classPhotoUrls: classPhotoUrls.value,
    records: records.value.map(({ studentId, attendanceStatus, remark }) => ({ studentId, attendanceStatus, remark }))
  })
  ElMessage.success('提交成功')
  router.push('/m/tasks')
}

onMounted(loadDetail)
onBeforeUnmount(() => classPhotoPreviewUrls.value.forEach(revokeObjectUrl))
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

.class-photo-card {
  margin-top: 12px;
  padding: 10px;
  border: 1px dashed #d9dfe8;
  border-radius: 10px;
  background: #f9fbff;
}

.hidden-file-input {
  display: none;
}

.class-photo-title {
  margin-bottom: 8px;
  font-size: 14px;
  color: #2e3b52;
  font-weight: 600;
}

.required {
  color: #d93026;
}

.photo-preview {
  width: 100%;
  height: 120px;
  border-radius: 8px;
  border: 1px solid #dbe4f3;
}

.photo-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.photo-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.photo-remove {
  align-self: center;
  padding: 0;
  font-size: 12px;
}

.photo-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #7a8aa4;
}
</style>
