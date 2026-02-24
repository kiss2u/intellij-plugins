<script setup lang="ts">
export function hello(<weak_warning descr="Vue: 'p' is declared but its value is never read." textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">p</weak_warning>: number, p2: number) {
    console.log(p2);
}
let <error descr="Vue: Type 'number' is not assignable to type 'string'.">a</error>: string = 1;
</script>

<template>
</template>
