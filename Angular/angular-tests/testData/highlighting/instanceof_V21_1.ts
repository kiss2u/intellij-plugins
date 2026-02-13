// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import { Component} from '@angular/core';

@Component({
  selector: 'app-root',
  template: `
      <div>
          {{ <error descr="TS2358: The left-hand side of an 'instanceof' expression must be of type 'any', an object type or a type parameter.">"foo"</error> <error descr="The instanceof operator is supported only in Angular 21.2 and above.">instanceof</error> <error descr="TS2339: Property 'String' does not exist on type 'AppComponent'.">String</error> }}
      </div>
  `,
  standalone: true
})
export class AppComponent {

  <warning descr="Unused field data">data</warning>: any

}