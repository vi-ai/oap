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
package oap.ws.validate;

import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.ws.WsException;

import java.util.Objects;

public class MethodValidatorPeer implements ValidatorPeer {

    private final Reflection.Method method;
    private final Object instance;

    public MethodValidatorPeer( Validate validate, Object instance ) {
        this.instance = instance;
        this.method = Reflect.reflect( instance.getClass() )
            .method( m -> Objects.equals( m.name(), validate.value() ) )
            .orElseThrow( () -> new WsException( "no such method " + validate.value() ) );
    }

    @Override
    public ValidationErrors validate( Object value ) {
        if( value != null && value.getClass().isArray() )
            return method.invoke( instance, (Object[]) value );
        else
            return method.invoke( instance, value );
    }
}