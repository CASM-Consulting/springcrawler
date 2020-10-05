package com.casm.acled.crawler.management;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Primary
@Component
public class ClockTimeProvider implements TimeProvider{

    @Override
    public LocalDateTime getTime() {
        return LocalDateTime.now();
    }
}
