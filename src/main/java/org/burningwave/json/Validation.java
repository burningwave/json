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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.burningwave.Strings;
import org.burningwave.Throwables;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

public interface Validation {

	public static class Config<I> {
		private static final Predicate<Path.Validation.Context<?, ?>> DEFAULT_PATH_FILTER;
		private static final Function<Check<?, ?, ?>, Predicate<Path.Validation.Context<?, ?>>> DEFAULT_CHECK_FILTER;

		static {
			DEFAULT_PATH_FILTER =
				pathValidationContext ->
					true;
			DEFAULT_CHECK_FILTER = check ->
				pathValidationContext ->
					true;
		}

		I jsonObjectOrSupplier;
		Predicate<Path.Validation.Context<?, ?>> pathFilter;
		Function<Check<?, ?, ?>, Predicate<Path.Validation.Context<?, ?>>> checkFilter;
		boolean validateAll;
		int logMode;
		Collection<String> checkGroupIds;

		private Config(I jsonObject) {
			this.jsonObjectOrSupplier = jsonObject;
			this.pathFilter = DEFAULT_PATH_FILTER;
			this.checkFilter = DEFAULT_CHECK_FILTER;
			this.logMode = 1;
			this.checkGroupIds = new ArrayList<>();
		}

		public static <I> Config<I> forJsonObject(I jsonObject) {
			return new Config<>(jsonObject);
		}

		public Config<I> withPathFilter(Predicate<Path.Validation.Context<?, ?>> pathFilter) {
			this.pathFilter = pathFilter;
			return this;
		}

		public Config<I> withCheckFilter(Function<Check<?, ?, ?>, Predicate<Path.Validation.Context<?, ?>>> checkFilter) {
			this.checkFilter = checkFilter;
			return this;
		}

		public Config<I> withCompleteValidation() {
			this.validateAll = true;
			return this;
		}

		public Config<I> withExitStrategyAtFirstError() {
			this.validateAll = false;
			return this;
		}

		public Config<I> withTheseChecks(String... checkGroupIds) {
			this.checkGroupIds.addAll(Arrays.asList(checkGroupIds));
			return this;
		}

		public Config<I> disableLogging() {
			this.logMode = 0;
			return this;
		}

		public Config<I> enableDeepLogging() {
			this.logMode = 2;
			return this;
		}

		boolean isDeepLoggingEnabled() {
			return logMode == 2;
		}

		boolean isErrorLoggingEnabled() {
			return isDeepLoggingEnabled() || logMode == 1;
		}

		Collection<String> getGroupIds() {
			return checkGroupIds;
		}
	}

	public static class Context {
		static final String MOCK_SCHEMA_LABEL;
		protected static final Object logger;

		static {
			logger = SLF4J.INSTANCE.tryToInitLogger(Context.class);
			MOCK_SCHEMA_LABEL = Strings.INSTANCE.toStringWithRandomUUIDSuffix("schemaMock");
		}

		final Function<Path.Validation.Context<?, ?>, Function<String, Function<Object[], Throwable>>> exceptionBuilder;
		final Validation.Config<?> validationConfig;
		final ObjectHandler inputHandler;
		final Collection<Throwable> exceptions;
		final Function<Path.Validation.Context<?, ?>, Consumer<Throwable>> exceptionAdder;
		final Collection<ObjectCheck> objectChecks;
		final Collection<IndexedObjectCheck<?>> indexedObjectChecks;
		final Collection<LeafCheck<?, ?>> leafChecks;

		Context(
			Function<Path.Validation.Context<?, ?>, Function<String, Function<Object[], Throwable>>> exceptionBuilder,
			Validation.Config<?> validationConfig,
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
			Path.Validation.Context<?, ?> pathValidationContext,
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

	public static class Exception extends RuntimeException {
		private static final long serialVersionUID = 391707186751457489L;

		protected final String path;

		public Exception(String path, String message) {
			super(message);
			this.path = path;
		}

		public String getPath() {
			return path;
		}

	}

}
