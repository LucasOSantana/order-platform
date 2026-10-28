import { Order, OrderItem, OrderStatus, OrderStatusChange } from '../../models/order';
import { calculateLineTotal, calculateOrderTotal } from '../../utils/order-total';

const CUSTOMERS: readonly [name: string, email: string][] = [
  ['Ana Souza', 'ana.souza@email.com'],
  ['Bruno Lima', 'bruno.lima@email.com'],
  ['Carla Mendes', 'carla.mendes@email.com'],
  ['Diego Rocha', 'diego.rocha@email.com'],
  ['Eduarda Alves', 'eduarda.alves@email.com'],
  ['Felipe Costa', 'felipe.costa@email.com'],
  ['Gabriela Nunes', 'gabriela.nunes@email.com'],
  ['Henrique Dias', 'henrique.dias@email.com'],
  ['Isabela Ferreira', 'isabela.ferreira@email.com'],
  ['João Pereira', 'joao.pereira@email.com'],
];

const PRODUCTS: readonly [description: string, unitPrice: number][] = [
  ['Notebook 14"', 4299.9],
  ['Mouse sem fio', 89.9],
  ['Teclado mecânico', 349],
  ['Monitor 27"', 1599],
  ['Headset com microfone', 259.9],
  ['Cabo USB-C 2m', 39.9],
  ['Webcam Full HD', 219],
  ['Cadeira ergonômica', 1290],
  ['SSD 1TB', 499.9],
  ['Hub USB 7 portas', 129.9],
];

const FAILURE_REASONS = [
  'Item sem estoque disponível',
  'Endereço de entrega não atendido',
  'Pagamento recusado na validação antifraude',
];

const HOUR_MS = 60 * 60 * 1000;
const SEED = 2026;

export function createSeedOrders(now = new Date(), count = 42): Order[] {
  const random = createRandom(SEED);
  const pick = <T>(list: readonly T[]): T => list[Math.floor(random() * list.length)];
  let itemId = 1;

  return Array.from({ length: count }, (_, index) => {
    const createdAt = new Date(now.getTime() - index * 17 * HOUR_MS - random() * 5 * HOUR_MS);
    const status: OrderStatus =
      index < 2 ? 'RECEIVED' : index < 4 ? 'PROCESSING' : random() < 0.18 ? 'FAILED' : 'COMPLETED';
    const failureReason = status === 'FAILED' ? pick(FAILURE_REASONS) : null;
    const [customerName, customerEmail] = pick(CUSTOMERS);

    const products = [...PRODUCTS]
      .sort(() => random() - 0.5)
      .slice(0, 1 + Math.floor(random() * 4));
    const items: OrderItem[] = products.map(([description, unitPrice]) => {
      const quantity = 1 + Math.floor(random() * 3);
      return {
        id: itemId++,
        description,
        quantity,
        unitPrice,
        lineTotal: calculateLineTotal({ quantity, unitPrice }),
      };
    });

    const statusHistory = buildStatusHistory(status, createdAt, failureReason);
    const lastChange = statusHistory[statusHistory.length - 1];

    return {
      id: createUuid(random),
      customerName,
      customerEmail,
      status,
      totalAmount: calculateOrderTotal(items),
      items,
      failureReason,
      attempts: status === 'RECEIVED' ? 0 : status === 'FAILED' ? 3 : 1,
      createdAt: createdAt.toISOString(),
      updatedAt: lastChange.changedAt,
      processedAt: status === 'COMPLETED' || status === 'FAILED' ? lastChange.changedAt : null,
      statusHistory,
    };
  });
}

function buildStatusHistory(
  status: OrderStatus,
  createdAt: Date,
  failureReason: string | null,
): OrderStatusChange[] {
  const at = (offsetMs: number) => new Date(createdAt.getTime() + offsetMs).toISOString();
  const history: OrderStatusChange[] = [
    { fromStatus: null, toStatus: 'RECEIVED', reason: null, changedAt: at(0) },
  ];

  if (status !== 'RECEIVED') {
    history.push({
      fromStatus: 'RECEIVED',
      toStatus: 'PROCESSING',
      reason: null,
      changedAt: at(1500),
    });
  }
  if (status === 'COMPLETED' || status === 'FAILED') {
    history.push({
      fromStatus: 'PROCESSING',
      toStatus: status,
      reason: failureReason,
      changedAt: at(6500),
    });
  }
  return history;
}

function createRandom(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state + 0x6d2b79f5) | 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function createUuid(random: () => number = Math.random): string {
  const hex = Array.from({ length: 32 }, () => Math.floor(random() * 16).toString(16));
  hex[12] = '4';
  hex[16] = ((parseInt(hex[16], 16) & 0x3) | 0x8).toString(16);
  const h = hex.join('');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}
