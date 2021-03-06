/**
 * Copyright (C) 2016 Instacount Inc. (developers@instacount.io)
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.instacount.appengine.counter.service;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.UUID;

import io.instacount.appengine.counter.CounterOperation;
import io.instacount.appengine.counter.CounterOperation.CounterOperationType;
import io.instacount.appengine.counter.data.CounterData;
import io.instacount.appengine.counter.data.CounterData.CounterStatus;
import io.instacount.appengine.counter.data.CounterShardData;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.googlecode.objectify.ObjectifyService;

/**
 * Unit tests for incrementing a counter via {@link ShardedCounterServiceImpl}.
 *
 * @author David Fuelling
 */
public class ShardedCounterServiceShardIncrementTest extends AbstractShardedCounterServiceTest
{

	@Before
	public void setUp() throws Exception
	{
		super.setUp();
	}

	@After
	public void tearDown()
	{
		super.tearDown();
	}

	// /////////////////////////
	// Unit Tests
	// /////////////////////////

	@Test(expected = NullPointerException.class)
	public void testIncrement_NullName() throws InterruptedException
	{
		shardedCounterService.increment(null, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncrement_BlankName() throws InterruptedException
	{
		shardedCounterService.increment("", 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncrement_EmptyName() throws InterruptedException
	{
		shardedCounterService.increment("  ", 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncrement_NegativeIncrement() throws InterruptedException
	{
		shardedCounterService.increment(TEST_COUNTER1, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncrement_ZeroIncrement() throws InterruptedException
	{
		shardedCounterService.increment(TEST_COUNTER1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncrement_CounterIsBeingDeleted() throws InterruptedException
	{
		// Store this in the Datastore to trigger the exception below...
		CounterData counterData = new CounterData(TEST_COUNTER1, 1);
		counterData.setCounterStatus(CounterStatus.DELETING);
		ObjectifyService.ofy().save().entity(counterData).now();

		try
		{
			shardedCounterService.increment(TEST_COUNTER1, 1);
		}
		catch (IllegalArgumentException e)
		{
			assertEquals("Can't mutate the amount of counter '" + TEST_COUNTER1
				+ "' because it's currently in the DELETING state but must be in in the AVAILABLE state!",
				e.getMessage());
			throw e;
		}
	}

	@Test
	public void testIncrement_DefaultNumShards() throws InterruptedException
	{
		shardedCounterService = new ShardedCounterServiceImpl();
		doCounterIncrementAssertions(TEST_COUNTER1, 50);
	}

	@Test
	public void testIncrement_Specifiy1Shard() throws InterruptedException
	{
		shardedCounterService = initialShardedCounterService(1);
		doCounterIncrementAssertions(TEST_COUNTER1, 50);
	}

	@Test
	public void testIncrement_Specifiy3Shard() throws InterruptedException
	{
		shardedCounterService = initialShardedCounterService(3);
		doCounterIncrementAssertions(TEST_COUNTER1, 50);
	}

	@Test
	public void testIncrement_Specifiy10Shards() throws InterruptedException
	{
		shardedCounterService = initialShardedCounterService(10);
		doCounterIncrementAssertions(TEST_COUNTER1, 50);
	}

	// ///////////////////
	// ///////////////////
	// ///////////////////

	/**
	 * Combines serial increment and decrement operations across two counters to ensure that each counter in operated
	 * upon in isolation.
	 */
	@Test
	public void testIncrementDecrementInterleaving()
	{
		shardedCounterService.increment(TEST_COUNTER1, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);
		shardedCounterService.increment(TEST_COUNTER1, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);
		shardedCounterService.increment(TEST_COUNTER1, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);

		assertEquals(BigInteger.valueOf(3L), shardedCounterService.getCounter(TEST_COUNTER1).get().getCount());
		assertEquals(BigInteger.valueOf(4L), shardedCounterService.getCounter(TEST_COUNTER2).get().getCount());

		shardedCounterService.increment(TEST_COUNTER1, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);
		shardedCounterService.increment(TEST_COUNTER1, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);
		shardedCounterService.increment(TEST_COUNTER1, 1);
		shardedCounterService.increment(TEST_COUNTER2, 1);

		assertEquals(BigInteger.valueOf(6L), shardedCounterService.getCounter(TEST_COUNTER1).get().getCount());
		assertEquals(BigInteger.valueOf(8L), shardedCounterService.getCounter(TEST_COUNTER2).get().getCount());

		shardedCounterService.decrement(TEST_COUNTER1, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);
		shardedCounterService.decrement(TEST_COUNTER1, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);
		shardedCounterService.decrement(TEST_COUNTER1, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);

		assertEquals(BigInteger.valueOf(3L), shardedCounterService.getCounter(TEST_COUNTER1).get().getCount());
		assertEquals(BigInteger.valueOf(4L), shardedCounterService.getCounter(TEST_COUNTER2).get().getCount());

		shardedCounterService.decrement(TEST_COUNTER1, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);
		shardedCounterService.decrement(TEST_COUNTER1, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);
		shardedCounterService.decrement(TEST_COUNTER1, 1);
		shardedCounterService.decrement(TEST_COUNTER2, 1);

		assertEquals(BigInteger.ZERO, shardedCounterService.getCounter(TEST_COUNTER1).get().getCount());
		assertEquals(BigInteger.ZERO, shardedCounterService.getCounter(TEST_COUNTER2).get().getCount());
	}

	// Tests counters with up to 15 shards and excerises each shard
	// (statistically, but not perfectly)
	@Test
	public void testIncrement_XShards() throws InterruptedException
	{
		for (int i = 1; i <= 15; i++)
		{
			shardedCounterService = this.initialShardedCounterService(i);

			doCounterIncrementAssertions(TEST_COUNTER1 + "-" + i, 15);
		}
	}

	@Test
	public void testIncrementResult()
	{
		final UUID operationUuid = UUID.randomUUID();

		final CounterOperation result = this.shardedCounterService.increment(TEST_COUNTER1, 1, 1, operationUuid);

		assertThat(result.getAppliedAmount(), is(1L));
		assertThat(result.getOperationUuid(), is(operationUuid));
		assertThat(result.getCounterOperationType(), CoreMatchers.is(CounterOperationType.INCREMENT));
		assertThat(result.getCounterShardDataKey(), CoreMatchers.is(CounterShardData.key(CounterData.key(TEST_COUNTER1), 1)));
		assertThat(result.getCreationDateTime(), is(not(nullValue())));
	}

	// /////////////////////////
	// Private Helpers
	// /////////////////////////

	private void doCounterIncrementAssertions(String counterName, int numIterations) throws InterruptedException
	{
		// ////////////////////////
		// With Memcache Caching
		// ////////////////////////
		for (int i = 1; i <= numIterations; i++)
		{
			shardedCounterService.increment(counterName + "-1", 1);
			assertEquals(BigInteger.valueOf(i), shardedCounterService.getCounter(counterName + "-1").get().getCount());
		}

		// ////////////////////////
		// No Memcache Caching
		// ////////////////////////
		for (int i = 1; i <= numIterations; i++)
		{
			if (this.isMemcacheAvailable())
			{
				this.memcache.clearAll();
			}
			shardedCounterService.increment(counterName + "-2", 1);
			if (this.isMemcacheAvailable())
			{
				this.memcache.clearAll();
			}
			assertEquals(BigInteger.valueOf(i), shardedCounterService.getCounter(counterName + "-2").get().getCount());
		}

		// ////////////////////////
		// Memcache Cleared BEFORE Increment Only
		// ////////////////////////
		for (int i = 1; i <= numIterations; i++)
		{
			// Simulate Capabilities Disabled
			if (this.isMemcacheAvailable())
			{
				this.memcache.clearAll();
			}
			shardedCounterService.increment(counterName + "-3", 1);
			assertEquals(BigInteger.valueOf(i), shardedCounterService.getCounter(counterName + "-3").get().getCount());
		}

		// ////////////////////////
		// Memcache Cleared AFTER Increment Only
		// ////////////////////////
		// Do this with no cache before the get()
		for (int i = 1; i <= numIterations; i++)
		{
			// Simulate Capabilities Disabled
			shardedCounterService.increment(counterName + "-4", 1);
			if (this.isMemcacheAvailable())
			{
				this.memcache.clearAll();
			}
			assertEquals(BigInteger.valueOf(i), shardedCounterService.getCounter(counterName + "-4").get().getCount());
		}

	}

}
