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

import lombok.extern.slf4j.Slf4j;
import oap.reflect.Reflection;

import java.util.Optional;

@Slf4j
public class FieldLinkReflection implements LinkReflection {
    private final Reflection reflection;
    private final Object instance;
    private final String field;

    public FieldLinkReflection( Reflection reflection, Object instance, String field ) {
        this.reflection = reflection;
        this.instance = instance;
        this.field = field;
    }

    @Override
    public boolean set( Object value ) {
        Reflection.Field field = reflection.field( this.field ).orElse( null );
        if( field != null ) {
            field.set( instance, value );
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object get() {
        Reflection.Field field = reflection.field( this.field ).orElse( null );
        if( field != null ) {
            return field.get( instance );
        } else {
            return null;
        }
    }

}