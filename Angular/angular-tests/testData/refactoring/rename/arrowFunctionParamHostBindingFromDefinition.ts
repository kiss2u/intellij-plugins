import {Directive} from '@angular/core';

@Directive({
  host: {
    '[attr.no-context]': '((a<caret>, b) => a / b)(5, 10) + a',
    '[attr.with-context]': 'a',
  }
})
export class TestDir {
  a = 1;
}