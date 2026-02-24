<script setup lang="ts">
let <error descr="Vue: Type 'number' is not assignable to type 'string'.">a</error>: string = 1;

function acceptNumber(num: number): number { return num; }

acceptNumber(<error descr="Vue: Argument of type 'boolean' is not assignable to parameter of type 'number'.">true</error>);
</script>

<template>
  <div v-text="acceptNumber(<error descr="Vue: Argument of type 'boolean' is not assignable to parameter of type 'number'.">true</error>)" />
  <div>{{acceptNumber(<error descr="Vue: Argument of type 'boolean' is not assignable to parameter of type 'number'.">true</error>)}}</div>
</template>
