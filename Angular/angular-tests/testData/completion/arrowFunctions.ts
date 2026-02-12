import {Component, signal} from '@angular/core';

@Component({
  template: `
    <input #foo>
    {{() => componentProp + 1}}
    {{(a,b) => val + 1}}
  `
})
export class TestComp {
  componentProp = 123;
}