<script setup lang="ts">
interface MyProps {
  optionsApiWithType: string
}

const props = defineProps<MyProps>()
</script>

<template>
  {{opt<caret>ionsApiWithType}}
  {{props.optionsApiWithType}}
</template>
