import {Component, signal} from '@angular/core';

@Component({
  template: `
    @let test = (a<caret>,b) => a + 1;
    {{a}}
  `
})
export class TestComp {
  a: number = 1
}