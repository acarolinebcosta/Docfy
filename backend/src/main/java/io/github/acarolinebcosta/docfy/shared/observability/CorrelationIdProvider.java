package io.github.acarolinebcosta.docfy.shared.observability;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdProvider {

    public String currentCorrelationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}