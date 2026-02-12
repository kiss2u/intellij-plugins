import {Component, signal} from '@angular/core';

@Component({
  template: `
    <!-- should infer the types of parameters of arrow functions passed in as callbacks -->
    <button (click)="sig.update(prev => acceptsString(<error descr="TS2345: Argument of type 'number' is not assignable to parameter of type 'string'.">prev</error>))"></button>
    
    <!-- should infer the return type of arrow functions -->
    <button (click)="sig.update(() => <error descr="TS2322: Type 'string' is not assignable to type 'number'.">'hello'</error>)"></button>
    
    <!-- should infer the parameter type of arrow functions when they are called immediately -->
    {{((a) => acceptsString(<error descr="TS2345: Argument of type 'number' is not assignable to parameter of type 'string'.">a</error>))(1)}}
    
    <!-- should not report implicit any errors on arrow functions defined in @let -->
    @let arrowFn = a => a;
    {{arrowFn(1)}}
    
    <!-- should not allow pipe to be used inside an arrow function -->
    {{ (a, b) => (a + b<error descr=") expected"> </error>| <error descr="Unresolved pipe pipe">pipe</error><error descr="Unexpected token ')'">)</error> }}
    
    <!-- should report an error for an arrow function with a body -->
    {{ () => <error descr="Multi-line arrow functions are not supported. If you meant to return an object literal, wrap it with parentheses.">{}</error> }}
    
    <!-- should report missing comma between arrow function parameters -->
    {{ (<error descr="TS2304: Cannot find name 'a'.">a</error><error descr=", or ) expected"> </error>b<error descr="Unexpected token ')'">)</error><error descr="Unexpected token '=>'"> </error>=><error descr="Unexpected token 'a'"> </error>a + b }}
    
    <!-- should report arrow function parameter starting with a comma -->
    {{ (<error descr="TS2695: Left side of comma operator is unused and has no side effects."></error><error descr="Expression expected"><error descr="TS1109: Expression expected.">,</error></error> <error descr="TS2339: Property 'a' does not exist on type 'TestComp'.">a</error><error descr="Unexpected token ')'">)</error><error descr="Unexpected token '=>'"> </error>=><error descr="Unexpected token 'a'"> </error>a }}
    
    <!-- should report arrow function parameter with a trailing comma -->
    {{ (a<error descr="Trailing comma in a parameter list is not supported">,</error> ) => a }}
    
    <!-- should report an arrow function without a closing paren -->
    {{ (a => a + 1<error descr=") expected"> </error>}}
    
    <!-- should report an arrow function without an opening paren -->
    {{ <error descr="TS2339: Property 'a' does not exist on type 'TestComp'.">a</error><error descr="Unexpected token ')'">)</error><error descr="Unexpected token '=>'"> </error>=><error descr="Unexpected token 'a'"> </error>a + 1 }}
    
    <!-- should report an error inside the arrow function expression -->
    {{ (a) => a.<error descr="Name expected"> </error><error descr="TS1003: Identifier expected.">+</error> 1 }}
    
    <!-- should report an error for chained expression in arrow function -->
    <div [title]="() => foo()<error descr="Binding expression cannot contain chained expressions">;</error> bar()"></div>
    <div [title]="() => (foo()<error descr=") expected">;</error> bar()<error descr="Unexpected token ')'">)</error>"></div>
  `
})
export class TestComp {

  sig = signal(1);

  acceptsString(<warning descr="Unused parameter value"><weak_warning descr="TS6133: 'value' is declared but its value is never read.">value</weak_warning></warning>: string): number {
    return 1;
  }

  foo() {}
  bar() {}
}