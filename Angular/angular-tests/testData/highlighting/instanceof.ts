// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import { Component} from '@angular/core';

export class Bar {
}

@Component({
  selector: 'app-root',
  template: `
      <div>
          {{ <error descr="TS2358: The left-hand side of an 'instanceof' expression must be of type 'any', an object type or a type parameter.">"foo"</error> instanceof <error descr="TS2339: Property 'String' does not exist on type 'AppComponent'.">String</error> }}
          {{ data instanceof Bar }}
          {{ bar instanceof Bar }}
      </div>
  `,
  standalone: true
})
export class AppComponent {

  data: any

  <error descr="TS2564: Property 'bar' has no initializer and is not definitely assigned in the constructor.">bar</error>: Bar;
  <error descr="TS2564: Property 'Bar' has no initializer and is not definitely assigned in the constructor.">Bar</error>: typeof Bar;
}