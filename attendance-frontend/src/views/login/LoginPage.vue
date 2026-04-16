<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="login-intro">
        <div class="intro-badge">智能考勤管理平台</div>
        <h1>杭科易网通考勤系统</h1>
        <p>统一管理部员、班级与学生考勤任务，支持多角色协同与移动端点名，提升日常考勤执行效率与统计准确性。</p>
        <div class="intro-points">
          <div>计划编排 + 自动任务生成</div>
          <div>移动端快速点名与补提</div>
          <div>按班级统计分析与导出</div>
        </div>
        <div class="intro-meta">
          <div>
            <div class="meta-label">记录保留策略</div>
            <div class="meta-value">仅保留最近 1 个月</div>
          </div>
          <div>
            <div class="meta-label">每日清理时间</div>
            <div class="meta-value">03:10</div>
          </div>
        </div>
      </section>
      <el-card class="login-card">
        <div class="login-brand">
          <img :src="logo" alt="杭科易网通Logo" class="login-brand-logo" />
          <div>
            <h2>欢迎登录</h2>
            <div class="login-subtitle">请使用管理员或部员账号登录系统</div>
          </div>
        </div>
        <el-form :model="form" label-width="70px" @keyup.enter="onLogin">
          <el-form-item label="账号"><el-input v-model="form.username" placeholder="请输入账号" size="large" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" /></el-form-item>
          <el-button type="primary" class="login-submit" @click="onLogin">登录系统</el-button>
        </el-form>
        <div class="login-footer-hint">登录即表示你同意按学院考勤管理规范使用本系统</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import logo from '../../images/hkywt_logo.png'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: '', password: '' })

const onLogin = async () => {
  const data = await userStore.login(form)
  router.push(data.homePath)
}
</script>

<style scoped>
.login-intro {
  position: relative;
  overflow: hidden;
}

.intro-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.5px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.intro-points {
  margin-top: 16px;
  display: grid;
  gap: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.94);
}

.intro-points > div::before {
  content: "• ";
}

.intro-meta {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.24);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.76);
}

.meta-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.login-card {
  backdrop-filter: blur(8px);
}

.login-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #8a98af;
}

.login-submit {
  width: 100%;
  margin-top: 8px;
  height: 42px;
  font-size: 15px;
}

.login-footer-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #98a5ba;
  text-align: center;
}
</style>
