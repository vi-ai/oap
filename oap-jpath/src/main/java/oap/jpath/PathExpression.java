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

package oap.jpath;

import lombok.ToString;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by igor.petrenko on 2020-06-09.
 */
@ToString
public class PathExpression {
    public final ArrayList<PathNode> list = new ArrayList<>();

    public void add( PathNode path ) {
        list.add( path );
    }

    @SuppressWarnings( "unchecked" )
    public void evaluate( Map<String, Object> variables, JPathOutput output ) {
        Pointer pointer = new MapPointer( ( Map<Object, Object> ) ( Object ) variables );

        for( var n : list ) pointer = pointer.resolve( n );

        output.write( pointer );
    }
}
