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

package oap.statsdb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import oap.storage.FilePersistence;
import oap.storage.Identifier;
import oap.storage.MemoryStorage;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Cuid;
import oap.util.Stream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.storage.Storage.Lock.CONCURRENT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 08.09.2017.
 *
 * @todo by some reason it fails every second run
 */
@Test
public class StatsDBTest extends AbstractTest {
    private static final KeySchema schema2 = new KeySchema( "n1", "n2" );
    private static final KeySchema schema3 = new KeySchema( "n1", "n2", "n3" );
    private Path masterDbPath;
    private Path nodeDbPath;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        masterDbPath = Env.tmpPath( "master.db" );
        nodeDbPath = Env.tmpPath( "node.db" );
    }

    @AfterMethod
    @Override
    public void afterMethod() throws Exception {
        Cuid.restore();

        super.afterMethod();
    }

    @Test
    public void children() {
        try( val master = new StatsDBMaster( schema2, new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT ) ) ) {
            master.start();
            master.update( "k1", "k2", c -> c.ci = 10, MockChild::new );
            master.update( "k1", "k3", c -> c.ci = 3, MockChild::new );
            master.update( "k2", "k4", c -> c.ci = 4, MockChild::new );
            master.update( "k1", c -> c.i2 = 10, MockValue::new );


            assertThat( master.children( "k1" ) )
                .hasSize( 2 )
                .contains( new MockChild( 10 ) )
                .contains( new MockChild( 3 ) );

            assertThat( master.children( "k2" ) )
                .hasSize( 1 )
                .contains( new MockChild( 4 ) );

            assertThat( master.children( "unknown" ) ).isEmpty();
            assertThat( master.children( "k1", "k2" ) ).isEmpty();
        }
    }

    @Test
    public void mergeChild() {
        try( val master = new StatsDBMaster( schema3, new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT ) );
             val node = new StatsDBNode( schema3, master, null, new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT ) ) ) {
            master.start();

            node.update( "p", ( p ) -> {}, () -> new MockValue( 1 ) );
            node.update( "p", "c1", c -> {}, () -> new MockChild( 1 ) );
            node.update( "p", "c1", "c2", c -> {}, () -> new MockChild( 2 ) );
            node.sync();

            assertThat( master.<MockValue>get( "p" ).sum ).isEqualTo( 3 );

            node.update( "p", ( p ) -> {}, () -> new MockValue( 1 ) );
            node.update( "p", "c1", "c2", "c3", c -> {}, () -> new MockChild( 2 ) );
            node.sync();

            node.update( "p", "c1", "c2", c -> {}, () -> new MockChild( 2 ) );
            node.sync();

            assertThat( master.<MockValue>get( "p" ).i2 ).isEqualTo( 2 );
            assertThat( master.<MockValue>get( "p" ).sum ).isEqualTo( 5 );
            assertThat( master.<MockChild>get( "p", "c1" ).ci ).isEqualTo( 1 );
            assertThat( master.<MockChild>get( "p", "c1" ).sum ).isEqualTo( 4 );
            assertThat( master.<MockChild>get( "p", "c1", "c2" ).ci ).isEqualTo( 4 );
            assertThat( master.<MockChild>get( "p", "c1", "c2", "c3" ).ci ).isEqualTo( 2 );
        }
    }


    @Test
    public void persistMaster() {
        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( masterDbPath, 10, storage );
             StatsDBMaster master = new StatsDBMaster( schema2, storage ) ) {
            persistence.start();
            master.start();
            master.update( "k1", "k2", c -> c.i2 = 10, MockValue::new );
        }

        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( masterDbPath, 10, storage );
             StatsDBMaster master = new StatsDBMaster( schema2, storage ) ) {
            persistence.start();
            master.start();
            assertThat( master.<MockValue>get( "k1", "k2" ) ).isNotNull();
            assertThat( master.<MockValue>get( "k1", "k2" ).i2 ).isEqualTo( 10 );
        }
    }

    @Test
    public void persistNode() {
        final MockRemoteStatsDB master = new MockRemoteStatsDB( schema2 );
        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( nodeDbPath, 10, storage );
             StatsDBNode node = new StatsDBNode( schema2, master, Env.tmpPath( "node" ), storage ) ) {
            persistence.start();
            node.update( "k1", "k2", c -> c.i2 = 10, MockValue::new );
        }

        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( nodeDbPath, 10, storage );
             StatsDBNode node = new StatsDBNode( schema2, master, Env.tmpPath( "node" ), storage ) ) {
            persistence.start();
            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNotNull();
            assertThat( node.<MockValue>get( "k1", "k2" ).i2 ).isEqualTo( 10 );
        }
    }

    @Test
    public void sync() {
        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( masterDbPath, 10, storage );
             StatsDBMaster master = new StatsDBMaster( schema2, storage );
             StatsDBNode node = new StatsDBNode( schema2, master, null, new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT ) ) ) {
            persistence.start();
            master.start();
            node.sync();

            node.update( "k1", "k2", c -> c.ci = 10, MockChild::new );
            node.update( "k1", "k3", c -> c.ci = 1, MockChild::new );
            node.update( "k1", c -> c.i2 = 20, MockValue::new );

            node.sync();
            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNull();
            assertThat( master.<MockChild>get( "k1", "k2" ).ci ).isEqualTo( 10 );
            assertThat( master.<MockValue>get( "k1" ).i2 ).isEqualTo( 20 );

            node.update( "k1", "k2", c -> c.ci = 10, MockChild::new );
            node.update( "k1", c -> c.i2 = 21, () -> new MockValue( 21 ) );

            node.sync();
            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNull();
            assertThat( master.<MockChild>get( "k1", "k2" ).ci ).isEqualTo( 20 );
            assertThat( master.<MockValue>get( "k1" ).i2 ).isEqualTo( 41 );
            assertThat( master.<MockValue>get( "k1" ).sum ).isEqualTo( 21L );
        }
    }

    @Test( dependsOnMethods = "sync" )
    public void calculatedValuesAfterRestart() {
        sync();

        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( masterDbPath, 10, storage );
             StatsDBMaster master = new StatsDBMaster( schema2, storage ) ) {
            persistence.start();
            master.start();
            assertThat( master.<MockValue>get( "k1" ).sum ).isEqualTo( 21L );
        }
    }

    @Test
    public void syncFailed() {
        final MockRemoteStatsDB master = new MockRemoteStatsDB( schema2 );

        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( nodeDbPath, 10, storage );
             StatsDBNode node = new StatsDBNode( schema2, master, Env.tmpPath( "node" ), storage ) ) {
            persistence.start();
            master.syncWithException( ( sync ) -> new RuntimeException( "sync" ) );
            node.update( "k1", "k2", c -> c.i2 = 10, MockValue::new );
            node.sync();
            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNull();
        }

        assertThat( master.syncs ).isEmpty();

        try( MemoryStorage<IdNode> storage = new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT );
             FilePersistence<IdNode> persistence = new FilePersistence<>( nodeDbPath, 10, storage );
             StatsDBNode node = new StatsDBNode( schema2, master, Env.tmpPath( "node" ), storage ) ) {
            persistence.start();

            master.syncWithoutException();
            node.sync();

            assertThat( node.<MockValue>get( "k1", "k2" ) ).isNull();
        }

        assertThat( master.syncs ).hasSize( 1 );
    }

    @Test
    public void version() {
        try( StatsDBMaster master = new StatsDBMaster( schema2, new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT ) );
             StatsDBNode node = new StatsDBNode( schema2, master, null, new MemoryStorage<>( Identifier.forAnnotationFixed(), CONCURRENT ) ) ) {

            master.start();

            Cuid.reset( "s", 0 );

            node.update( "k1", c -> c.i2 = 20, MockValue::new );
            node.sync();
            assertThat( master.<MockValue>get( "k1" ).i2 ).isEqualTo( 20 );

            Cuid.reset( "s", 0 );
            node.update( "k1", c -> c.i2 = 21, MockValue::new );
            node.sync();
            assertThat( master.<MockValue>get( "k1" ).i2 ).isEqualTo( 20 );
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class MockValue implements Node.Container<MockValue, MockChild> {
        public long l1;
        public int i2;

        @JsonIgnore
        public long sum;

        public MockValue() {
            this( 0 );
        }

        public MockValue( int i2 ) {
            this.i2 = i2;
        }

        @Override
        public MockValue aggregate( Stream<MockChild> children ) {
            sum = children.mapToLong( c -> c.sum + c.ci ).sum();

            return this;
        }

        @Override
        public MockValue merge( MockValue other ) {
            l1 += other.l1;
            i2 += other.i2;

            return this;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class MockChild implements Node.Container<MockChild, MockChild> {
        public long ci;
        public long sum;

        public MockChild() {
        }

        public MockChild( long ci ) {
            this.ci = ci;
        }

        @Override
        public MockChild merge( MockChild other ) {
            ci += other.ci;

            return this;
        }

        @Override
        public MockChild aggregate( Stream<MockChild> children ) {
            sum = children.mapToLong( c -> c.ci ).sum();
            return this;
        }
    }
}
