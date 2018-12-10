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

import lombok.val;
import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.Map;

import static oap.json.testng.JsonAsserts.assertJson;

/**
 * Created by igor.petrenko on 10.07.2018.
 */
public class ResourceSchemaStorageTest {
    @Test
    public void testGet() {
        val schema = ResourceSchemaStorage.INSTANCE.get( "/schema/test-schema.conf" );

        assertJson( Binder.json.marshal( Binder.hoconWithoutSystemProperties.unmarshal( Map.class, schema ) ) )
            .isEqualTo( "{\n"
                + "\t\"type\": \"object\",\n"
                + "\t\"properties\": {\n"
                + "\t\t\"a\": {\n"
                + "\t\t\t\"type\": \"string\"\n"
                + "\t\t}\n"
                + "\t}\n"
                + "}" );
    }

    @Test
    public void testGetWithExtends() {
        val schema = ResourceSchemaStorage.INSTANCE.get( "/schema/test-schema-1.conf" );

        assertJson( Binder.json.marshal( Binder.hoconWithoutSystemProperties.unmarshal( Map.class, schema ) ) )
            .isEqualTo( "{\n"
                + "\t\"type\": \"object\",\n"
                + "\t\"properties\": {\n"
                + "\t\t\"a\": {\n"
                + "\t\t\t\"type\": \"string\"\n"
                + "\t\t},\n"
                + "\t\t\"b\": {\n"
                + "\t\t\t\"type\": \"integer\"\n"
                + "\t\t}\n"
                + "\t}\n"
                + "}" );
    }
}