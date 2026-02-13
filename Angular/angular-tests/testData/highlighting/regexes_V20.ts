import {Component} from '@angular/core';

export interface User {
  name: string,
  pictureUrl: string,
  isHuman?: boolean,
  isRobot?: boolean,
}

@Component({
  selector: 'robot-profile',
  standalone: true,
  template: `
    {{ <error descr="Regular expression literals are supported only in Angular 21.0 and above.">/abc/</error>.<error descr="TS2339: Property 'match' does not exist on type 'RegExp'.">match</error>(user.name) }}
  `
})
export class RobotProfileComponent {
    user!: User
}
