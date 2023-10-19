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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.burningwave.Classes;
import org.burningwave.TerminateIterationException;
import org.burningwave.Throwables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
public class ObjectHandler  {
	private static Function<ObjectHandler, Object> valueRetriever;
	static {
		try {
			if ((boolean)Configuration.freezeAndGet().get(Configuration.Key.REFLECTION_ENABLED)) {
				valueRetriever = buildValueRetriever(org.burningwave.reflection.FieldAccessor.INSTANCE);
			}
		} catch (Throwable exc) {//NOSONAR

		} finally {
			if (valueRetriever == null) {
				valueRetriever = buildAlwaysNullVaueIfNotValorizedRetriever();
			}
		}
	}

	static Function<ObjectHandler, Object> buildValueRetriever(
		org.burningwave.reflection.FieldAccessor fieldAccessor
	) {
		return objectHandler -> {
			Object value = objectHandler.rootHandler.getValue();
			for (String pathSegment : objectHandler.removeRootPrefix(objectHandler.path).split("\\.")) {//NOSONAR
				if (value == null) {
					break;
				}
				if (value instanceof Map) {
					pathSegment = "[" + pathSegment + "]";
				}
				try {
					value = fieldAccessor.get(value, pathSegment);
				} catch (IndexOutOfBoundsException exc) {
					value = null;
					break;
				}
			}
			return value;
		};
	}

	static Function<ObjectHandler, Object> buildAlwaysNullVaueIfNotValorizedRetriever() {
		return objectHandler ->
			objectHandler.rootHandler.rawValue == objectHandler.rootHandler.getValue() ?
				objectHandler.rawValue : null;
	}

	final ObjectMapper objectMapper;
	final ObjectHandler rootHandler;
	final String path;
	Supplier<Object> valueSupplier;
	final Object rawValue;

	ObjectHandler (
		ObjectMapper objectMapper,
		String path,
		ObjectHandler masterobjectHandler,
		Object convertedValue,
		Object originalValue
	) {
		this.objectMapper = objectMapper;
		this.path = path;
		if (masterobjectHandler == null && Path.Segment.root.equals(path)) {
			this.rootHandler = this;
		} else {
			this.rootHandler = masterobjectHandler;
		}
		this.rawValue = convertedValue;
		if (originalValue != null) {
			this.valueSupplier = () -> originalValue;
		} else {
			this.valueSupplier = () -> valueRetriever.apply(this);
		}
	}

	public static ObjectHandler create(
		ObjectMapper objectMapper,
		Object jsonObject
	) {
		if (isConvertible(jsonObject)) {
			if (jsonObject instanceof Collection) {
				return new ObjectHandler(
					objectMapper,
					Path.Segment.root,
					null,
					objectMapper.convertValue(jsonObject, new TypeReference<List<Object>>(){}),
					jsonObject
				);
			} else {
				return new ObjectHandler(
					objectMapper,
					Path.Segment.root,
					null,
					objectMapper.convertValue(jsonObject, new TypeReference<Map<String, Object>>(){}),
					jsonObject
				);
			}
		}
		return new ObjectHandler(objectMapper, Path.Segment.root, null, jsonObject, jsonObject);
	}

	static boolean isConvertible(Object jsonObject) {
		return !(
			Classes.INSTANCE.isPrimitive(jsonObject) ||
			jsonObject instanceof Map ||
			(jsonObject instanceof Collection && ((Collection<?>)jsonObject).stream().findFirst().map(Map.class::isInstance).orElseGet(() -> false))
		);
	}

	public Finder newFinder() {
		return new Finder(this);
	}

	public ValueFinder newValueFinder() {
		return new ValueFinder(this);
	}

	public <V> ValueFinderAndConverter<V> newValueFinderAndConverter(Class<V> outputClass) {
		return new ValueFinderAndConverter<>(this, outputClass);
	}

	protected <O> O findFirst(
		Object jSonObject,
		Predicate<ObjectHandler> filter,
		Function<ObjectHandler, O> converter
	) {
		List<O> collector = new ArrayList<>();
		try {
			findValues(
				isRoot() ?
					Path.Segment.root : "",
				jSonObject,
				objectHandler -> {
					boolean testResult = objectHandler.rawValue != null && filter.test(objectHandler);
					return new AbstractMap.SimpleEntry<>(testResult, testResult ? TerminateIterationException.INSTANCE : null);
				},
				converter,
				collector
			);
		} catch (TerminateIterationException exc) {//NOSONAR

		}
		return collector.stream().findFirst().orElseGet(() -> null);
	}

	protected <O> List<O> find(
		Object jSonObject,
		Predicate<ObjectHandler> filter,
		Function<ObjectHandler, O> converter
	) {
		List<O> collector = new ArrayList<>();
		findValues(
			isRoot() ?
				Path.Segment.root : "",
			jSonObject,
			objectHandler ->
				new AbstractMap.SimpleEntry<>(filter.test(objectHandler), null),
			converter,
			collector
		);
		return collector;
	}

