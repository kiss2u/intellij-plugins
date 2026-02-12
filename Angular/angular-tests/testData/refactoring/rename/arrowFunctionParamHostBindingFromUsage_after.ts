import {Directive} from '@angular/core';

@Directive({
  host: {
    '[attr.no-context]': '((newName, b) => newName<caret> / b)(5, 10) + a',
    '[attr.with-context]': 'a',
  }
})
export class TestDir {
  a = 1;
}