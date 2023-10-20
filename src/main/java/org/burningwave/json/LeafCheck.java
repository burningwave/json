/*
 * This file is part of Burningwave JSON.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/json
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Roberto Gentili
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.burningwave.json;

import java.util.function.Predicate;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

public class LeafCheck<S extends JsonSchema, T> extends Check.Abst<S, T, LeafCheck<S, T>> {

	LeafCheck(Class<S> jsonSchemaClass, Predicate<Path.Validation.Context<S,T>> predicate) {
		super(jsonSchemaClass, predicate);
	}

	public static class OfString extends LeafCheck<StringSchema, String>{

		OfString(Predicate<Path.Validation.Context<StringSchema, String>> predicate) {
			super(StringSchema.class, predicate);
		}

		public OfString checkNotEmpty() {
			return (OfString)execute(pathValidationContext -> {
				if (pathValidationContext.getRawValue().isEmpty()) {
					pathValidationContext.rejectValue(Check.Error.IS_EMPTY);
				}
			});
		}

	}

}