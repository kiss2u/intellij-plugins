import {Component} from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
  
    <!-- type check object spread assignments in templates -->
    @let obj = {a: 1, ...foo}; {{checkObj(<error descr="TS2345: Argument of type '{ b: string; a: number; }' is not assignable to parameter of type '{ a: number; b: number; }'.
  Types of property 'b' are incompatible.
    Type 'string' is not assignable to type 'number'.">obj</error>)}}
    
    <!-- type check array spread assignments in templates -->
    @let array = [1, ...bar]; {{checkArray(<error descr="TS2345: Argument of type '(string | number)[]' is not assignable to parameter of type 'number[]'.
  Type 'string | number' is not assignable to type 'number'.
    Type 'string' is not assignable to type 'number'.">array</error>)}}
    
    <!-- type check rest arguments in a function call -->
    {{fn('one', <error descr="TS2345: Argument of type 'number' is not assignable to parameter of type 'string'.">...rest</error>)}}
  
    {{obj}} {{array}}
  `,
})
export class TestComponent {

  foo = {b: 'two'};
  bar = ['two'];
  rest = [2];

  checkObj(<warning descr="Unused parameter obj"><weak_warning descr="TS6133: 'obj' is declared but its value is never read.">obj</weak_warning></warning>: {a: number, b: number}) {}
  checkArray(<warning descr="Unused parameter arr"><weak_warning descr="TS6133: 'arr' is declared but its value is never read.">arr</weak_warning></warning>: number[]) {}
  fn(<warning descr="Unused parameter first"><weak_warning descr="TS6133: 'first' is declared but its value is never read.">first</weak_warning></warning>: string, ...<warning descr="Unused parameter rest"><weak_warning descr="TS6133: 'rest' is declared but its value is never read.">rest</weak_warning></warning>: string[]) {}

}