/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oap.concurrent.scheduler;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Threads;
import oap.util.Lists;
import oap.util.Try;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

@Slf4j
public class QuartzScheduled extends Scheduled {
    private final JobDetail job;
    private ScheduledRunnable runnable;

    QuartzScheduled( JobDetail job, ScheduledRunnable runnable ) {
        this.job = job;
        this.runnable = runnable;
    }

    @Override
    @SneakyThrows
    public void cancel() {
        log.trace( "cancelling {}", job );
        Scheduler.scheduler.deleteJob( job.getKey() );
        Scheduler.jobFactory.unregister( job.getKey() );

        int i = 10;

        while( --i >= 0 && Lists.contains( Scheduler.scheduler.getCurrentlyExecutingJobs(),
            j -> j.getJobDetail().getKey().equals( job.getKey() ) ) ) {

            try {
                Lists.find( Scheduler.scheduler.getCurrentlyExecutingJobs(),
                    j -> j.getJobDetail().getKey().equals( job.getKey() ) )
                    .ifPresent( Try.consume( j -> {
                        log.debug( "running job [{}]...", j.getJobDetail().getKey() );
                        Scheduler.scheduler.interrupt( j.getJobDetail().getKey() );
                    } ) );
            } catch( Exception e ) {
                log.error( e.getMessage(), e );
            }

            Threads.sleepSafely( 1000 );
        }
        log.trace( "cancelled {}", job );
    }

    @Override
    public void triggerNow() {
        runnable.waitFor( () -> {
            try {
                Scheduler.scheduler.triggerJob( job.getKey() );
            } catch( SchedulerException e ) {
                log.warn( e.getMessage(), e );
            }
        } );
    }

    @Override
    public String toString() {
        return job.toString();
    }
}
