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
package oap.http;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.AsyncCallbacks;
import oap.io.Closeables;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Stream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.reactor.IOReactorException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.ProgressInputStream.progress;
import static oap.util.Maps.Collectors.toMap;
import static oap.util.Pair.__;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

@Slf4j
public class Client extends AsyncCallbacks<Client> {
   private static final long FOREVER = Long.MAX_VALUE;

   private final BasicCookieStore basicCookieStore;
   private final Path certificateLocation;
   private final String certificatePassword;

   private CloseableHttpAsyncClient client;

   public static Client DEFAULT = new Client()
      .onError( e -> log.error( e.getMessage(), e ) )
      .onTimeout( () -> log.error( "timeout" ) );

   public Client() {
      this( null, null );
   }

   public Client( Path certificateLocation, String certificatePassword ) {
      this.certificateLocation = certificateLocation;
      this.certificatePassword = certificatePassword;
      this.basicCookieStore = new BasicCookieStore();
      this.client = initialize();
   }

   public Response get( String uri ) {
      return get( uri, Maps.empty(), Maps.empty() );
   }

   @SafeVarargs
   public final Response get( String uri, Pair<String, Object>... params ) {
      return get( uri, Maps.of( params ) );
   }

   public Response get( String uri, Map<String, Object> params ) {
      return get( uri, params, Maps.empty() );
   }

   public Response get( String uri, Map<String, Object> params, Map<String, Object> headers ) {
      return get( uri, params, headers, FOREVER )
         .orElseThrow( () -> new RuntimeException( "no response" ) );
   }

   public Optional<Response> get( String uri, Map<String, Object> params, long timeout ) {
      return get( uri, params, Maps.empty(), timeout );
   }

   public Optional<Response> get( String uri, Map<String, Object> params, Map<String, Object> headers, long timeout ) {
      return execute( new HttpGet( Uri.uri( uri, params ) ), headers, timeout );
   }


   public Response post( String uri, Map<String, Object> params ) {
      return post( uri, params, Maps.empty() );
   }

   public Response post( String uri, Map<String, Object> params, Map<String, Object> headers ) {
      return post( uri, params, headers, FOREVER )
         .orElseThrow( () -> new RuntimeException( "no response" ) );
   }

   public Optional<Response> post( String uri, Map<String, Object> params, long timeout ) {
      return post( uri, params, Maps.empty(), timeout );
   }

   public Optional<Response> post( String uri, Map<String, Object> params, Map<String, Object> headers, long timeout ) {
      try {
         HttpPost request = new HttpPost( uri );
         request.setEntity( new UrlEncodedFormEntity( Stream.of( params.entrySet() )
            .<NameValuePair>map( e -> new BasicNameValuePair( e.getKey(),
               e.getValue() == null ? "" : e.getValue().toString() ) )
            .toList()
         ) );
         return execute( request, headers, timeout );
      } catch( UnsupportedEncodingException e ) {
         throw new UncheckedIOException( e );
      }
   }

   public Response post( String uri, String content, ContentType contentType ) {
      return post( uri, content, contentType, Maps.empty() );
   }

   public Response post( String uri, String content, ContentType contentType, Map<String, Object> headers ) {
      return post( uri, content, contentType, headers, FOREVER )
         .orElseThrow( () -> new RuntimeException( "no response" ) );
   }

   public Optional<Response> post( String uri, String content, ContentType contentType, long timeout ) {
      return post( uri, content, contentType, Maps.empty(), timeout );
   }

   public Optional<Response> post( String uri, String content, ContentType contentType, Map<String, Object> headers, long timeout ) {
      HttpPost request = new HttpPost( uri );
      request.setEntity( new StringEntity( content, contentType ) );
      return execute( request, headers, timeout );
   }

   public Optional<Response> post( String uri, byte[] content, long timeout ) {
      HttpPost request = new HttpPost( uri );
      request.setEntity( new ByteArrayEntity( content, APPLICATION_OCTET_STREAM ) );
      return execute( request, Maps.empty(), timeout );
   }

   public Response post( String uri, InputStream content, ContentType contentType ) {
      HttpPost request = new HttpPost( uri );
      request.setEntity( new InputStreamEntity( content, contentType ) );
      return execute( request, Maps.empty(), FOREVER )
         .orElseThrow( () -> new RuntimeException( "no response" ) );
   }

   public Response put( String uri, String content, ContentType contentType ) {
      HttpPut request = new HttpPut( uri );
      request.setEntity( new StringEntity( content, contentType ) );
      return execute( request, Maps.empty(), FOREVER )
         .orElseThrow( () -> new RuntimeException( "no response" ) );
   }

   public Response delete( String uri ) {
      return delete( uri, FOREVER );
   }

   public Response delete( String uri, long timeout ) {
      HttpDelete request = new HttpDelete( uri );
      return execute( request, Maps.empty(), timeout )
         .orElseThrow( () -> new RuntimeException( "no response" ) );
   }

   public List<Cookie> getCookies() {
      return basicCookieStore.getCookies();
   }

   public void clearCookies() {
      basicCookieStore.clear();
   }

