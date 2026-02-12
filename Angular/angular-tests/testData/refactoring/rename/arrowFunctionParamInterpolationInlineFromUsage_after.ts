import {Component, signal} from '@angular/core';

@Component({
  template: `
      {{ (newName, b) => newName<caret> + 1 }}
      {{ a }}
  `
})
export class TestComp {
  a: number = 1
}