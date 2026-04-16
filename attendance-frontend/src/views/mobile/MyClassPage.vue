<template>
  <div class="m-page">
    <div class="m-header">我的负责班级</div>
    <el-card class="m-card" v-for="item in list" :key="item.id">
      <div>{{ item.className }}</div>
      <div style="font-size:12px;color:#909399">{{ item.classCode }} / {{ item.grade || '-' }} / {{ item.major || '-' }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myClassesApi } from '../../api/class'

const list = ref([])
const load = async () => { list.value = await myClassesApi() }
onMounted(load)
</script>
