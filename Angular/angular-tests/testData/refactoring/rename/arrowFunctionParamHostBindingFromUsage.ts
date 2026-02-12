import {Directive} from '@angular/core';

@Directive({
  host: {
    '[attr.no-context]': '((a, b) => a<caret> / b)(5, 10) + a',
    '[attr.with-context]': 'a',
  }
})
export class TestDir {
  a = 1;
}