	protected <I, O> void findValues(
		String path,
		Object jSonObject,
		Function<ObjectHandler, Map.Entry<Boolean, TerminateIterationException>> filter,
		Function<ObjectHandler, O> converter,
		Collection<O> collector
	) {
		if (jSonObject instanceof Map) {
			findValues(
				path,
				(Map<String, Object>)jSonObject,
				filter,
				converter,
				collector
			);
		} else if (jSonObject instanceof Collection) {
			findValues(
				path,
				(Collection<I>)jSonObject,
				filter,
				converter,
				collector
			);
		} else {
			checkAndCollectValue(
				path,
				jSonObject,
				filter,
				converter,
				collector
			);
		}
	}

	protected <O> void findValues(
		String path,
		Map<String, Object> jSonObject,
		Function<ObjectHandler, Map.Entry<Boolean, TerminateIterationException>> filter,
		Function<ObjectHandler, O> converter,
		Collection<O> collector
	) {
		checkAndCollectValue(path, jSonObject, filter, converter, collector);
		for (Map.Entry<String, Object> nameAndValue : jSonObject.entrySet()) {
			String iteratedPath = (!path.isEmpty() ?
				path + "." :
				path
			) + nameAndValue.getKey();
			findValues(
				iteratedPath,
				nameAndValue.getValue(),
				filter,
				converter,
				collector
			);
		}
	}

	protected <I, O> void findValues(
		String path,
		Collection<I> jSonObject,
		Function<ObjectHandler, Map.Entry<Boolean, TerminateIterationException>> filter,
		Function<ObjectHandler, O> converter,
		Collection<O> collector
	) {
		checkAndCollectValue(path, jSonObject, filter, converter, collector);
		Stream<I> indexedObjectStream = jSonObject != null?
			(jSonObject).stream():
			Stream.of();
		AtomicInteger index = new AtomicInteger(0);
		indexedObjectStream.forEach(vl -> findValues(
			path + "[" + index.getAndIncrement() + "]",
			vl,
			filter,
			converter,
			collector
		));
	}

	<O> void checkAndCollectValue(//NOSONAR
		String path,
		Object value,
		Function<ObjectHandler, Map.Entry<Boolean, TerminateIterationException>> filter,
		Function<ObjectHandler, O> converter,
		Collection<O> collector
	) {
		path = removeRootPrefix(path);
		String finalPath = this.path.isEmpty() ? path :
			path.isEmpty() ?//NOSONAR
				this.path :
				this.path + "." + path;
		ObjectHandler objectHandler = !Path.INSTANCE.isRoot(finalPath) ?
			new ObjectHandler(
				objectMapper,
				finalPath,
				rootHandler,
				value,
				null
			) :
			rootHandler;
		Map.Entry<Boolean, TerminateIterationException> testResult = filter.apply(objectHandler);
		if (testResult.getKey().booleanValue()) {
			collector.add(converter.apply(objectHandler));
		}
		TerminateIterationException terminateIterationException = testResult.getValue();
		if (terminateIterationException != null) {
			throw terminateIterationException;
		}
	}

	public <T> T getRawValue() {
		return (T)rawValue;
	}

	public <T> T getValue() {
		return (T)valueSupplier.get();
	}

	public <T> T getValueOrRawValue() {
		T value = getValue();
		return value != null ? value : getRawValue();
	}

	protected String removeRootPrefix(String path) {
		if (!Path.Segment.root.isEmpty() && path.startsWith(Path.Segment.root)) {
			path = path.replaceFirst(Path.Segment.root, "");//NOSONAR
			if (path.startsWith(".")) {
				path = path.substring(1);
			}
		}
		return path;
	}

	public String getPath() {
		return this.path;
	}

	public ObjectHandler getParent() {
		try {
			return rootHandler.newFinder().findForPathEquals(Path.INSTANCE.normalize(this.path, Path.Segment.parent));
		} catch (IllegalArgumentException exc) {
			return null;
		}
	}

	public boolean isRoot() {
		return Path.INSTANCE.isRoot(path);
	}

	public <I, T> T convert(Class<T> targetClass) {
		I originalValue = getValueOrRawValue();
		if (originalValue == null) {
			return null;
		}
		return objectMapper.convertValue(originalValue, targetClass);
	}

	@Override
	public String toString() {
		return
			path + " - " +
			Optional.ofNullable(rawValue).map(this::valueToString).orElseGet(() -> "null");
	}

