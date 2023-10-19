/*
 * This file is part of Burningwave JSON.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/JSON
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.burningwave.Strings;
import org.burningwave.Throwables;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

public class ValidationContext {
	static final String MOCK_SCHEMA_LABEL;
	protected static final Object logger;

	static {
		logger = SLF4J.INSTANCE.tryToInitLogger(ValidationContext.class);
		MOCK_SCHEMA_LABEL = Strings.INSTANCE.toStringWithRandomUUIDSuffix("schemaMock");
	}

	final Function<Path.ValidationContext<?, ?>, Function<String, Function<Object[], Throwable>>> exceptionBuilder;
	final ValidationConfig<?> validationConfig;
	final ObjectHandler inputHandler;
	final Collection<Throwable> exceptions;
	final Function<Path.ValidationContext<?, ?>, Consumer<Throwable>> exceptionAdder;
	final Collection<ObjectCheck> objectChecks;
	final Collection<IndexedObjectCheck<?>> indexedObjectChecks;
	final Collection<LeafCheck<?, ?>> leafChecks;

	ValidationContext(//NOSONAR
		Function<Path.ValidationContext<?, ?>, Function<String, Function<Object[], Throwable>>> exceptionBuilder,
		ValidationConfig<?> validationConfig,
		ObjectHandler jsonObjectWrapper,
		Collection<ObjectCheck> objectChecks,
		Collection<IndexedObjectCheck<?>> indexedObjectChecks,
		Collection<LeafCheck<?, ?>> leafChecks
	) {
		this.exceptionBuilder = exceptionBuilder;
		this.validationConfig = validationConfig;
		this.inputHandler = jsonObjectWrapper;
		this.exceptions = validationConfig.validateAll ? new ArrayList<>() : null;
		this.exceptionAdder = validationConfig.validateAll ?
			pathValidationContext -> exceptions::add :
			pathValidationContext -> exc -> {
				if (logger != null && validationConfig.isErrorLoggingEnabled()) {
					((org.slf4j.Logger)logger).debug(
						"Validation of path {} failed",
						pathValidationContext.path
					);
				}
				Throwables.INSTANCE.throwException(exc);
			};
		this.objectChecks = objectChecks;
		this.indexedObjectChecks = indexedObjectChecks;
		this.leafChecks = leafChecks;
	}

	void rejectValue(
		Path.ValidationContext<?, ?> pathValidationContext,
		String checkType,
		Object... messageArgs
	) {
		exceptionAdder.apply(
			pathValidationContext
		).accept(
			exceptionBuilder
			.apply(pathValidationContext)
			.apply(checkType)
			.apply(messageArgs)
		);
	}

	boolean checkValue(JsonSchema jsonSchema, Object value) {
		if (value != null) {
			if (!checkUnindexedObject(jsonSchema, value)) {
				return false;
			} else if (jsonSchema instanceof ArraySchema) {
				if (!(value instanceof Collection)) {
					return false;
				}
				JsonSchema itemsSchema = ((ArraySchema)jsonSchema).getItems().asSingleItems().getSchema();
				for (Object item : (Collection<?>)value) {
					if (!checkUnindexedObject(itemsSchema, item)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean checkUnindexedObject(JsonSchema jsonSchema, Object value) {
		return (jsonSchema instanceof ObjectSchema && value instanceof Map) ||
			(jsonSchema instanceof StringSchema && value instanceof String) ||
			(jsonSchema instanceof IntegerSchema && value instanceof Integer) ||
			 (jsonSchema instanceof NumberSchema && value instanceof Number) ||
			 (jsonSchema instanceof BooleanSchema && value instanceof Boolean);
	}

	public ObjectHandler getInputHandler() {
		return inputHandler;
	}

}