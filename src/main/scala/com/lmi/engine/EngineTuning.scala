package com.lmi.engine

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import scala.concurrent.ExecutionContext

case class BasicProductionTuning() extends EngineTuning {}

case class TestTuning() extends EngineTuning(
	OBJ_EVICTION_DELAY_MS = 1,
	SIMILARITY_TRIM_DELAY_MS = 1,
	COOCURRENCE_UPDATE_DELAY_MS = 1,
	HISTORY_UPDATE_DELAY_MS = 1,
	EMPTY_DEQUEUE_SLEEP_MS = 1,
	MAX_EXPECTED_PARALLEL_HISTORY_UPDATES = 100) {}

/* Key polling params for tuning */
class EngineTuning(
	                  //Delay between calls to evict old objects
	                  //Increase to give CPU/Net to other processes at cost of memory
	                  val OBJ_EVICTION_DELAY_MS: Long = 1000 * 10,
	
	                  //Delay from updating a HistoryEntry to triggering futher pipelines
	                  //Increase this to avoid duplicate HistoryEntrys at cost of latency
	                  val HISTORY_UPDATE_DELAY_MS: Long = 10,
	
	                  //Delay from updating a CoocurrencePair to triggering futher pipelines
	                  //Incease to de-duplicate processing of hot items at cost of latency
	                  val COOCURRENCE_UPDATE_DELAY_MS: Long = 10,
	
	                  //Delay from updating a SimilarityScore to triggering trimming of excess scores
	                  //Increase to give CPU/Net to other processes at cost of higher memory consumption
	                  val SIMILARITY_TRIM_DELAY_MS: Long = 1000 * 30,
	
	                  //Polling delay after processing outgoing similarity changes
	                  val OUTPUT_STREAMER_POLL_DELAY_MS: Long = 10,
	
	                  //Max number of ParsedEvents to pull out per local ParsedEvents pop
	                  //Increasing will require more memory while decreasing lowers throughput
	                  val MAX_DEQUEUES_PER_RUN: Long = 10000,
	
	                  //Time to wait after polling for ParsedEvents but getting nothing
	                  //Increase to give CPU/Net to other processes at cost of wake-up time for new events
	                  val EMPTY_DEQUEUE_SLEEP_MS: Long = 20,
	
	                  val MAX_EXPECTED_PARALLEL_HISTORY_UPDATES: Long = 1000
                  ) extends Serializable {}

object EngineThreads {
	
	val THREAD_PRIORITY_HIGH: Int = Runtime.getRuntime.availableProcessors
	val THREAD_PRIORITY_MEDIUM: Int = Math.max(Runtime.getRuntime.availableProcessors / 2, 1)
	val THREAD_PRIORITY_LOW: Int = 1
	
	val dequeuePool = newScheduledPool(THREAD_PRIORITY_HIGH, "QueuedEventPuller")
	val historyPool = newScheduledPool(THREAD_PRIORITY_HIGH, "HistoryUpdatedHandler")
	val coocurrencePool = newScheduledPool(THREAD_PRIORITY_HIGH, "CoocurrenceUpdatedHandler")
	val similarityPool = newScheduledPool(THREAD_PRIORITY_HIGH, "SimilarityTrimmer")
	val coocurrencePoolContext = ExecutionContext.fromExecutor(coocurrencePool)
	val outputStreamPool = newScheduledPool(THREAD_PRIORITY_HIGH, "OutputStreamer")
	val evictionPool = newScheduledPool(THREAD_PRIORITY_LOW, "EvictionHandler")
	
	private def newScheduledPool(poolSize: Int, poolName: String): ScheduledExecutorService = {
		Executors.newScheduledThreadPool(poolSize, new ThreadFactory {
			override def newThread(r: Runnable): Thread = new Thread(r, poolName)
		})
	}
	
}
