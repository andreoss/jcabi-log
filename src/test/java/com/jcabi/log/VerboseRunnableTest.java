/*
 * Copyright (c) 2012-2020, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.log;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link VerboseRunnable}.
 * @since 0.1
 */
@SuppressWarnings({ "PMD.DoNotUseThreads", "PMD.TooManyMethods" })
public final class VerboseRunnableTest {

    @Test
    public void logsExceptionsInRunnable() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new VerboseRunnable(
                (Runnable) () -> {
                    throw new IllegalArgumentException("oops");
                }
            ).run()
        );
    }

    @Test
    public void swallowsExceptionsInRunnable() {
        new VerboseRunnable(
            (Runnable) () -> {
                throw new IllegalArgumentException("boom");
            },
            true
        ).run();
    }

    @Test
    public void swallowsExceptionsInCallable() {
        new VerboseRunnable(
            () -> {
                throw new IllegalArgumentException("boom-2");
            },
            true
        ).run();
    }

    @Test
    public void translatesToStringFromUnderlyingRunnable() throws Exception {
        final String text = "some text abc";
        final Runnable verbose = new VerboseRunnable(
            new Runnable() {
                @Override
                public void run() {
                    assert true;
                }

                @Override
                public String toString() {
                    return text;
                }
            }
        );
        MatcherAssert.assertThat(
            verbose,
            Matchers.hasToString(Matchers.containsString(text))
        );
    }

    @Test
    public void translatesToStringFromUnderlyingCallable() throws Exception {
        final String text = "some text abc-2";
        final Runnable verbose = new VerboseRunnable(
            new Callable<Void>() {
                @Override
                public Void call() {
                    return null;
                }

                @Override
                public String toString() {
                    return text;
                }
            },
            true
        );
        MatcherAssert.assertThat(
            verbose,
            Matchers.hasToString(Matchers.containsString(text))
        );
    }

    @Test
    public void preservesInterruptedStatus() throws Exception {
        final ScheduledExecutorService svc =
            Executors.newSingleThreadScheduledExecutor();
        final AtomicReference<Thread> thread = new AtomicReference<>();
        final AtomicInteger runs = new AtomicInteger();
        svc.scheduleWithFixedDelay(
            new VerboseRunnable(
                () -> {
                    runs.addAndGet(1);
                    thread.set(Thread.currentThread());
                    TimeUnit.HOURS.sleep(1L);
                    return null;
                },
                true,
                false
            ),
            1L, 1L,
            TimeUnit.MICROSECONDS
        );
        while (thread.compareAndSet(null, null)) {
            TimeUnit.MILLISECONDS.sleep(1L);
        }
        thread.get().interrupt();
        svc.shutdown();
        MatcherAssert.assertThat(
            svc.awaitTermination(1L, TimeUnit.SECONDS),
            Matchers.is(true)
        );
        MatcherAssert.assertThat(runs.get(), Matchers.is(1));
    }

}
