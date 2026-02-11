// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import { Component, Input } from '@angular/core';
import { NgForOf, NgIf } from '@angular/common';

interface IData {
  icon?: string
  check?(): string
}

@Component({
  selector: 'app-root',
  template: `
    @let arr_simple = [...foo];
    @let arr_otherEntries = [1, ...foo, 2];
    @let arr_multipleSpreads = [...foo, 1, ...bar, ...baz, 2];
    @let arr_inlineArraySpread = [1, ...[2, ...[3]]];
    
    <!-- Use the arrays so they don't get flagged as unused. -->
    {{arr_simple}} {{arr_otherEntries}} {{arr_multipleSpreads}} {{arr_inlineArraySpread}}
    
    @let simple = {...foo};
    @let otherProps = {a: 1, ...foo, b: 2};
    @let multipleSpreads = {...foo, a: 1, ...bar, ...baz, b: 2};
    @let objectLiteral = {a: 1, ...{b: {...{c: 3}}}};
    
    <!-- Use the objects so they don't get flagged as unused. -->
    {{simple}} {{otherProps}} {{multipleSpreads}} {{objectLiteral}}
    
    {{fn(...foo)}}
    {{fn(1, ...foo, 2)}}
    {{fn(...foo, 1, ...bar, ...baz, 2)}}
    {{fn(1, ...[2, ...[3]])}}     
      
  `,
  standalone: true
})
export class AppComponent {
  foo = [];
  bar = [];
  baz = [];
  fn(..._: any[]) {}
}