package com.dlr.examples.applicationinterrupts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        // pretend the thread shuts down well within docker stop's timeout
        threadWithoutInterrupt(1000);
        threadInterruptedOnShutdown(1000);
        futureFromExecutorService(1000);

        // pretend that shut down takes longer than docker stop's timeout to shutdown
        threadWithoutInterrupt(15000);
        threadInterruptedOnShutdown(15000);
        futureFromExecutorService(15000);
    }

    private static void threadWithoutInterrupt(long timeout) {
        // this never prints the loop interrupt statement.
        String name = "threadWithoutInterrupt wait " + timeout;
        Thread t = new Thread(new RunnableLoop(name, timeout));
        t.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(name + " thread: shutting down");
        }));
    }

    private static void threadInterruptedOnShutdown(long timeout) {
        // Setting interrupt doesn't cause the application to wait for the thread to exit.
        // The statements inside the interrupt block in the loop may or may not be executed.
        String name = "threadInterruptedOnShutdown wait " + timeout;
        Thread t = new Thread(new RunnableLoop(name, timeout));
        t.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(name + " thread: setting interrupt");
            t.interrupt();
            System.out.println(name + " thread: shutting down");
        }));
    }

    private static void futureFromExecutorService(long timeout) {
        // the executor service submit method allows us to get a handle on the thread
        // via a future and set the interrupt in the shutdown hook
        String name = "futureFromExecutorService wait " + timeout;
        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<?> app = service.submit(new RunnableLoop(name, timeout));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(name + " thread: setting interrupt");
            app.cancel(true);
            service.shutdown();

            try {
                // give the thread time to shutdown. This needs to be comfortably less than the
                // time the docker stop command will wait for a container to terminate on its own
                // before forcibly killing it.
                if (!service.awaitTermination(7, TimeUnit.SECONDS)) {
                    System.out.println(name + " thread: did not shutdown in time, forcing service shutdown");
                    service.shutdownNow();
                } else {
                    System.out.println(name + " thread: shutdown cleanly");
                }
            } catch (InterruptedException e) {
                System.out.println(name + " thread: shutdown timer interrupted, forcing service shutdown");
                service.shutdownNow();
            }
        }));
    }
}
