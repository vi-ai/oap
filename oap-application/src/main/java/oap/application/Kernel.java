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
package oap.application;

import oap.application.remote.RemoteInvocationHandler;
import oap.application.supervision.Supervisor;
import oap.json.Parser;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Maps;
import oap.util.Stream;
import org.slf4j.Logger;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;

public class Kernel {
    private static Logger logger = getLogger( Kernel.class );
    private final Set<Module> modules;
    private Supervisor supervisor = new Supervisor();

    public Kernel( List<URL> modules ) {
        logger.debug( "modules = " + modules );

        this.modules = modules
            .stream()
            .map( Module::parse )
            .collect( toSet() );
    }

    private Set<Module.Service> initializeServices( Set<Module.Service> services, Set<String> initialized,
        Map<String, Map<String, Object>> config ) {

        HashSet<Module.Service> deferred = new HashSet<>();

        for( Module.Service service : services )
            if( initialized.containsAll( service.dependsOn ) ) {
                logger.debug( "initializing " + service.name );

                Reflection reflect = Reflect.reflect( service.implementation );

                Object instance;
                if( service.remoteUrl == null ) {
                    config.getOrDefault( service.name, Collections.emptyMap() )
                        .forEach( service.parameters::put );
                    initializeServiceLinks( service );
                    instance = reflect.newInstance( service.parameters );
                } else instance = RemoteInvocationHandler.proxy(
                    service.remoteUrl,
                    service.remoteName,
                    reflect.underlying
                );
                Application.register( service.name, instance );
                if( service.supervision.supervise )
                    supervisor.startSupervised( service.name, instance );
                if( service.supervision.thread )
                    supervisor.startThread( service.name, instance );
                else {
                    if( service.supervision.schedule && service.supervision.delay > 0 )
                        supervisor.scheduleWithFixedDelay( service.name, (Runnable) instance,
                            service.supervision.delay, TimeUnit.SECONDS );
                    else if( service.supervision.schedule && service.supervision.cron != null )
                        supervisor.scheduleCron( service.name, (Runnable) instance,
                            service.supervision.cron );
                }
                initialized.add( service.name );
            } else {
                logger.debug( "dependencies are not ready - deferring " + service.name );
                deferred.add( service );
            }

        return deferred.size() == services.size() ? deferred : initializeServices( deferred, initialized, config );
    }

    private void initializeServiceLinks( Module.Service service ) {
        for( Map.Entry<String, Object> entry : service.parameters.entrySet() )
            if( entry.getValue() instanceof String && ((String) entry.getValue()).startsWith( "@service:" ) ) {
                logger.debug( "for " + service.name + " linking " + entry );
                Object link = Application.service( ((String) entry.getValue()).substring( "@service:".length() ) );
                if( link == null ) throw new ApplicationException(
                    "for " + service.name + " service link " + entry.getValue() + " is not initialized yet" );
                entry.setValue( link );
            }
    }

    private Set<Module> initialize( Set<Module> modules, Set<String> initialized,
        Map<String, Map<String, Object>> config ) {
        HashSet<Module> deferred = new HashSet<>();

        for( Module module : modules ) {
            logger.debug( "initializing module " + module.name );
            if( initialized.containsAll( module.dependsOn ) ) {

                Set<Module.Service> def =
                    initializeServices( new LinkedHashSet<>( module.services ), new LinkedHashSet<>(), config );
                if( !def.isEmpty() ) {
                    List<String> names = Stream.of( def.stream() ).map( s -> s.name ).toList();
                    logger.error( "failed to initialize: " + names );
                    throw new ApplicationException( "failed to initialize services: " + names );
                }

                initialized.add( module.name );
            } else {
                logger.debug( "dependencies are not ready - deferring " + module.name );
                deferred.add( module );
            }
        }

        return deferred.size() == modules.size() ? deferred : initialize( deferred, initialized, config );
    }

    public void start( Map<String, Map<String, Object>> config ) {
        logger.debug( "initializing application kernel..." );
        logger.trace( "modules = " + Stream.of( modules ).map( m -> m.name ).toList() );

        if( !initialize( modules, new HashSet<>(), config ).isEmpty() ) {
            logger.error( "failed to initialize: " + modules );
            throw new ApplicationException( "failed to initialize modules" );
        }

        supervisor.start();
    }

    public void stop() {
        supervisor.stop();
        Application.unregisterServices();
    }

    public void start( Path configPath ) {
        start( configPath.toFile().exists() ? Parser.parse( configPath ) : Maps.of() );
    }

}
