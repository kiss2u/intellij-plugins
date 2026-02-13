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
    <!-- should report invalid regular expression flag -->
    {{ "foo".match(/abc/<error descr=", or ) expected">O</error><error descr="Unexpected token ')'">)</error> }}
    
    <!-- should report unterminated regex -->
    {{ <error descr="TS1161: Unterminated regular expression literal.">/abc</error> }}
  `
})
export class RobotProfileComponent {
    <warning descr="Unused field user">user</warning>!: User
}
