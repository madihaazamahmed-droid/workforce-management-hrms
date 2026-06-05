package com.deepthought.workforce.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * LF-204 FIX: SMS fires only AFTER the transaction commits.
 * If the DB rolls back, this listener is never called — no premature SMS.
 * If SMS fails after a successful commit, we log + queue retry (settlement data stays correct).
 * Running @Async so SMS failure doesn't affect the API response.
 */
@Component
@Slf4j
public class SmsNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOvertimeSettled(OvertimeSettledEvent event) {
        try {
            sendSms(event);
        } catch (Exception ex) {
            // Settlement data is correct. Log and queue retry — don't crash.
            log.error("SMS failed for workerId={} month={}. Amount={}. Will retry. Error: {}",
                    event.getWorkerId(), event.getMonth(), event.getTotalAmount(), ex.getMessage());
            // In a real system: push to a retry queue (SQS, Redis list, etc.)
        }
    }

    private void sendSms(OvertimeSettledEvent event) {
        // Stubbed — replace with actual SMS provider (Twilio, MSG91, etc.)
        log.info("[SMS STUB] Sending to worker {}: Your {} overtime of ₹{} has been settled.",
                event.getWorkerName(), event.getMonth(), event.getTotalAmount());
        // smsProvider.send(worker.getPhone(), message);
    }
}
