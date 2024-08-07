// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import {Component} from "@angular/core"

@Component({
  template: `
    {{ privateUsed }}
    {{ protectedUsed }}
    {{ publicUsed }}
  `
})
export class MyComponent {
  protected privateUsed: string;
  private privateUnused: string;

  protected protectedUsed: string;
  protected protectedUnused: string;

  publicUsed: string;
  publicUnused: string;


}
