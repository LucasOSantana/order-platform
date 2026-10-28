import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { DEFAULT_CURRENCY_CODE, LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreateOrderRequest } from '../../models/order';
import { OrderForm } from './order-form';

registerLocaleData(localePt);

describe('OrderForm', () => {
  let fixture: ComponentFixture<OrderForm>;
  let host: HTMLElement;
  let emitted: CreateOrderRequest[];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: DEFAULT_CURRENCY_CODE, useValue: 'BRL' },
      ],
    });
    fixture = TestBed.createComponent(OrderForm);
    host = fixture.nativeElement;
    emitted = [];
    fixture.componentInstance.submitted.subscribe((request) => emitted.push(request));
    await fixture.whenStable();
  });

  function type(selector: string, value: string): void {
    const input = host.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    host.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function clickButton(text: string): void {
    const button = [...host.querySelectorAll('button')].find((b) => b.textContent?.includes(text));
    button!.click();
  }

  it('does not submit an invalid form and shows validation errors', async () => {
    await submit();

    expect(emitted).toEqual([]);
    const errors = [...host.querySelectorAll('.field__error')].map((e) => e.textContent?.trim());
    expect(errors).toContain('Campo obrigatório.');
  });

  it('emits a trimmed request without any total field', async () => {
    type('#customerName', '  Ana Souza  ');
    type('#customerEmail', 'ana@email.com');
    type('#item-0-description', ' Mouse ');
    type('#item-0-quantity', '2');
    type('#item-0-unitPrice', '10.5');

    await submit();

    expect(emitted).toEqual([
      {
        customerName: 'Ana Souza',
        customerEmail: 'ana@email.com',
        items: [{ description: 'Mouse', quantity: 2, unitPrice: 10.5 }],
      },
    ]);
  });

  it('adds and removes items and updates the estimated total', async () => {
    clickButton('Adicionar item');
    await fixture.whenStable();

    type('#item-0-quantity', '2');
    type('#item-0-unitPrice', '10');
    type('#item-1-quantity', '1');
    type('#item-1-unitPrice', '5.5');
    await fixture.whenStable();

    expect(host.querySelectorAll('.item-row')).toHaveLength(2);
    expect(host.querySelector('.form-footer__amount')?.textContent).toContain('25,50');

    host.querySelector<HTMLButtonElement>('[aria-label="Remover item 2"]')!.click();
    await fixture.whenStable();

    expect(host.querySelectorAll('.item-row')).toHaveLength(1);
    expect(host.querySelector('.form-footer__amount')?.textContent).toContain('20,00');
  });

  it('shows server-side field errors', async () => {
    fixture.componentRef.setInput('serverErrors', [
      { field: 'customerEmail', message: 'Domínio de e-mail não aceito.' },
    ]);
    await fixture.whenStable();

    expect(host.querySelector('#customerEmail-error')?.textContent).toContain(
      'Domínio de e-mail não aceito.',
    );
  });
});
