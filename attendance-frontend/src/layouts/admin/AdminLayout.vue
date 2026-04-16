<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <div class="logo-brand">
          <img :src="logo" alt="杭科易网通Logo" class="logo-image" />
          <div>
            <div class="logo-title">杭科易网通</div>
            <div class="logo-subtitle">智能考勤管理平台</div>
          </div>
        </div>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/dashboard">首页</el-menu-item>
        <el-menu-item index="/system/member">部员管理</el-menu-item>
        <el-menu-item index="/student/list">学生信息</el-menu-item>
        <el-menu-item index="/student/class">班级管理</el-menu-item>
        <el-menu-item index="/attendance/plan">考勤计划</el-menu-item>
        <el-menu-item index="/attendance/record">考勤记录</el-menu-item>
        <el-menu-item index="/attendance/report">统计分析</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>
          <h2 class="header-title">{{ pageTitle }}</h2>
          <div class="header-subtitle">{{ todayText }}</div>
        </div>
        <div class="header-right">
          <div class="user-chip">
            <span class="user-chip-dot" />
            <span>{{ userStore.userInfo.realName || '部长' }}</span>
          </div>
          <el-button type="danger" plain size="small" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import logo from '../../images/hkywt_logo.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const titleMap = {
  '/dashboard': '后台首页',
  '/system/member': '部员管理',
  '/student/list': '学生信息',
  '/student/class': '班级管理',
  '/attendance/plan': '考勤计划',
  '/attendance/record': '考勤记录',
  '/attendance/report': '统计分析'
}

const pageTitle = computed(() => titleMap[route.path] || '考勤系统')
const todayText = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'full',
  timeZone: 'Asia/Shanghai'
}).format(new Date())

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>
