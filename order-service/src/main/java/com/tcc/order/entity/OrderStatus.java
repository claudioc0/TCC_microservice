package com.tcc.order.entity;

/**
 * Fluxo obrigatorio (linear, sem retrocesso):
 *   PENDENTE -> CONFIRMADO -> ENVIADO -> ENTREGUE
 *
 * CANCELADO e um estado terminal acessivel apenas a partir de PENDENTE ou CONFIRMADO.
 */
public enum OrderStatus {

    PENDENTE {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == CONFIRMADO || next == CANCELADO;
        }
    },
    CONFIRMADO {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == ENVIADO || next == CANCELADO;
        }
    },
    ENVIADO {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == ENTREGUE;
        }
    },
    ENTREGUE {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return false;
        }
    },
    CANCELADO {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(OrderStatus next);
}
