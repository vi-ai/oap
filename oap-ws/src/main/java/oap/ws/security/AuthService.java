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

package oap.ws.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import oap.util.Hash;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AuthService {

    private final Cache<String, Token> tokenStorage;
    private final UserStorage userStorage;
    private final String salt;

    public AuthService( UserStorage userStorage, int expirationTime, String salt ) {
        this.tokenStorage = CacheBuilder.newBuilder()
            .expireAfterAccess( expirationTime, TimeUnit.MINUTES )
            .build();
        this.userStorage = userStorage;
        this.salt = salt;
    }

    public synchronized Optional<Token> generateToken( String email, String password ) {
        final User user = userStorage.getByEmail( email.toLowerCase() ).orElse( null );
        if( user == null ) return Optional.empty();

        final String inputPassword = Hash.sha256( salt, password );
        if( !user.getPassword().equals( inputPassword ) ) return Optional.empty();

        Token token = null;

        for( Token t : tokenStorage.asMap().values() ) {
            if( t.user.getEmail().equals( user.getEmail() ) ) {
                token = t;
                break;
            }
        }

        if( token != null ) {
            log.debug( "Updating existing token for user [{}]...", user.getEmail() );
            tokenStorage.put( token.id, token );

            return Optional.of( token );
        }

        log.debug( "Generating new token for user [{}]...", user.getEmail() );
        token = new Token();
        token.user = user;
        token.created = DateTime.now();
        token.id = UUID.randomUUID().toString();

        tokenStorage.put( token.id, token );

        return Optional.of( token );

    }

    public synchronized Optional<Token> getToken( String tokenId ) {
        return Optional.ofNullable( tokenStorage.getIfPresent( tokenId ) );
    }

    public void invalidateUser( String email ) {
        final ConcurrentMap<String, Token> tokens = tokenStorage.asMap();

        for( Map.Entry<String, Token> entry : tokens.entrySet() ) {
            if( Objects.equals( entry.getValue().user.getEmail(), email.toLowerCase() ) ) {
                log.debug( "Deleting token [{}]...", entry.getKey() );
                tokenStorage.invalidate( entry.getKey() );

                return;
            }
        }
    }

}