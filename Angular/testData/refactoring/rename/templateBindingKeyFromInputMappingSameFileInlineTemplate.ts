import {Component, Directive, Input} from '@angular/core';

@Directive({
    selector: '[appClicks]',
    inputs: ['app: app<caret>ClicksParam'],
    standalone: true,
})
export class AppClicksDirective {
    app!: string

    @Input()
    appClicks!: boolean
}

@Component({
    selector: 'test',
    template: `
      <div *appClicks="true; param: 'foo'"></div>
      <ng-template [appClicks]="true" [appClicksParam]="'foo'"></ng-template>/
    `,
    imports: [
        AppClicksDirective
    ]
})
export class ChipComponent {

}
