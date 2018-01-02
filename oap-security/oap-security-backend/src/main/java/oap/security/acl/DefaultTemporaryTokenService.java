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

package oap.security.acl;

import lombok.val;
import oap.storage.Storage;
import org.joda.time.DateTimeUtils;

import java.util.Optional;

/**
 * Created by igor.petrenko on 27.12.2017.
 */
public class DefaultTemporaryTokenService implements TemporaryTokenService, Runnable {
    private final Storage<TemporaryToken> storage;
    private final long expiration;

    public DefaultTemporaryTokenService( Storage<TemporaryToken> storage, long expiration ) {
        this.storage = storage;
        this.expiration = expiration;
    }

    @Override
    public String create( String objectId ) {
        return storage.store( new TemporaryToken( null, objectId, DateTimeUtils.currentTimeMillis() ) ).id;
    }

    @Override
    public Optional<TemporaryToken> get( String tokenId ) {
        return storage.get( tokenId );
    }

    @Override
    public void run() {
        val now = DateTimeUtils.currentTimeMillis() - expiration;
        for( val tt : storage ) {
            if( tt.time < now ) {
                storage.delete( tt.id );
            }
        }
    }
}