package cep_backend.service;

import cep_backend.mapper.TradeOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TradeOrderTimeoutScheduler {
    private static final Logger log = LoggerFactory.getLogger(TradeOrderTimeoutScheduler.class);
    private static final int PENDING_PAYMENT_TIMEOUT_MINUTES = 15;

    private final TradeOrderRepository tradeOrderRepository;

    public TradeOrderTimeoutScheduler(TradeOrderRepository tradeOrderRepository) {
        this.tradeOrderRepository = tradeOrderRepository;
    }

    @Scheduled(fixedDelay = 30_000L, initialDelay = 15_000L)
    public void cancelTimedOutPendingPaymentOrders() {
        int affected = tradeOrderRepository.cancelTimedOutPendingOrders(PENDING_PAYMENT_TIMEOUT_MINUTES);
        if (affected > 0) {
            log.info("Auto-cancelled {} pending-payment order(s) over {} minute(s)",
                    affected,
                    PENDING_PAYMENT_TIMEOUT_MINUTES);
        }
    }
}
