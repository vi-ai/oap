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

package oap.logstream;

import oap.logstream.exceptions.LoggerException;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by igor.petrenko on 19.06.2017.
 */
public class LoggingEvent {
    protected final ConcurrentLinkedQueue<LoggerListener> listeners = new ConcurrentLinkedQueue<>();

    public void addListener( LoggerListener listener ) {
        listeners.add( listener );
    }

    public void removeListener( LoggerListener listener ) {
        listeners.remove( listener );
    }

    protected void fireError( String msg ) {
        listeners.forEach( listener -> listener.error( msg ) );
    }

    protected void fireError( LoggerException exception ) {
        fireError( exception.getMessage() );
    }
}
