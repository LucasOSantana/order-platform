import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const notBlankValidator: ValidatorFn = (control) => {
  const value = control.value;
  return typeof value === 'string' && value.length > 0 && value.trim().length === 0
    ? { blank: true }
    : null;
};

export const integerValidator: ValidatorFn = (control) => {
  const value = control.value;
  return value === null || value === '' || Number.isInteger(Number(value))
    ? null
    : { integer: true };
};

export function validationMessage(control: AbstractControl): string | null {
  const errors: ValidationErrors | null = control.errors;
  if (!errors) {
    return null;
  }
  if (errors['server']) {
    return errors['server'];
  }
  if (errors['required'] || errors['blank']) {
    return 'Campo obrigatório.';
  }
  if (errors['email']) {
    return 'Informe um e-mail válido.';
  }
  if (errors['maxlength']) {
    return `Máximo de ${errors['maxlength'].requiredLength} caracteres.`;
  }
  if (errors['min']) {
    return `O valor mínimo é ${errors['min'].min}.`;
  }
  if (errors['integer']) {
    return 'Informe um número inteiro.';
  }
  return 'Valor inválido.';
}
