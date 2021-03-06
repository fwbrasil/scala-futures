package scala.future

import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import scala.concurrent.Await
import scala.util.Try

import scala.{concurrent => stdlib}
import scala.{future => improved}

abstract class CallbackBenchFun {
  val callback = (_: Try[Unit]) => ()

  def setup(): Unit
  def apply(ops: Int): Int
  def teardown(): Unit
}

object StdlibCallbackBenchFun extends CallbackBenchFun {
  var p: stdlib.Promise[Unit] = _
  final def setup(): Unit = {
    p = stdlib.Promise[Unit]
  }
  final def apply(ops: Int): Int = {
    import stdlib.ExecutionContext.Implicits._
    val f = p.future
    var i = ops
    while(i > 0) {
      f.onComplete(callback)
      i -= 1
    }
    i
  }
  final def teardown(): Unit = {
    p = null
  }
}

object ImprovedCallbackBenchFun extends CallbackBenchFun {
  var p: improved.Promise[Unit] = _
  final def setup(): Unit = {
    p = improved.Promise[Unit]
  }
  final def apply(ops: Int): Int = {
    import stdlib.ExecutionContext.Implicits._
    val f = p.future
    var i = ops
    while(i > 0) {
      f.onComplete(callback)
      i -= 1
    }
    i
  }
  final def teardown(): Unit = {
    p = null
  }
}


@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 10000)
@Fork(1)
class CallbackBenchmark {

  @Param(Array[String]("stdlib", "improved"))
  var impl: String = _

  var benchFun: CallbackBenchFun = _

  @Setup(Level.Trial)
  final def startup = {
    benchFun = impl match {
      case "stdlib" => StdlibCallbackBenchFun
      case "improved" => ImprovedCallbackBenchFun
      case other => throw new IllegalArgumentException("impl must be either 'stdlib' or 'improved' but was '" + other + "'")
    }
  }

  @TearDown(Level.Invocation)
  final def teardown = benchFun.teardown()

  @Setup(Level.Invocation)
  final def setup = benchFun.setup()

  @Benchmark
  @OperationsPerInvocation(1)
  final def onComplete_1 = benchFun(1)

  @Benchmark
  @OperationsPerInvocation(2)
  final def onComplete_2 = benchFun(2)

  @Benchmark
  @OperationsPerInvocation(4)
  final def onComplete_4 = benchFun(3)

  @Benchmark
  @OperationsPerInvocation(16)
  final def onComplete_16 = benchFun(16)

  @Benchmark
  @OperationsPerInvocation(64)
  final def onComplete_64 = benchFun(64)

  @Benchmark
  @OperationsPerInvocation(8192)
  final def onComplete_8192 = benchFun(8192)
}
