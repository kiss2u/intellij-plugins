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
    <div [title]="/abc/.matches('foo') ? 'bar' : 'baz'"></div>
    {{ "foo".match(/abc/O) }}
    {{ "foo".match(/abc/gig) }}
    {{ /abc }}
    {{ "foo".match(/abc/gi) }}
  `
})
export class RobotProfileComponent {
    user!: User
}
