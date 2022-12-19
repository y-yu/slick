package slick.test.jdbc.hikaricp

import java.util.concurrent.{BlockingQueue, RejectedExecutionHandler, ThreadFactory, ThreadPoolExecutor, TimeUnit}
import slick.util.AsyncExecutor.PrioritizedRunnable
import slick.util.{AsyncExecutor, ManagedArrayBlockingQueue}

import scala.concurrent.duration.DurationInt

object ExceptionInBeforeExecutorTest {
  val y = AsyncExecutor(name = "aaaaa",
    minThreads = 2,
    maxThreads = 2,
    queueSize = 2,
    maxConnections = 2,
    keepAliveTime = 2.minutes,
    registerMbeans = true)

  val x = new ThreadPoolExecutor(
    2,
    2,
    10000L,
    TimeUnit.MILLISECONDS,
    new ManagedArrayBlockingQueue(2, 2).asInstanceOf[BlockingQueue[Runnable]],
    new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        println("newThread")
        new Thread(r)
      }
    },
    new RejectedExecutionHandler {
      override def rejectedExecution(r: Runnable, executor: ThreadPoolExecutor): Unit = {
        (new Error("rejected")).printStackTrace()
      }
    }
  ) {
    override def beforeExecute(t: Thread, r: Runnable): Unit = {
      println("beforeExecute")
      //throw new Exception("exception from beforeExecute")
    }
  }

  def main(args: Array[String]): Unit = {
    try {
      (1 to 10).foreach { n =>
        /*
        val runnable = PrioritizedRunnable(
          AsyncExecutor.WithConnection,
          x => {
            x.f()
            println(s"hello $n")
          }
        )
        */

        println(s"call execute: $n")
        val runnable = y.prioritizedRunnable(
          AsyncExecutor.WithConnection,
          x => {
            println(s"hello $n")
            x.f()
          }
        )
        x.execute(runnable)
      }
      Thread.sleep(1000)
      println("finish calling `execute`")
      Thread.sleep(1000)
    } catch {
      case e: Throwable =>
        println("caught in main thread")
        e.printStackTrace()
        throw e
    } finally {
      println("start to shutdown")
      x.shutdown()
      println("end to shutdown")
    }
  }
}