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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.burningwave.Strings;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

@SuppressWarnings("unchecked")
public interface Check<S extends JsonSchema, T, O extends Check<S, T, O>> {

	public O execute(Consumer<Path.Validation.Context<S, T>> action);

	public enum Error {
		UNEXPECTED_TYPE,
		IS_NULL,
		IS_EMPTY,
		NOT_IN_RANGE;

		public String key() {
			return Strings.INSTANCE.underscoredToCamelCase(name());
		}
	}

	public default O checkMandatory() {
		return execute(pathValidationContext -> {
			if (pathValidationContext.isFieldRequired() && pathValidationContext.getRawValue() == null) {
				pathValidationContext.rejectValue(Check.Error.IS_NULL);
			}
		});
	}

// All
	public static Group forAll() {
		return Group.of(
			whenObject(null),
			whenIndexedObject(null),
			whenValue(null)
		);
	}

	public static <O> Group when(Predicate<Path.Validation.Context<JsonSchema, O>> predicate) {
		return Group.of(
			whenObject((Predicate)predicate),
			whenIndexedObject((Predicate)predicate),
			whenValue((Predicate)predicate)
		);
	}

	public static Group whenPathStartsWith(String... pathSegments) {
		return Group.of(
			whenObjectPathStartsWith(pathSegments),
			whenIndexedObjectPathStartsWith(pathSegments),
			whenValuePathStartsWith(pathSegments)
		);
	}

	public static Group whenPathEndsWith(String... pathSegments) {
		return Group.of(
			whenObjectPathEndsWith(pathSegments),
			whenIndexedObjectPathEndsWith(pathSegments),
			whenValuePathEndsWith(pathSegments)
		);
	}

	public static Group whenPathContains(String... pathSegments) {
		return Group.of(
			whenObjectPathContains(pathSegments),
			whenIndexedObjectPathContains(pathSegments),
			whenValuePathContains(pathSegments)
		);
	}

	public static Group whenPathEquals(String... pathSegments) {
		return Group.of(
			whenObjectPathEquals(pathSegments),
			whenIndexedObjectPathEquals(pathSegments),
			whenValuePathEquals(pathSegments)
		);
	}

// Value
	public static LeafCheck<JsonSchema, Object> forAllValues() {
		return whenValue(null);
	}

	public static LeafCheck<JsonSchema, Object> whenValue(Predicate<Path.Validation.Context<JsonSchema, Object>> predicate) {
		return new LeafCheck<>(JsonSchema.class, predicate);
	}

