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

package oap.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import oap.testng.AbstractTest;
import oap.testng.Asserts;
import org.influxdb.dto.Point;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class InfluxDBReporterTest extends AbstractTest {

    private MockInfluxDB influxDB;
    private MetricRegistry registry;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        influxDB = new MockInfluxDB();
        registry = new MetricRegistry();

        DateTimeUtils.setCurrentMillisFixed( 1454055727921L );
    }

    @Test
    public void testReport_aggregates() throws Exception {
        final InfluxDBReporter reporter = createReporter( influxDB, registry, asList( "test.*", "test2.test2.*" ) );

        registry.counter( "test.name1" ).inc();
        registry.counter( "test.name2" ).inc( 2 );

        registry.register( "test2.test2.g1", ( Gauge ) () -> 10 );
        registry.register( "test2.test2.g2", ( Gauge ) () -> 10 );

        registry.histogram( "test.h1" ).update( 10 );
        registry.histogram( "test.h2" ).update( 20 );

        registry.meter( "test2.test2.m1" ).mark();
        registry.meter( "test2.test2.m2" ).mark();

        registry.timer( "test.t1" ).update( 10, TimeUnit.DAYS );
        registry.timer( "test.t2" ).update( 10, TimeUnit.HOURS );

        reporter.report(
            registry.getGauges(),
            registry.getCounters(),
            registry.getHistograms(),
            registry.getMeters(),
            registry.getTimers()
        );

        assertString( getPoints() ).contains(
            "test", "t1=10.0", "g1=10i", "h1=10.0", "h1_75th=10.0", "name1=1i,name2=2i",
            "t1=10.0","t2=0.41666666666666663", "h2_stddev=0.0", "m2=0.0" );
    }

    public InfluxDBReporter createReporter( MockInfluxDB influxDB, MetricRegistry registry, List<String> aggregates ) {
        return new InfluxDBReporter(
            influxDB,
            "database",
            emptyMap(),
            registry,
            "name",
            new ReporterFilter( emptyList(), emptyList() ),
            aggregates,
            TimeUnit.DAYS,
            TimeUnit.DAYS
        );
    }

    @Test
    public void testAggregatesWithTags() {
        final InfluxDBReporter reporter = createReporter( influxDB, registry, singletonList( "test.*" ) );

        registry.counter( "test.name1,b=10,v=20" ).inc();
        registry.counter( "test.name2,b=10,v=20" ).inc( 2 );

        reporter.report(
            registry.getGauges(),
            registry.getCounters(),
            registry.getHistograms(),
            registry.getMeters(),
            registry.getTimers()
        );

        assertString( getPoints() ).isEqualTo( "test,b=10,v=20 name1=1i,name2=2i 1454055727921000000" );
    }

    public String getPoints() {
        return influxDB.writes.stream().map( Point::lineProtocol ).collect( joining( "\n" ) );
    }
}