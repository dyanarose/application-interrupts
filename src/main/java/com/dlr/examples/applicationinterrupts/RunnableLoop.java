package com.dlr.examples.applicationinterrupts;

public class RunnableLoop implements Runnable {
    private String name;
    private long timeout;
    public RunnableLoop(String name, long timeout) {
        this.name = name;
        this.timeout = timeout;
    }
    @Override
    public void run() {
        System.out.println(name + " loop: starting loop");
        long i = 0;
        while (i < Long.MAX_VALUE) {
            if (!Thread.interrupted()) {
                i++;
                continue;
            }
            // pretend we're doing shutdown work
            try {
                Thread.sleep(timeout);
                System.out.println(name + " loop: interrupted at i = " + i );
            } catch (InterruptedException e) {
                System.out.println(name + " loop: forcibly stopped");
            }
            return;
        }
    }
}
