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
package oap.json.schema._number;

import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;

import java.util.ArrayList;
import java.util.List;

public abstract class NumberJsonValidator<T extends Number> extends JsonSchemaValidator<NumberSchemaAST> {
   protected abstract boolean valid( Object value );

   @Override
   public List<String> validate( JsonValidatorProperties properties, NumberSchemaAST schema, Object value ) {
      if( !valid( value ) ) return typeFailed( properties, schema, value );

      Double doubleValue = ( ( Number ) value ).doubleValue();
      List<String> errors = new ArrayList<>();
      schema.minimum.filter( minimum -> doubleValue < minimum && !schema.exclusiveMinimum.orElse( false ) )
         .ifPresent( minimum -> errors.add( "number is lower than the required minimum " + minimum ) );

      schema.maximum.filter( maximum -> doubleValue > maximum && !schema.exclusiveMaximum.orElse( false ) )
         .ifPresent( maximum -> errors.add( "number is greater than the required maximum " + maximum ) );

      schema.minimum.filter( minimum -> doubleValue <= minimum && schema.exclusiveMinimum.orElse( false ) )
         .ifPresent( minimum -> errors.add( "number is not strictly greater than the required minimum " + minimum ) );

      schema.maximum.filter( maximum -> doubleValue >= maximum && schema.exclusiveMaximum.orElse( false ) )
         .ifPresent( maximum -> errors.add( "number is not strictly lower than the required maximum " + maximum ) );

      return errors;
   }

   @Override
   public NumberSchemaASTWrapper parse( JsonSchemaParserContext context ) {
      final NumberSchemaASTWrapper wrapper = context.createWrapper( NumberSchemaASTWrapper::new );

      wrapper.common = node( context ).asCommon();
      wrapper.exclusiveMinimum = node( context ).asBoolean( "exclusiveMinimum" ).optional();
      wrapper.exclusiveMaximum = node( context ).asBoolean( "exclusiveMaximum" ).optional();
      wrapper.minimum = node( context ).asDouble( "minimum" ).optional();
      wrapper.maximum = node( context ).asDouble( "maximum" ).optional();

      return wrapper;
   }
}
