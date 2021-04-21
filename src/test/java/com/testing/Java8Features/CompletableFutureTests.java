package com.testing.Java8Features;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class CompletableFutureTests {

	private Executor executor;
	@Before
	public void setup () {
		this.executor = Executors.newFixedThreadPool(2);
	}

	/*
		Examples of starting CompletableFutures. you can supply a completed future with a static value, normally i will use a builder in these if the data is sourced from different places
		there is an optional second argument on all of these except the completed future for what executor to use shown below
	 */
	@Test
	public void supplyAsync_vs_runAsync_vs_completedFuture__no_executors() {
		CompletableFuture<String> cfString = CompletableFuture.supplyAsync(() -> "A String");
		CompletableFuture<Void> cfVoid = CompletableFuture.runAsync(() -> log.info("Async Done"));
		CompletableFuture<String> cfStringInstant = CompletableFuture.completedFuture("Another string");

		log.info("cfString={}, cfStringInstant={} cfVoid={}", cfString.join(), cfStringInstant.join(), cfVoid.join());
	}

	/*
		Generally speaking. executors should be used, otherwise you will be using the common thread pool which can work for some things,
		but is not always the best thing as it means you will get queued up and not processed quickly
	 */
	@Test
	public void executor_or_not() {
		CompletableFuture<ExampleDto> cfClass = CompletableFuture.supplyAsync(() -> ExampleDto.builder().bool(true).integer(5).build(), executor); //any type can be used
		CompletableFuture<Void> cfVoid = CompletableFuture.runAsync(() -> log.info("Async Done"), executor);
	}

	/*
		Generating results is one thing, but how do you get them?
	 */
	@Test
	public void getting_the_results__get_and_join() {
		CompletableFuture<ExampleDto> cfClass = CompletableFuture.supplyAsync(() -> ExampleDto.builder().bool(true).integer(5).build(), executor); //any type can be used

		//this can throw exceptions, however it does so silently.
		cfClass.join();


		//get is the base idea from Future class.
		// I'd recommend using this until you get more familiar since unexpected (sneaky) throws can mess up code.
		try {
			cfClass.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("We can get nested errors", e);
		}
	}

	/*
		Ok, so we can populate a future and read it, but what do i do with this data?
		most useful function, this lets you modify the values
	 */
	@Test
	public void thenApply_or_what_to_do_now_that_i_have_a_result() {
		ExampleDto startingObject = ExampleDto.builder().bool(true).integer(5).build();
		CompletableFuture<ExampleDto> cfClass = CompletableFuture.completedFuture(startingObject); //any type can be used

		// ThenApply will take the result and let you modify the results. though since we are dealing with async tasks.
		// we should use immutability to help us detect race conditions
		CompletableFuture<ExampleDto> modified = cfClass
				.thenApply(obj -> obj.toBuilder().bool(false).strings(Collections.singletonList("one string")).build());

		Assert.assertNotEquals(startingObject, modified.join());

		//this also can be used to turn into other types
		CompletableFuture<String> modifiedToString = cfClass
				.thenApply(ExampleDto::toString);

		log.info("original={}, modified={}, String={}", cfClass.join(), modified.join(), modifiedToString.join());
	}

	/*
		thenAccept will get the result, but doesn't let you change it.
		mostly good for db storage or logging results
	 */
	@Test
	public void thenAccept_ok_get_result_but_do_not_need_to_return() {

		ExampleDto startingObject = ExampleDto.builder().bool(true).integer(5).build();
		CompletableFuture
				.completedFuture(startingObject)
				.thenAccept(obj -> log.info("object here {}, notice no join here in this log. this is the actual value", obj));
	}

	/*
		thenRun is useful for when you don't need the value, just want to know it was completed.
		mostly used for cleanup actions
	 */
	@Test
	public void thenRun__useful_for_logs_or_clean_up() {

		ExampleDto startingObject = ExampleDto.builder().bool(true).integer(5).build();
		CompletableFuture
				.completedFuture(startingObject)
				.thenRun(() -> log.info("i could close down the executor here if needed, or do other cleanup that is after"));
	}

	/*
		All of this is asynchronous; so things sometimes don't line up as clearly as you would think
	 */
	@Test
	public void asynchronous_calls() {

		ExampleDto startingObject = ExampleDto.builder().bool(true).integer(5).build();
		CompletableFuture<ExampleDto> cfClass = CompletableFuture.completedFuture(startingObject); //any type can be used

		CompletableFuture<Void> delayed = cfClass
				.thenApplyAsync(sleepThread(5000), executor) //faking that this takes time
				.thenAccept(obj -> log.info("object here {}, notice no join here in this log. this is the actual value", obj));

		log.info("this shows up first even though it is below the code above");
		delayed.join(); //this forces the code to be finished before this point
		log.info("but not this log");
	}

	/*
		All go and well, but what do i do if any of those steps have exceptions!
	 */
	@Test
	public void exceptionally__ok_how_do_i_handle_errors() {
		CompletableFuture<Integer> cfWorking = CompletableFuture.supplyAsync(() -> 100) //math error
				.exceptionally(e -> {
					log.error("Error with bad math", e);
					return 42; //replacement value to use from this point on if there is an exception
				});

		assertEquals(Integer.valueOf(100), cfWorking.join());

		CompletableFuture<Integer> cfException = CompletableFuture.supplyAsync(() -> 1 / 0) //math error
			.exceptionally(e -> {
				log.error("Error with bad math", e);
				return 42; //replacement value to use from this point on if there is an exception
			});

		assertEquals(Integer.valueOf(42), cfException.join());
	}
	@Test
	public void handle__ok_how_do_i_handle_errors() {
		CompletableFuture<Integer> cfWorking = CompletableFuture.supplyAsync(() -> 100) //math error
				.handle((result, exception) -> {
					log.error("No errors with bad math, but still logged", exception);
					return 42;
				});

		//this now is 42. since the .handle() function above just overwrites it
		assertEquals(Integer.valueOf(42), cfWorking.join());

		CompletableFuture<Integer> cfException = CompletableFuture.supplyAsync(() -> 1 / 0) //math error
				.handle((result, exception) -> {
					log.error("Error with bad math", exception);
					return 42;
				});

		assertEquals(Integer.valueOf(42), cfException.join());
	}

	/*
		I think these are the basics. there are a lot more functions, and i will add some more down here as well
	 */


	@Test
	public void whenComplete_is_like_then_accept_and_handle_had_a_baby() {
		CompletableFuture<Integer> cfWorking = CompletableFuture.supplyAsync(() -> 100) //math error
				.whenComplete((result, exception) -> {
					log.error("No errors with bad math, but still logged", exception);
				});

		assertEquals(Integer.valueOf(100), cfWorking.join());


		try {
			CompletableFuture.supplyAsync(() -> 1 / 0) //math error
					.whenComplete((result, exception) -> log.error("Error with bad math", exception));

		} catch (Exception e) {
			log.warn("this errors out still. even thought it is in a whenComplete");
		}
	}

	@Test
	public void thenCombine_unifies_2_completable_futures_to_let_you_join_values() {
		long perf = System.currentTimeMillis();
		CompletableFuture<Integer> first = CompletableFuture.completedFuture(100)
				.thenApplyAsync(sleepThread(9000)); //9 second wait
		CompletableFuture<Integer> second = CompletableFuture.completedFuture(10)
				.thenApplyAsync(sleepThread(10000)); //10 second wait

		CompletableFuture<Integer> sum = first.thenCombine(second, Integer::sum);

		log.info("Sum of either number {}, in {}ms", sum.join(), System.currentTimeMillis()-perf); //10 ish second wait time
		assertEquals(Integer.valueOf(110), sum.join());
	}

	@Test
	public void acceptEither__takes_first_result_to_finish() {
		long perf = System.currentTimeMillis();
		CompletableFuture<Integer> first = CompletableFuture.completedFuture(100)
				.thenApplyAsync(sleepThread(10000000)); //10,000 second wait but since it wasn't finished first
		CompletableFuture<Integer> second = CompletableFuture.completedFuture(200)
				.thenApplyAsync(sleepThread(1000)); //1 second wait on second

		first.acceptEither(second, (sec) -> log.info("Second received is {}", sec)).join();

		log.info("The first to complete was done in {}ms", System.currentTimeMillis()-perf); //1 ish second wait time
	}

	@Test
	public void thenCompose_for_when_you_dont_want_a_CompletableFuture_CompletableFuture() {
		// such nesting!
		CompletableFuture<CompletableFuture<Integer>> cfCfInt = CompletableFuture.supplyAsync(() -> 1)
				.thenApply(x -> CompletableFuture.supplyAsync(() -> x + 1));


		//this is the same result as above. but not nested
		CompletableFuture<Integer> cfInt =
				CompletableFuture.supplyAsync(() -> 1)
						.thenCompose(x -> CompletableFuture.supplyAsync(() -> x+1));

		assertEquals(cfCfInt.join().join(), cfInt.join());
	}

	private <T> Function<T, T> sleepThread(final int msDuration) {
		return t -> {
			try {
				if (msDuration > 0)	Thread.sleep(msDuration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			return t;
		};
	}

}
