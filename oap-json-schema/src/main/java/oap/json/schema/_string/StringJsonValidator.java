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
package oap.json.schema._string;

import oap.json.schema.JsonSchemaParserProperties;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaAST;
import oap.util.Either;
import oap.util.Lists;
import oap.util.OptionalList;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class StringJsonValidator implements JsonSchemaValidator<StringSchemaAST> {
    @Override
    public Either<List<String>, Object> validate( JsonValidatorProperties properties, StringSchemaAST schema,
        Object value ) {
        if( !(value instanceof String) ) return Either.left(
            Lists.of(
                properties.error( "instance is of type " + getType( value ) +
                    ", which is none of the allowed primitive types ([" + schema.common.schemaType +
                    "])" ) ) );

        String strValue = (String) value;

        Optional<String> minLengthResult = schema.minLength
            .filter( minLength -> strValue.length() < minLength )
            .map( minLength -> "string is shorter than minLength " + minLength );

        Optional<String> maxLengthResult = schema.maxLength
            .filter( maxLength -> strValue.length() > maxLength )
            .map( maxLength -> "string is longer than maxLength " + maxLength );

        Optional<String> patternResult = schema.pattern
            .filter( pattern -> !pattern.matcher( strValue ).matches() )
            .map( pattern -> "string does not match specified regex " + pattern );

        return OptionalList
            .<String>builder()
            .add( minLengthResult )
            .add( maxLengthResult )
            .add( patternResult )
            .toEigher( value );
    }

    @Override
    public SchemaAST parse( JsonSchemaParserProperties properties ) {
        SchemaAST.CommonSchemaAST common = node( properties ).asCommon();
        Optional<Integer> minLength = node( properties ).asInt( "minLength" ).optional();
        Optional<Integer> maxLength = node( properties ).asInt( "maxLength" ).optional();
        Optional<String> pattern = node( properties ).asString( "pattern" ).optional();

        return new StringSchemaAST( common, minLength, maxLength, pattern.map( Pattern::compile ) );
    }
}