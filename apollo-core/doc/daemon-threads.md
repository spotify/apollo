# Daemon threads

Daemon threads have the problem that they are not stack-managed by the
JVM at all. This means that:

  * The threads are instantly aborted at JVM shutdown.
  * `finally` blocks are completely ignored and not executed.
  * JVM shutdown hooks registered on the thread will not be executed.

However, with normal threads and `threadPool.shutdownNow()`, the
following happens:

  * The interrupted flag is set on all the threads in the pool, which
    means that the next blocking operation throws
    `InterruptedException` on all the threads. The thread gets a
    chance to do orderly clean-up before it re-throws the
    `InterruptedException` or re-sets the interrupted flag.
  * When the exception propagates up the stack, all the `finally`
    blocks are executed.
  * Shutdown hooks will be run that are registered on the thread,

In other words, with the following code, `"Hello, World!"` is never
printed:

```java
public class Main {
  public static void main(String... args) {
    Thread t = new Thread(new MyRunnable());
    t.setDaemon(true);
    t.start();
    // Note: program ends, which means that the daemon thread `t`
    // will be stopped
  }
}

class MyRunnable implements Runnable {
  @Override
  public void run() {
    try {
      Thread.sleep(10000000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      System.out.println("Hello, World!");
    }
  }
}
```

This is not so bad if your background threads only do things like
logging or setting flags on other threads. However, you may need to
do things like log reporting, connection pooling,
and other such things that rely on sane shutdown behavior (closing
files and sockets, sending "disconnect" messages, reporting leader
re-election), and that shutdown behavior never happens if you use
daemon threads.

Sources:

  * <http://docs.oracle.com/javase/specs/jls/se7/html/jls-12.html>
  * <http://stackoverflow.com/questions/8663107/how-does-the-jvm-terminate-daemon-threads-or-how-to-write-daemon-threads-that-t>
