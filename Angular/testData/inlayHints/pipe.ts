import {Component} from "@angular/core";
import {PercentPipe} from "@angular/common";

@Component(<hint text="obj(59120,59123):"/>{
  selector: 'app-test',
  standalone: true,
  imports: [PercentPipe],
  template: `
    <main class="main">
      <div [title]=" <hint text="value(122803,122808):"/>1 + 96.5 + 12 | percent : <hint text="digitsInfo(122827,122837):"/>'4' : <hint text="locale(122848,122854):"/>'pl' "></div>
    </main>
  `,
})
export class TestComponent {

}
