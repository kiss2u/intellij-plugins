import {Component} from '@angular/core';

@Component({
 selector: 'arrow-test',
 standalone: true,
 templateUrl: "./template.html"
})
export class ArrowComponent {

  b: string

  withCallback(callback: (a: number, b?: number) => number) {

  }

  componentFn(value: number): number {

  }

}
