package com.lmi.engine.worker.util.ignite

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.LongAdder

import com.lmi.engine.EngineThreads
import org.apache.ignite.lang.{IgniteFuture, IgniteInClosure}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object BatchLatchFactory {
	
	def newOptional(useLatch: Boolean): Option[BatchLatch] = {
		if(useLatch)
			Some(new BatchLatch())
		else
			None
	}
	
}

//Similar to a CountdownLatch, but the count is dynamic based on added IgniteFutures
//MUST BE USED as follows: call listenForFuture any number of times, then call await once
class BatchLatch() {
	
	private val counter = new LongAdder()
	private val lock = new Semaphore(1)
	private var onComplete: mutable.ListBuffer[() => Unit] = ListBuffer()
	
	def listenForFuture[T](f: IgniteFuture[T]): Unit = {
		increment()
		
		f.listen(new IgniteInClosure[IgniteFuture[T]] {
			override def apply(e: IgniteFuture[T]): Unit = {
				decrement()
			}
		})
	}
	
	private def increment() = {
		counter.increment()
		lock.tryAcquire()
	}
	
	private def decrement() = {
		counter.decrement()
		if(counter.sum() == 0) {
			invokeCompletion()
			lock.release() //Blindly releasing is okay
		}
	}
	
	private def invokeCompletion() = {
		val toCall = onComplete
		onComplete = ListBuffer()
		toCall.foreach(l => l())
	}
	
	def listenForFuture[T](f: Future[T]): Unit = {
		//Use coocurPool since CoocurService is only caller of this method (so far...?)
		implicit val con = EngineThreads.coocurrencePoolContext
		
		increment()
		
		f.foreach(_ => {
			decrement()
		})
	}
	
	//Not thread-safe
	def setOnComplete(lambda: () => Unit): Unit = {
		increment()
		onComplete += lambda
		decrement()
	}
	
	def await(): Unit = {
		lock.acquire()
		lock.release()
	}
	
}
