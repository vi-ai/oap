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

package oap.application.link;

import java.util.ListIterator;

/**
 * Created by igor.petrenko on 08.01.2019.
 */
public class ListLinkReflection implements LinkReflection {
    private final ListIterator<Object> iterator;
    private Object value = null;
    private boolean init = false;
    private boolean set = false;

    public ListLinkReflection( ListIterator<Object> iterator ) {
        this.iterator = iterator;
    }

    @Override
    public boolean set( Object value ) {
        get();
        if( value == null ) {
            iterator.remove();
            return false;
        } else {
            if( !set ) {
                iterator.set( value );
                set = true;
            } else {
                iterator.add( value );
            }
            return true;
        }
    }

    @Override
    public Object get() {
        if( init ) return value;
        value = iterator.next();
        init = true;
        return value;
    }
}