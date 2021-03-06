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

package oap.testng;

import oap.io.IoStreams;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static oap.testng.Asserts.contentOfTestResource;
import static oap.testng.Asserts.pathOfTestResource;

/**
 * Created by Admin on 05.07.2016.
 */
public class AssertsTest {

    @Test
    public void testSortedContentOfFileResource() {
        Path unsorted = pathOfTestResource( getClass(), "random-flow-of-mind.txt" );
        String expected = contentOfTestResource( getClass(), "sorted-flow-of-mind.txt" );
        Asserts.assertFile( unsorted ).hasSortedLinesContent( expected, IoStreams.Encoding.PLAIN );
    }


}
