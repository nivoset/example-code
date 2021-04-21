package com.testing.Java8Features;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Slf4j
public class LocalDateTest {

    private Clock staticClock;
    private Clock systemClock;
    private LocalDateExampleService mockedLocalDateExampleService;
    private LocalDateExampleService realLocalDateExampleService;



    @Before
    public void setup () {
        this.staticClock = Clock.fixed(LocalDate.of(
                    2020,
                    Month.APRIL, //can use a number for this, however i find this to be easier
                    1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC),
                ZoneId.of("UTC"));
        //this is what you would override to use in any class using a clock.
        this.systemClock = Clock.systemDefaultZone();

        this.mockedLocalDateExampleService = new LocalDateExampleService(this.staticClock);
        this.realLocalDateExampleService = new LocalDateExampleService(this.systemClock); //could be called with no arguments
    }

    /*
    Generating a date is from the factory function of "now"
    this takes an optional clock argument that helps make testing 100% easier
     */
    @Test
    public void LocalDate_new_instance_from_clock_vs_new_instance() {
        LocalDate fromStatic = LocalDate.now(staticClock);
        System.out.println(fromStatic.format(DateTimeFormatter.ISO_LOCAL_DATE));

        assertEquals("2020-04-01", fromStatic.format(DateTimeFormatter.ISO_LOCAL_DATE));
        LocalDate now = LocalDate.now();
        log.info("now={} fromStatic={}", now, fromStatic);

        assertNotEquals("2020-04-01", now.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Test
    public void example__UseInAClass() {
        LocalDate fakeNow = mockedLocalDateExampleService.getLocalDate();
        LocalDate realNow = realLocalDateExampleService.getLocalDate();
        assertNotEquals(fakeNow, realNow);
    }

    @Test
    public void noMutationAllowed_changes_are_made_in_new_instances() {
        LocalDate now = LocalDate.now(staticClock);
        LocalDate startOfNextMonth = now.plusMonths(1).withDayOfMonth(5);

        assertEquals("2020-04-01", now.format(DateTimeFormatter.ISO_DATE));
        assertEquals("2020-05-05", startOfNextMonth.format(DateTimeFormatter.ISO_DATE));
    }

    @Test
    public void canBeConvertedToA_LocalDateTime_or_ZonedDateTime_Easily() {
        LocalDateTime now = LocalDate.now(staticClock)
                .atStartOfDay()
                .withHour(5)
                .withMinute(30)
                .withSecond(57);
        assertEquals("2020-04-01T05:30:57", now.format(DateTimeFormatter.ISO_DATE_TIME));

        //optional argument of ZoneId turns it into ZonedDateTime;
        ZonedDateTime notQuiteNow = LocalDate.now(staticClock)
                .atStartOfDay(ZoneId.of("America/New_York"));
        assertEquals("2020-04-01T00:00:00-04:00[America/New_York]", notQuiteNow.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    @Test
    public void whereIsThisDateManipulationUseful() {
        LocalDate lastYear = LocalDate.now(staticClock).minusYears(1);
        assertEquals("2019-04-01", lastYear.format(DateTimeFormatter.ISO_DATE));
    }

    @Test
    public void comparingDates() {
        LocalDate now = LocalDate.now(systemClock);
        LocalDate fromStatic = LocalDate.now(staticClock);
        LocalDate anotherStaticTime = LocalDate.now(staticClock);

        assertEquals(1, now.compareTo(fromStatic)); //now is more recent than from static
        assertEquals(0, fromStatic.compareTo(anotherStaticTime)); //both static items are made from static clock. so they are the same
        assertEquals(-1, fromStatic.compareTo(now)); //from static is before now
    }
}
