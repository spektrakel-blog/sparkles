import { async, TestBed } from '@angular/core/testing';
import { ComponentDemosModule } from './component-demos.module';

describe('ComponentDemosModule', () => {
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ComponentDemosModule]
    }).compileComponents();
  }));

  it('should create', () => {
    expect(ComponentDemosModule).toBeDefined();
  });
});
