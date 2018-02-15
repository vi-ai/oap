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

package oap.storage.mongo;

import lombok.val;
import org.bson.Document;
import org.testng.annotations.Test;

import static oap.application.ApplicationUtils.service;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 13.02.2018.
 */
public class OplogServiceTest extends AbstractMongoTest {
    @Test
    public void testOplog() {
        try( val oplogListener = service( new OplogService( mongoClient ) ) ) {

            val sb = new StringBuilder();

            oplogListener.addListener( "test_OplogServiceTest", new OplogService.OplogListener() {
                @Override
                public void updated( String table, Object id ) {
                    sb.append( 'u' );
                }

                @Override
                public void deleted( String table, Object id ) {
                    sb.append( 'd' );
                }

                @Override
                public void inserted( String table, Object id ) {
                    sb.append( 'i' );
                }
            } );

            mongoClient.database.getCollection( "test_OplogServiceTest" ).insertOne( new Document( "test", "test" ) );
            mongoClient.database.getCollection( "test_OplogServiceTest2" ).updateOne( new Document( "test", "test" ), new Document( "$set", new Document( "test", "test2" ) ) );
            mongoClient.database.getCollection( "test_OplogServiceTest" ).updateOne( new Document( "test", "test" ), new Document( "$set", new Document( "test", "test2" ) ) );
            mongoClient.database.getCollection( "test_OplogServiceTest" ).updateOne( new Document( "test", "test2" ), new Document( "$set", new Document( "test", "test3" ) ) );
            mongoClient.database.getCollection( "test_OplogServiceTest" ).deleteOne( new Document( "test", "test3" ) );

            assertEventually( 100, 100, () -> assertThat( sb.toString() ).isEqualTo( "iuud" ) );
        }
    }
}