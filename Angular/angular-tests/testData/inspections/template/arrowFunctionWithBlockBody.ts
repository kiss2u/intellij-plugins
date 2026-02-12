// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import {Component} from '@angular/core';

<error descr="TS2354: This syntax requires an imported helper but module 'tslib' cannot be found.">@Component({
  selector: 'my-comp',
  template: `
  {{ () => <error descr="Multi-line arrow functions are not supported. If you meant to return an object literal, wrap it with parentheses.">{a: 12}</error> }}
  {{ () => <error descr="Multi-line arrow functions are not supported. If you meant to return an object literal, wrap it with parentheses.">{}</error> }}
  {{ () => <error descr="Multi-line arrow functions are not supported. If you meant to return an object literal, wrap it with parentheses.">{<error descr="TS2339: Property 'return' does not exist on type 'MyComponent'.">return</error><error descr="Newline or semicolon expected"> </error><error descr="TS1005: ';' expected.">12</error>}</error> }}
  `,
})</error>
export class MyComponent {

}