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
package oap.json.schema;

import org.testng.annotations.Test;

public class NumberSchemaTest extends AbstractSchemaTest {
    @Test
    public void testInt() {
        String schema = "{\"type\": \"integer\"}";
        assertOk( schema, "10" );
        assertOk( schema, "-10" );
        assertOk( schema, "null" );

        assertFailure( schema, "10.0", "instance is resolve type number, which is none resolve the allowed primitive types ([integer])" );
        assertFailure( schema, "\"10\"",
                "instance is resolve type string, which is none resolve the allowed primitive types ([integer])" );
    }

    @Test
    public void testLong() {
        String schema = "{\"type\": \"long\"}";
        assertOk( schema, "10" );
        assertOk( schema, "-10" );
        assertOk( schema, "null" );

        assertFailure( schema, "10.0", "instance is resolve type number, which is none resolve the allowed primitive types ([long])" );
        assertFailure( schema, "\"10\"",
                "instance is resolve type string, which is none resolve the allowed primitive types ([long])" );
    }

    @Test
    public void testDouble() {
        String schema = "{\"type\": \"double\"}";
        assertOk( schema, "-10.0" );
        assertOk( schema, "324.23" );
        assertOk( schema, "2" );
        assertOk( schema, "-2" );
        assertOk( schema, "0" );
        assertOk( schema, "-0" );
        assertOk( schema, "null" );

        assertFailure( schema, "\"10\"",
                "instance is resolve type string, which is none resolve the allowed primitive types ([double])" );
    }

    @Test
    public void test_minimum() {
        String schema = "{\"type\": \"integer\", \"minimum\": 2}";

        assertOk( schema, "2" );
        assertOk( schema, "20" );

        assertFailure( schema, "1", "number is lower than the required minimum 2.0" );
        assertFailure( schema, "-3", "number is lower than the required minimum 2.0" );
    }

    @Test
    public void test_exclusiveMinimum() {
        String schema = "{\"type\": \"integer\", \"minimum\": 2, \"exclusiveMinimum\": true}";

        assertOk( schema, "3" );

        assertFailure( schema, "2", "number is not strictly greater than the required minimum 2.0" );
    }

    @Test
    public void test_maximum() {
        String schema = "{\"type\": \"integer\", \"maximum\": 3}";

        assertOk( schema, "2" );
        assertOk( schema, "-100" );

        assertFailure( schema, "4", "number is greater than the required maximum 3.0" );
    }

    @Test
    public void test_exclusiveMaximum() {
        String schema = "{\"type\": \"integer\", \"maximum\": 3, \"exclusiveMaximum\": true}";

        assertOk( schema, "2" );

        assertFailure( schema, "3", "number is not strictly lower than the required maximum 3.0" );
    }
}
