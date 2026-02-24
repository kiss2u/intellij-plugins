<script setup lang="ts">
export function hello(p: number, p2: number) {
    console.log(p2);
}
let <error descr="Vue: Type 'number' is not assignable to type 'string'.">a</error>: string = 1;
</script>

<template>
</template>
