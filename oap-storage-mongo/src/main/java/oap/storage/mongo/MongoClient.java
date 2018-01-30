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

import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import lombok.val;
import org.bson.codecs.configuration.CodecRegistries;

import java.io.Closeable;

/**
 * Created by igor.petrenko on 21.12.2017.
 */
public class MongoClient implements Closeable {
    public final MongoDatabase database;
    protected final com.mongodb.MongoClient mongoClient;
    private final String host;
    private final int port;
    private final Migration migration;

    public MongoClient( String host, int port, String database, Migration migration ) {
        this.host = host;
        this.port = port;
        this.migration = migration;

        val codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JodaTimeCodec() ),
            com.mongodb.MongoClient.getDefaultCodecRegistry() );

        val options = MongoClientOptions.builder().codecRegistry( codecRegistry ).build();

        mongoClient = new com.mongodb.MongoClient( new ServerAddress( host, port ), options );
        this.database = mongoClient.getDatabase( database );
    }

    public void start() {
        migration.run( database );
    }

    @Override
    public void close() {
        mongoClient.close();
    }

    public MongoDatabase getDatabase( String database ) {
        return mongoClient.getDatabase( database );
    }
}
