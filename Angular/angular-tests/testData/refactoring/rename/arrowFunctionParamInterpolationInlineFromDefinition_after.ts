import {Component, signal} from '@angular/core';

@Component({
  template: `
      {{ (newName<caret>, b) => newName + 1 }}
      {{ a }}
  `
})
export class TestComp {
  a: number = 1
}