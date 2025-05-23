// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import {Component} from '@angular/core';
import {SlotsComponent} from "./componentClassUsagesInTemplates"

@Component({
  selector: 'apps-root',
  templateUrl: './slots.test.component.html',
  standalone: true,
  imports:[
    SlotsComponent
  ]
})
export class SlotsTestComponent {
}
