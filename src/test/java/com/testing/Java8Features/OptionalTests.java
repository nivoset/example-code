package com.testing.Java8Features;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.Assert.*;

@Slf4j
public class OptionalTests {

    /*
        Optionals help us avoid writing null checks on code.
        you don't want to use these in data models though. these are mainly for returns from functions.
     */
    @Test
    public void optional_of__basics() {
        //if you know you have data here
        Optional<String> optStr = Optional.of("String");

        //if you maybe do not have a value (function maybe can return a null, or class may not be populated
        Optional<String> optStrNull = Optional.ofNullable(null);

        //always empty optional
        Optional<String> empty = Optional.empty();
    }

    @Test
    public void avoidNullPointerExceptions__this_doesnt_crash() {
        Optional.empty().map(v -> v.equals("test"));
    }

    @Test
    public void gettingResults() {
        Optional<String> fulfilledOpt = Optional.of("TEST");

        //see the .get() is flagged yellow? this may cause an exception since we didn't check it
        fulfilledOpt.get();

        //this check handles it if using into if statements (this is yellow because it is always true currently)
        if (fulfilledOpt.isPresent()) {
            String result = fulfilledOpt.get();
            assertEquals(result, "TEST");
        }

        //however, you can also just as for something or else a default
        String orElseString = fulfilledOpt.orElse("Replacement if null");

        //or throw a custom exception if null
        fulfilledOpt.orElseThrow(NullPointerException::new);

        //or run a function to get results (to avoid computing if not needed)
        //this is the one i use the most, since this saves cycles if not needed
        String stringOrDateString = fulfilledOpt.orElseGet(() -> LocalDate.now().toString()); //only runs _if_ the result is null

        //also, you can just run a Consumer of the value
        //i use this the most. in Java 11 there is ifPresentOrElse where you pass 2 functions, one for if it exists, one for if it doesn't
        fulfilledOpt.ifPresent(str -> log.info("result string {}", str));

    }

    //but if only null checks, what use is it?
    //how about mapping results
    //we can use Function<?, ?> functions to map from 1 result to another
    @Test
    public void optional_map_convertNumber_to_string() {
        Optional<String> strNumber = Optional.of(50).map(String::valueOf);
        Optional<Integer> backToInt = strNumber.map(Integer::valueOf);

        log.info("both work and exist string = {}, number={}", strNumber.orElse(null), backToInt.orElse(null));

        assertEquals(Integer.valueOf(50), backToInt.orElse(null));
        assertEquals("50", strNumber.orElse(null));
    }

    //we can also filter results to give us artificial empty optional if needed, useful if you want to remove certain cases
    // this uses the Predicate<?> type of function.
    @Test
    public void optional_filter__filter_unwanted_results() {
        Optional<Integer> exists = Optional.of(100).filter(i -> true);
        Optional<Integer> nowNull = Optional.of(100).filter(i -> false);

        log.info("exists={} nowNull={}", exists.orElse(null), nowNull.orElse(null));

        assertTrue(exists.isPresent());
        assertFalse(nowNull.isPresent());
    }

    /*
        optionals however, do not handle exceptions without their own try/catch in the functions
        generally i write functions to return null on exceptions and log in the function what happened.
        this lets me carry the optional to the end and handle how i want to deal with not having a value there.
     */
    @Test(expected =  ArithmeticException.class)
    public void optional_exceptions() {
        Optional<Integer> exists = Optional.of(100).map(divideBy(10));
        assertEquals(Integer.valueOf(10), exists.orElse(null));

        Optional<Integer> throwsException = Optional.of(100).map(divideBy(0));

    }

    private Function<Integer, Integer> divideBy(Integer divisor) {
        return num -> num/divisor;
    }
}