	protected String valueToString(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exc) {
			return Throwables.INSTANCE.throwException(exc);
		}
	}

	public static final class Configuration {

		private Configuration() {}

		private static Map<String, Object> values;
		private static boolean freezed;

		public static final class Key {

			private Key() {}

			private static final String REFLECTION_ENABLED = "reflection.enabled";
		}

		static {
			values = new LinkedHashMap<>();
			putConfigurationValue(Configuration.Key.REFLECTION_ENABLED, true);
		}

		public static void disableReflection() {
			putConfigurationValue(Configuration.Key.REFLECTION_ENABLED, false);
		}

		private static void putConfigurationValue(String key, Object value) {
			try {
				values.put(key, value);
			} catch (UnsupportedOperationException exc) {
				throw new UnsupportedOperationException("Cannot add configuration value after that the " + ObjectHandler.class.getSimpleName() + " has been initialized");
			}
		}

		private static Map<String, Object> freezeAndGet() {
			if (!freezed) {
				values = Collections.unmodifiableMap(Configuration.values);
			}
			return values;
		}

	}

	abstract static class AbstFinder {
		private ObjectHandler objectHandler;

		AbstFinder(ObjectHandler objectHandler) {
			this.objectHandler = objectHandler;
		}

		public <V> List<V> find(Predicate<ObjectHandler> filter) {
			return objectHandler.find(objectHandler.rawValue, filter, this::convert);
		}

		public <V> V findFirst(Predicate<ObjectHandler> filter) {
			return objectHandler.findFirst(objectHandler.rawValue, filter, this::convert);
		}

		public <V> V findForPathEquals(String... pathSegments) {
			return findFirst(oW -> oW.path.equals(Path.of(pathSegments)));
		}

		public <V> V findFirstForPath(Predicate<String> pathPredicate) {
			return findFirst(oW -> pathPredicate.test(oW.path));
		}

		public <V> V findFirstForPathStartsWith(String... pathSegments) {
			return findFirstForPathMatches(Path.INSTANCE.toStartsWithRegEx(Path.of(pathSegments)));
		}

		public <V> V findFirstForPathEndsWith(String... pathSegments) {
			return findFirstForPathMatches(Path.INSTANCE.toEndsWithRegEx(Path.of(pathSegments)));
		}

		public <V> V findFirstForPathContains(String... pathSegments) {
			return findFirstForPathMatches(Path.INSTANCE.toContainsRegEx(Path.of(pathSegments)));
		}

		public <V> V findFirstForPathMatches(String regEx) {
			return findFirst(oW -> oW.path.matches(regEx));
		}

		public <I, V> V findFirstForValue(Predicate<I> valuePredicate) {
			return findFirst(oW -> valuePredicate.test(oW.getValueOrRawValue()));
		}

		public <V> List<V> findForPathStartsWith(String... pathSegments) {
			return findForPathMatches(Path.INSTANCE.toStartsWithRegEx(Path.of(pathSegments)));
		}

		public <V> List<V> findForPathEndsWith(String... pathSegments) {
			return findForPathMatches(Path.INSTANCE.toEndsWithRegEx(Path.of(pathSegments)));
		}

		public <V> List<V> findForPathContains(String... pathSegments) {
			return findForPathMatches(Path.INSTANCE.toContainsRegEx(Path.of(pathSegments)));
		}

		public <V> List<V> findForPathMatches(String regEx) {
			return find(oW -> oW.path.matches(regEx));
		}

		public <V> List<V> findForPath(Predicate<String> pathPredicate) {
			return find(oW -> pathPredicate.test(oW.path));
		}

		public <I, V> List<V> findForValue(Predicate<I> valuePredicate) {
			return find(oW -> valuePredicate.test(oW.getValueOrRawValue()));
		}

		<V> List<V> convert(List<ObjectHandler> founds) {
			return (List<V>)founds.stream().map(this::convert).collect(Collectors.toList());
		}

		abstract <V> V convert(ObjectHandler found);

	}

	public static class Finder extends AbstFinder {

		Finder(ObjectHandler objectHandler) {
			super(objectHandler);
		}

		@Override
		ObjectHandler convert(ObjectHandler found) {
			return found;
		}

	}

	public static class ValueFinder extends AbstFinder {

		ValueFinder(ObjectHandler objectHandler) {
			super(objectHandler);
		}

		@Override
		<V> V convert(ObjectHandler found) {
			return found.getValueOrRawValue();
		}

	}

	public static class ValueFinderAndConverter<V> extends AbstFinder {

		private Class<V> outputClass;

		ValueFinderAndConverter(ObjectHandler objectHandler, Class<V> outputClass) {
			super(objectHandler);
			this.outputClass = outputClass;
		}

		@Override
		V convert(ObjectHandler found) {
			return found.convert(outputClass);
		}

		public ValueFinderAndConverter<V> changeOutputClass(Class<V> outputClass) {
			this.outputClass = outputClass;
			return this;
		}

	}

}