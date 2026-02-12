import {Component, signal} from '@angular/core';

@Component({
  template: `
    {{(a,b) => a<caret> + 1}}
    {{a}}
  `
})
export class TestComp {
  a: number = 1
}