   private Optional<Response> execute( HttpUriRequest request, Map<String, Object> headers, long timeout ) {
      try {
         headers.forEach( ( name, value ) -> request.setHeader( name, value == null ? "" : value.toString() ) );

         Future<HttpResponse> future = client.execute( request, FUTURE_CALLBACK );
         HttpResponse response = timeout == FOREVER ? future.get() :
            future.get( timeout, MILLISECONDS );

         Map<String, String> responsHeaders = headers( response );
         Response result;
         if( response.getEntity() != null ) {
            HttpEntity entity = response.getEntity();
            try( InputStream is = entity.getContent() ) {
               result = new Response(
                  response.getStatusLine().getStatusCode(),
                  response.getStatusLine().getReasonPhrase(),
                  responsHeaders,
                  Optional.ofNullable( entity.getContentType() )
                     .map( ct -> ContentType.parse( entity.getContentType().getValue() ) ),
                  ByteStreams.toByteArray( is )
               );
            }
         } else result = new Response(
            response.getStatusLine().getStatusCode(),
            response.getStatusLine().getReasonPhrase(),
            responsHeaders
         );
         onSuccess.run();
         return Optional.of( result );
      } catch( ExecutionException e ) {
         onError.accept( e );
         throw Throwables.propagate( e );
      } catch( IOException e ) {
         onError.accept( e );
         throw new UncheckedIOException( e );
      } catch( InterruptedException | TimeoutException e ) {
         onTimeout.run();
         return Optional.empty();
      }
   }

   private static Map<String, String> headers( org.apache.http.HttpResponse response ) {
      return Arrays.stream( response.getAllHeaders() )
         .map( h -> __( h.getName(), h.getValue() ) )
         .collect( toMap() );
   }

   public void download( String url, Path file, Consumer<Integer> progress ) {
      try {
         HttpEntity entity = resolve( url ).getEntity();
         try( InputStream in = new BufferedInputStream( entity.getContent() ) ) {
            IoStreams.write( file, PLAIN, in, progress( entity.getContentLength(), progress ) );
         }
         onSuccess.run();
      } catch( ExecutionException e ) {
         onError.accept( e );
         throw Throwables.propagate( e );
      } catch( IOException e ) {
         onError.accept( e );
         throw new UncheckedIOException( e );
      } catch( InterruptedException e ) {
         onTimeout.run();
      }
   }

   private HttpResponse resolve( String url ) throws InterruptedException, ExecutionException, IOException {
      HttpGet request = new HttpGet( url );
      Future<HttpResponse> future = client.execute( request, FUTURE_CALLBACK );
      HttpResponse response = future.get();
      if( response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null ) return response;
      else if( response.getStatusLine().getStatusCode() == 302 ) {
         Header location = response.getFirstHeader( "Location" );
         if( location == null ) throw new IOException( "redirect w/o location!" );
         log.debug( "following {}", location.getValue() );
         return resolve( location.getValue() );
      } else
         throw new IOException( response.getStatusLine().toString() );
   }


   @ToString
   public static class Response {
      public final int code;
      public final String reasonPhrase;
      public final Optional<ContentType> contentType;
      public final Map<String, String> headers;
      public final Optional<byte[]> content;
      public final Optional<String> contentString;

      public Response( int code, String reasonPhrase, Map<String, String> headers, Optional<ContentType> contentType, byte[] content ) {
         this.code = code;
         this.reasonPhrase = reasonPhrase;
         this.headers = headers;
         this.contentType = contentType;
         this.content = Optional.ofNullable( content );
         this.contentString = this.content.map( bytes ->
            new String( bytes, contentType.map( ContentType::getCharset )
               .orElse( StandardCharsets.UTF_8 ) ) );
      }

      public Response( int code, String reasonPhrase, Map<String, String> headers ) {
         this( code, reasonPhrase, headers, Optional.empty(), null );
      }

      public <T> Optional<T> unmarshal( Class<?> clazz ) {
         return this.contentString.map( json -> Binder.json.unmarshal( clazz, json ) );
      }
   }

   private static final FutureCallback<org.apache.http.HttpResponse> FUTURE_CALLBACK = new FutureCallback<org.apache.http.HttpResponse>() {
      @Override
      public void completed( org.apache.http.HttpResponse result ) {
      }

      @Override
      public void failed( Exception e ) {
      }

      @Override
      public void cancelled() {

      }
   };

   private static SSLContext createSSLContext( Path certificateLocation, String certificatePassword ) {
      try {
         KeyStore keyStore = KeyStore.getInstance( "JKS" );
         keyStore.load( IoStreams.in( certificateLocation, PLAIN ),
            certificatePassword.toCharArray() );

         TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
         trustManagerFactory.init( keyStore );

         SSLContext sslContext = SSLContext.getInstance( "TLS" );
         sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

         return sslContext;
      } catch( KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException e ) {
         throw Throwables.propagate( e );
      }
   }

   private CloseableHttpAsyncClient initialize() {
      try {
         CloseableHttpAsyncClient client = ( certificateLocation != null ?
            HttpAsyncClients.custom()
               .setSSLContext( createSSLContext( certificateLocation, certificatePassword ) )
            : HttpAsyncClients.custom() )
            .setMaxConnPerRoute( 1000 )
            .setConnectionManager( new PoolingNHttpClientConnectionManager(
               new DefaultConnectingIOReactor( IOReactorConfig.custom()
                  .setConnectTimeout( 1000 )
                  .setSoTimeout( 1000 )
                  .build() ) ) )
            .setMaxConnTotal( 10000 )
            .setKeepAliveStrategy( DefaultConnectionKeepAliveStrategy.INSTANCE )
            .setDefaultRequestConfig( RequestConfig
               .custom()
               .setRedirectsEnabled( false )
               .setCookieSpec( CookieSpecs.STANDARD )
               .build() )
            .setDefaultCookieStore( basicCookieStore )
            .build();
         client.start();
         return client;
      } catch( IOReactorException e ) {
         throw new UncheckedIOException( e );
      }
   }

   public void reset() {
      Closeables.close( client );
      client = initialize();
   }

}
