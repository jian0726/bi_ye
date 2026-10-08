<template>
  <div class="flex flex-col gap-6">
    <div
      v-for="i in count"
      :key="i"
      class="card p-7"
    >
      <div class="flex items-center gap-2.5 mb-4">
        <div class="skeleton h-3 w-14" />
        <div class="skeleton h-3 w-20" />
      </div>
      <div class="skeleton h-5.5 w-4/5 mb-3" style="height: 22px" />
      <div class="skeleton h-3.5 w-full mb-2" />
      <div class="skeleton h-3.5 w-3/5" />
      <div class="mt-5 flex gap-2">
        <div class="skeleton h-6 w-14 rounded-full" />
        <div class="skeleton h-6 w-16 rounded-full" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ count?: number }>(), { count: 4 })
</script>
