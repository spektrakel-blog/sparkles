import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewChecked,
  ViewChild,
  AfterViewInit,
  Inject
} from '@angular/core';
import { ComponentFactoryResolver, ViewContainerRef, ComponentRef } from '@angular/core';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs/Subscription';
import { MessageService } from '../messages/message.service';
import { MessageTypes, LaunchMessage } from '../messages/message.interfaces';
import { UrlSerializer } from '../url/url-serializer.service';
import { EntryPoint, ENTRY_POINTS, isAppLaunch } from '../app-launcher.interfaces';

@Component({
  selector: 'u-app-outlet',
  template: `<ng-container #outlet></ng-container>`
})
export class GuestComponent implements OnInit, OnDestroy, AfterViewInit, AfterViewChecked {
  @ViewChild('outlet', { read: ViewContainerRef })
  outlet: ViewContainerRef;

  private sub: Subscription[] = [];

  private componentRef: ComponentRef<any>;

  constructor(
    private messages: MessageService,
    private resolver: ComponentFactoryResolver,
    private urlSerializer: UrlSerializer,
    @Inject(ENTRY_POINTS) private entryPoints: EntryPoint[]
  ) {}

  ngOnInit() {}

  ngOnDestroy() {
    if (this.componentRef) {
      this.componentRef.destroy();
    }

    this.sub.forEach(sub => sub.unsubscribe());
  }

  ngAfterViewInit() {
    const sub = this.messages.messages$
      .pipe(
        filter(msg => {
          return msg.type === MessageTypes.LAUNCH;
        })
      )
      .subscribe(msg => this.onHandleLaunch(msg as LaunchMessage));
    this.sub.push(sub);
  }

  ngAfterViewChecked() {}

  onHandleLaunch(msg: LaunchMessage) {
    const url = msg.payload;

    const entryPoint = this.entryPoints.find(ep => ep.path === url.entryPoint);
    const componentFactory = this.resolver.resolveComponentFactory(entryPoint.component);

    // Clean up old component
    if (this.componentRef) {
      this.componentRef.destroy();
    }

    if (!this.outlet) {
      alert('no outlet!');
    }

    // Create new component
    this.componentRef = this.outlet.createComponent(componentFactory, this.outlet.length);
    const component = this.componentRef.instance;
    if (isAppLaunch(component)) {
      component.onAppLaunch(url);
    }
    this.componentRef.changeDetectorRef.detectChanges();
  }
}
