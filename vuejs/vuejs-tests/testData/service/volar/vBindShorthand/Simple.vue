<script setup lang="ts">
const <error>shouldError</error>: string = 5;
const id = "el"
const ariaLabel = "hello"
</script>

<template>
  <div :id />
  <!-- below was a bug in Vue LS https://github.com/vuejs/language-tools/issues/3830 -->
  <div <warning>:aria-label</warning> />
</template>
