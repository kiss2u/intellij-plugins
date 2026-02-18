<script setup lang="ts">
interface MyProps {
  optionsApiWithType: string
}

const props = defineProps<MyProps>()
</script>

<template>
  {{optionsApiWithType}}
  {{props.optionsApiWithType}}
</template>