	public static LeafCheck<JsonSchema, Object> whenValuePathStartsWith(String... pathSegments) {
		return new LeafCheck<>(
			JsonSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static LeafCheck<JsonSchema, Object> whenValuePathEndsWith(String... pathSegments) {
		return new LeafCheck<>(
			JsonSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static LeafCheck<JsonSchema, Object> whenValuePathContains(String... pathSegments) {
		return new LeafCheck<>(
			JsonSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static LeafCheck<JsonSchema, Object> whenValuePathEquals(String... pathSegments) {
		return new LeafCheck<>(
			JsonSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	// String
	public static LeafCheck<StringSchema, String> forAllStringValues() {
		return whenStringValue(null);
	}

	public static LeafCheck.OfString whenStringValue(Predicate<Path.Validation.Context<StringSchema,String>> predicate) {
		return new LeafCheck.OfString(predicate);
	}

	public static LeafCheck<StringSchema, String> whenStringValuePathStartsWith(String... pathSegments) {
		return new LeafCheck.OfString(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static LeafCheck<StringSchema, String> whenStringValuePathEndsWith(String... pathSegments) {
		return new LeafCheck.OfString(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static LeafCheck<StringSchema, String> whenStringValuePathContains(String... pathSegments) {
		return new LeafCheck.OfString(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static LeafCheck<StringSchema, String> whenStringValuePathEquals(String... pathSegments) {
		return new LeafCheck.OfString(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	// Integer
	public static LeafCheck<IntegerSchema, Integer> forAllIntegerValues() {
		return whenIntegerValue(null);
	}

	public static LeafCheck<IntegerSchema, Integer> whenIntegerValue(Predicate<Path.Validation.Context<IntegerSchema,Integer>> predicate) {
		return new LeafCheck<>(IntegerSchema.class, predicate);
	}

	public static LeafCheck<IntegerSchema, String> whenIntegerValuePathStartsWith(String... pathSegments) {
		return new LeafCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static LeafCheck<IntegerSchema, String> whenIntegerValuePathEndsWith(String... pathSegments) {
		return new LeafCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static LeafCheck<IntegerSchema, String> whenIntegerValuePathContains(String... pathSegments) {
		return new LeafCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static LeafCheck<IntegerSchema, String> whenIntegerValuePathEquals(String... pathSegments) {
		return new LeafCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	// Number
	public static LeafCheck<NumberSchema, Number> forAllNumberValues() {
		return whenNumberValue(null);
	}

	public static LeafCheck<NumberSchema, Number> whenNumberValue(Predicate<Path.Validation.Context<NumberSchema,Number>> predicate) {
		return new LeafCheck<>(NumberSchema.class, predicate);
	}

	public static LeafCheck<NumberSchema, String> whenNumberValuePathStartsWith(String... pathSegments) {
		return new LeafCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static LeafCheck<NumberSchema, String> whenNumberValuePathEndsWith(String... pathSegments) {
		return new LeafCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static LeafCheck<NumberSchema, String> whenNumberValuePathContains(String... pathSegments) {
		return new LeafCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static LeafCheck<NumberSchema, String> whenNumberValuePathEquals(String... pathSegments) {
		return new LeafCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	//Boolean
	public static LeafCheck<BooleanSchema, Boolean> forAllBooleanValues() {
		return whenBooleanValue(null);
	}

	public static LeafCheck<BooleanSchema, Boolean> whenBooleanValue(Predicate<Path.Validation.Context<BooleanSchema,Boolean>> predicate) {
		return new LeafCheck<>(BooleanSchema.class, predicate);
	}

	public static LeafCheck<BooleanSchema, String> whenBooleanValuePathStartsWith(String... pathSegments) {
		return new LeafCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static LeafCheck<BooleanSchema, String> whenBooleanValuePathEndsWith(String... pathSegments) {
		return new LeafCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static LeafCheck<BooleanSchema, String> whenBooleanValuePathEquals(String... pathSegments) {
		return new LeafCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static LeafCheck<BooleanSchema, String> whenBooleanValuePathContains(String... pathSegments) {
		return new LeafCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

// Object
	public static ObjectCheck forAllObjects() {
		return whenObject(null);
	}

	public static ObjectCheck whenObject(Predicate<Path.Validation.Context<ObjectSchema, Map<String, Object>>> predicate) {
		return new ObjectCheck(predicate);
	}

	public static ObjectCheck whenObjectPathStartsWith(String... pathSegments) {
		return new ObjectCheck(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static ObjectCheck whenObjectPathEndsWith(String... pathSegments) {
		return new ObjectCheck(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static ObjectCheck whenObjectPathContains(String... pathSegments) {
		return new ObjectCheck(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static ObjectCheck whenObjectPathEquals(String... pathSegments) {
		return new ObjectCheck(
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

// Indexed objects
	public static <I> IndexedObjectCheck<I> forAllIndexedObjects() {
		return whenIndexedObject(null);
	}

	public static <I> IndexedObjectCheck<I> whenIndexedObject(Predicate<Path.Validation.Context<ArraySchema, List<I>>> predicate) {
		return new IndexedObjectCheck<>(null, predicate);
	}

	public static <I> IndexedObjectCheck<I> whenIndexedObjectPathStartsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			null,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static <I> IndexedObjectCheck<I> whenIndexedObjectPathEndsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			null,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static <I> IndexedObjectCheck<I> whenIndexedObjectPathContains(String... pathSegments) {
		return new IndexedObjectCheck<>(
			null,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static <I> IndexedObjectCheck<I> whenIndexedObjectPathEquals(String... pathSegments) {
		return new IndexedObjectCheck<>(
			null,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	// Indexed strings
	public static IndexedObjectCheck<String> forAllIndexedStrings() {
		return whenIndexedStrings(null);
	}

	public static IndexedObjectCheck<String> whenIndexedStrings(Predicate<Path.Validation.Context<ArraySchema, List<String>>> predicate) {
		return new IndexedObjectCheck<>(StringSchema.class, predicate);
	}

	public static IndexedObjectCheck<String> whenIndexedStringsPathStartsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			StringSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<String> whenIndexedStringsPathEndsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			StringSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<String> whenIndexedStringsPathContains(String... pathSegments) {
		return new IndexedObjectCheck<>(
			StringSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static IndexedObjectCheck<String> whenIndexedStringsPathEquals(String... pathSegments) {
		return new IndexedObjectCheck<>(
			StringSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	// Indexed integers
	public static IndexedObjectCheck<Integer> forAllIndexedIntegers() {
		return whenIndexedIntegers(null);
	}

	public static IndexedObjectCheck<Integer> whenIndexedIntegers(Predicate<Path.Validation.Context<ArraySchema, List<Integer>>> predicate) {
		return new IndexedObjectCheck<>(IntegerSchema.class, predicate);
	}

	public static IndexedObjectCheck<Integer> whenIndexedIntegersPathStartsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<Integer> whenIndexedIntegersPathEndsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<Integer> whenIndexedIntegersPathContains(String... pathSegments) {
		return new IndexedObjectCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static IndexedObjectCheck<Integer> whenIndexedIntegersPathEquals(String... pathSegments) {
		return new IndexedObjectCheck<>(
			IntegerSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	// Indexed numbers
	public static IndexedObjectCheck<Number> forAllIndexedNumbers() {
		return whenIndexedNumbers(null);
	}

	public static IndexedObjectCheck<Number> whenIndexedNumbers(Predicate<Path.Validation.Context<ArraySchema, List<Number>>> predicate) {
		return new IndexedObjectCheck<>(NumberSchema.class, predicate);
	}

	public static IndexedObjectCheck<Number> whenIndexedNumbersPathStartsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<Number> whenIndexedNumbersPathEndsWith(String... pathSegments) {
		return new IndexedObjectCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<Number> whenIndexedNumbersPathContains(String... pathSegments) {
		return new IndexedObjectCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static IndexedObjectCheck<Number> whenIndexedNumbersPathEquals(String... pathSegments) {
		return new IndexedObjectCheck<>(
			NumberSchema.class,
			Abst.buildPathPredicate(Path.of(pathSegments), Path.INSTANCE::toRegEx)
 		);
	}

	// Indexed boolean
	public static IndexedObjectCheck<Boolean> forAllIndexedBooleans() {
		return whenIndexedBooleans(null);
	}

	public static IndexedObjectCheck<Boolean> whenIndexedBooleans(Predicate<Path.Validation.Context<ArraySchema, List<Boolean>>> predicate) {
		return new IndexedObjectCheck<>(BooleanSchema.class, predicate);
	}

	public static IndexedObjectCheck<Boolean> whenIndexedBooleansPathStartsWith(String path) {
		return new IndexedObjectCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(path, Path.INSTANCE::toStartsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<Boolean> whenIndexedBooleansPathEndsWith(String path) {
		return new IndexedObjectCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(path, Path.INSTANCE::toEndsWithRegEx)
 		);
	}

	public static IndexedObjectCheck<Boolean> whenIndexedBooleansPathContains(String path) {
		return new IndexedObjectCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(path, Path.INSTANCE::toContainsRegEx)
 		);
	}

	public static IndexedObjectCheck<Boolean> whenIndexedBooleansPathEquals(String path) {
		return new IndexedObjectCheck<>(
			BooleanSchema.class,
			Abst.buildPathPredicate(path, Path.INSTANCE::toRegEx)
 		);
	}

	abstract class Abst<S extends JsonSchema, T, C extends Abst<S, T, C>> implements Check<S, T, C> {
		Class<S> schemaClass;
		Predicate<Path.Validation.Context<S,T>> predicate;
		Consumer<Path.Validation.Context<S,T>> action;

		Abst(Class<S> jsonSchemaClass, Predicate<Path.Validation.Context<S,T>> predicate) {
			this.schemaClass = jsonSchemaClass;
			this.predicate = buildBasicPredicate(jsonSchemaClass);
			if (predicate != null) {
				this.predicate = this.predicate.and(predicate);
			}
		}

		Predicate<Path.Validation.Context<S,T>> buildBasicPredicate(Class<? extends JsonSchema> jsonSchemaClass) {
			return pathValidationContext -> jsonSchemaClass.isInstance(pathValidationContext.jsonSchema);
		}

		static <S extends JsonSchema, T> Predicate<Path.Validation.Context<S,T>> buildPathPredicate(String path, UnaryOperator<String> pathProcessor) {
			String pathRegEx = pathProcessor != null ?
						pathProcessor.apply(path) : path;
			return new Path.Predicate<Path.Validation.Context<S,T>>(path, pathRegEx) {
				@Override
				public boolean test(Path.Validation.Context<S, T> pathValidationContext) {
					return pathValidationContext.path.matches(pathRegEx);
				}

			};
		}

		@Override
		public synchronized C execute(Consumer<Path.Validation.Context<S,T>> action) {
			if (this.action != null) {
				this.action = this.action.andThen(action);
			} else {
				this.action = action;
			}
			return (C)this;
		}

	}

	public static class Group implements Check<JsonSchema, Object, Group> {
		Collection<Check<?, ?, ?>> items;

		public Group(Check<?, ?, ?>... items) {
			this.items = new ArrayList<>(Arrays.asList(items));
		}

		public static Check.Group of(Check<?, ?, ?>... items) {
			return new Check.Group(items);
		}

		@Override
		public Check.Group execute(Consumer<Path.Validation.Context<JsonSchema, Object>> action) {
			for (Check<?, ?, ?> item : this.items) {
				((Abst<?, ?, ?>)item).action = (Consumer)action;
			}
			return this;
		}


	}

}