<template>
  <div class="dashboard-page">
    <el-card class="hero-card">
      <div class="hero-grid">
        <div>
          <div class="hero-kicker">智能考勤管理平台</div>
          <h1 class="hero-title">杭科易网通物联网技术学院考勤系统</h1>
          <p class="hero-desc">
            面向学院日常点名、计划编排、考勤追踪与统计分析的一体化后台。
            支持按班级和计划快速筛选、移动端点名、自动化数据清理，帮助考勤工作更规范、更高效。
          </p>
          <div class="hero-actions">
            <el-button type="primary" @click="go('/attendance/plan')">创建考勤计划</el-button>
            <el-button @click="go('/attendance/record')">查看考勤记录</el-button>
            <el-button @click="go('/attendance/report')">进入统计分析</el-button>
          </div>
        </div>
        <div class="hero-panel">
          <div class="panel-title">系统介绍</div>
          <div class="panel-line"><span>1.</span><span>计划驱动：先设计划，再生成任务，流程清晰。</span></div>
          <div class="panel-line"><span>2.</span><span>移动点名：部员可在手机端快速提交和二次修改。</span></div>
          <div class="panel-line"><span>3.</span><span>记录可追溯：记录页支持按区间、班级、状态检索导出。</span></div>
          <div class="panel-line"><span>4.</span><span>自动维护：系统每日自动清理 1 个月前考勤记录。</span></div>
        </div>
      </div>
    </el-card>

    <div class="stat-grid">
      <el-card v-for="item in stats" :key="item.label" class="stat-card" shadow="never">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-note">{{ item.note }}</div>
      </el-card>
    </div>

    <el-row :gutter="14">
      <el-col :span="8" v-for="item in modules" :key="item.path">
        <el-card class="module-card" shadow="never" @click="go(item.path)">
          <div class="module-icon">{{ item.icon }}</div>
          <div class="module-title">{{ item.title }}</div>
          <div class="module-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="tip-card" shadow="never">
      <template #header><span>使用建议</span></template>
      <div class="tip-list">
        <div>1. 新学期开始前，先完成班级与学生信息更新，再创建考勤计划。</div>
        <div>2. 计划建议按教学场景拆分（如早自习、晚自习），便于后续统计对比。</div>
        <div>3. 若出现换届，建议先使用“批量删除”清理历史数据后再导入新生。</div>
      </div>
    </el-card>

    <div class="studio-watermark">INet工作室制作</div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = [
  { label: '运行状态', value: '正常', note: '系统服务已就绪' },
  { label: '记录保留策略', value: '1个月', note: '每日 03:10 自动清理' },
  { label: '点名模式', value: '移动优先', note: '支持已提交后再修改' }
]

const modules = [
  { path: '/system/member', icon: '👥', title: '部员管理', desc: '管理点名成员、负责班级和账号状态。' },
  { path: '/student/list', icon: '🎓', title: '学生信息', desc: '维护学生档案，支持导入与批量删除。' },
  { path: '/attendance/plan', icon: '📅', title: '考勤计划', desc: '配置计划时间窗、执行星期与适用班级。' },
  { path: '/attendance/record', icon: '🧾', title: '考勤记录', desc: '按条件筛选、导出记录，快速追溯。' },
  { path: '/attendance/report', icon: '📊', title: '统计分析', desc: '查看各班考勤总量、状态占比和趋势。' },
  { path: '/student/class', icon: '🏫', title: '班级管理', desc: '维护班级基础信息和启停状态。' }
]

const go = (path) => router.push(path)
</script>

<style scoped>
.dashboard-page {
  position: relative;
  padding-bottom: 22px;
}

.hero-card {
  margin-bottom: 14px;
  border: none;
  background:
    radial-gradient(circle at 12% 18%, rgba(73, 145, 255, 0.22), transparent 34%),
    radial-gradient(circle at 85% 8%, rgba(38, 193, 143, 0.2), transparent 32%),
    linear-gradient(130deg, #0f2b58 0%, #1d4f96 45%, #2c6dc7 100%);
  color: #eff5ff;
}

.hero-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 1px;
  opacity: 0.88;
}

.hero-title {
  margin: 8px 0 12px;
  font-size: 30px;
  line-height: 1.2;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: rgba(239, 245, 255, 0.94);
}

.hero-actions {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-actions :deep(.el-button:not(.el-button--primary)) {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.hero-panel {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(11, 24, 50, 0.34);
  padding: 14px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 10px;
}

.panel-line {
  display: grid;
  grid-template-columns: 20px 1fr;
  gap: 6px;
  font-size: 13px;
  line-height: 1.65;
  color: rgba(239, 245, 255, 0.92);
  margin-bottom: 4px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.stat-label {
  color: #66789a;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
  color: #1f2a44;
}

.stat-note {
  margin-top: 8px;
  font-size: 12px;
  color: #8b9bb6;
}

.module-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.module-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(25, 51, 93, 0.12);
}

.module-icon {
  font-size: 22px;
}

.module-title {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #1f2a44;
}

.module-desc {
  margin-top: 6px;
  color: #657894;
  font-size: 13px;
  line-height: 1.65;
}

.tip-card {
  margin-top: 2px;
}

.tip-list {
  font-size: 13px;
  color: #4f607a;
  line-height: 1.75;
}

.studio-watermark {
  position: fixed;
  right: 18px;
  bottom: 10px;
  font-size: 11px;
  color: rgba(44, 65, 101, 0.5);
  letter-spacing: 0.4px;
  pointer-events: none;
  user-select: none;
}

@media (max-width: 1280px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
