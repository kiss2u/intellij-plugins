import {Component} from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
  
    @let obj = {a: 1, <error descr="Spread syntax is supported only in Angular 21.1 and above.">...foo</error>};
    @let array = [1, <error descr="Spread syntax is supported only in Angular 21.1 and above.">...bar</error>];
    {{fn('one', <error descr="Spread syntax is supported only in Angular 21.1 and above."><error descr="TS2345: Argument of type 'number' is not assignable to parameter of type 'string'.">...rest</error></error>)}}
  
    {{obj}} {{array}}
  `,
})
export class TestComponent {

  foo = {b: 'two'};
  bar = ['two'];
  rest = [2];

  fn(<warning descr="Unused parameter first"><weak_warning descr="TS6133: 'first' is declared but its value is never read.">first</weak_warning></warning>: string, ...<warning descr="Unused parameter rest"><weak_warning descr="TS6133: 'rest' is declared but its value is never read.">rest</weak_warning></warning>: string[]) {}

}