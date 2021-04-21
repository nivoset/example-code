package com.testing.Java8Features;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class LocalDateExampleService {

    private final Clock clock;

    //used for actual building in spring
    @Autowired
    LocalDateExampleService() {
        this(Clock.systemUTC());
    }

    //used for testing.
    LocalDateExampleService(Clock clock) {
        this.clock = clock;
    }

    public LocalDate getLocalDate() {
        //all instances in this class should use the clock.
        return LocalDate.now(clock);
    }
}
