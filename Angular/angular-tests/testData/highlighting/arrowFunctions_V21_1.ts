import {Component, signal} from '@angular/core';

@Component({
  template: `
    <button (click)="sig.update(<error descr="Arrow functions are supported only in Angular 21.2 and above.">() => <error descr="TS2322: Type 'string' is not assignable to type 'number'.">'hello'</error></error>)"></button>
    {{(<error descr="Arrow functions are supported only in Angular 21.2 and above.">(a) => acceptsString(<error descr="TS2345: Argument of type 'number' is not assignable to parameter of type 'string'.">a</error>)</error>)(1)}}
   
  `
})
export class TestComp {

  sig = signal(1);

  acceptsString(<warning descr="Unused parameter value"><weak_warning descr="TS6133: 'value' is declared but its value is never read.">value</weak_warning></warning>: string): number {
    return 1;
  }
}