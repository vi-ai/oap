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

package oap.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;
import oap.testng.Env;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static oap.storage.Storage.LockStrategy.Lock;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 19.09.2017.
 */
public class MongoStorageTest {
    private String dbName;
    private MongoStorage<TestMongoBean> storage;
    private MongoClient mongoClient;

    @BeforeMethod
    public void beforeMethod() {
        dbName = "db" + Env.teamcityBuildPrefix().replace( ".", "_" );

        mongoClient = new MongoClient( "localhost", 27017 );
        mongoClient.getDatabase( dbName ).drop();

        storage = reopen();
    }

    public MongoStorage<TestMongoBean> reopen() {

        return new MongoStorage<>( mongoClient, dbName, "test", b -> b.id, ( b, id ) -> b.id = id, Lock, TestMongoBean.class );
    }

    @AfterMethod
    public void afterMethod() {
        storage.database.drop();
        storage.close();
        mongoClient.close();
    }

    @Test
    public void testStore() {
        val bean1 = storage.store( new TestMongoBean() );
        storage.store( new TestMongoBean() );
        storage.store( new TestMongoBean( bean1.id ) );

        storage.close();

        storage = reopen();

        assertThat( storage ).hasSize( 2 );

    }

    @ToString
    @EqualsAndHashCode( of = { "id" } )
    public static class TestMongoBean {
        public String id;

        @JsonCreator
        public TestMongoBean( @JsonProperty String id ) {
            this.id = id;
        }

        public TestMongoBean() {
            this( null );
        }
    }
}