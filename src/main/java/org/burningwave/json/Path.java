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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.burningwave.Classes;
import org.burningwave.Strings;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

@SuppressWarnings("unchecked")
public class Path {
	public static final Path INSTANCE;

	public static class Segment {

		protected Segment() {}

		public static final String root = "";
		public static final String parent = "../";
		public static final String current = "./";

		public static final String toIndexed(String pathSegment, int... indexes) {
			StringBuilder indexedNameBuilder = new StringBuilder(pathSegment);
			indexedNameBuilder.append("[");
			for (int i = 0; i < indexes.length; i++) {
				indexedNameBuilder.append(indexes[i]);
				indexedNameBuilder.append(",");
			}
			String indexedName = indexedNameBuilder.toString();
			if (indexedName.endsWith(",")) {
				indexedName = indexedName.substring(0, indexedName.length()-1);
			}
			return indexedName + "]";
		}
	}

	private final Pattern multipleIndexesSearcher;
	private final Pattern singleIndexSearcher;
	private final Pattern pathSeparatorSearcher;
	private final Pattern unvalidCurrentOrParentDirectoryPlaceHolderSearcher;

	static {
		INSTANCE = new Path();
	}

	private Path(){
		multipleIndexesSearcher = Pattern.compile("\\[([\\d*\\s*,*]+)\\]");
		singleIndexSearcher = Pattern.compile("\\[([\\d*]+)\\]");
		pathSeparatorSearcher = Pattern.compile("\\.(?![\\/\\.])");
		unvalidCurrentOrParentDirectoryPlaceHolderSearcher = Pattern.compile("(?<![\\.\\/])\\.\\/|(?<!\\/)\\.\\.\\/");
	}

	public static final String of(String... pathSegments) {
		return String.join(
			".",
			Arrays.asList(pathSegments).stream().filter(value -> !value.isEmpty()).collect(Collectors.toList())
		).replace(Segment.parent + ".", Segment.parent)
		.replace(Segment.current + ".", Segment.current);
	}

	public String getName(String path) {
		String[] splittedPath = path.split("\\.");
		return splittedPath[splittedPath.length -1];
	}

	public String normalize(String basePath, String pathOrRelativePath) {
		String joinCharacter = "";
		if (basePath == null) {
			basePath = "";
		}
		if (pathOrRelativePath == null) {
			pathOrRelativePath = "";
		}
		if (basePath.isEmpty()) {
			if (pathOrRelativePath.startsWith(".")) {
				throw new IllegalArgumentException("Base path cannot be null if the path or relative parameter is a relative path");
			}
		} else {
			if (pathOrRelativePath.startsWith(".") || basePath.endsWith(".")) {
				joinCharacter = "/";
			} else if (!pathOrRelativePath.isEmpty()) {
				joinCharacter = ".";
			}
		}
		return normalize(String.join(joinCharacter, Arrays.asList(basePath, pathOrRelativePath)));
	}

	public String normalize(String path) {
		if (path == null) {
			throw new IllegalArgumentException("path is null");
		}
		String originalPath = path;
		if (unvalidCurrentOrParentDirectoryPlaceHolderSearcher.matcher(path).find()) {
			throw new IllegalArgumentException(path + " contains not valid " + Segment.current + " or " + Segment.parent + " references");
		}
		Matcher matcher = singleIndexSearcher.matcher(path);
		int placeHolderIndex = 1;
		Map<String, String> indexesHolder = new LinkedHashMap<>();
		while (matcher.find()) {
			String placeHolder = "${" + placeHolderIndex++ + "}";
			indexesHolder.put("/" + placeHolder, matcher.group(1));
			path = path.replaceFirst(singleIndexSearcher.pattern(), "/\\" + placeHolder);
		}
		path = Strings.INSTANCE.normalizePath(
			pathSeparatorSearcher.matcher(path).replaceAll("/")// Sostituiamo }. con /
		);
		if (path == null) {
			throw new IllegalArgumentException(originalPath + " is not a valid path");
		}
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		while(path.endsWith("/")) {
			path = path.substring(0, path.lastIndexOf("/"));
		}
		for (Map.Entry<String, String> replacement : indexesHolder.entrySet()) {
			path = path.replace(replacement.getKey(), "[" + replacement.getValue() + "]") ;
		}
		return path.replace("/", ".");
	}

	public List<Integer> getIndexes(String path) {
		Matcher matcher = singleIndexSearcher.matcher(path);
		List<Integer> indexes = new ArrayList<>();
		while (matcher.find()) {
			indexes.add(Integer.parseInt(matcher.group(1)));
		}
		return indexes;
	}

	public String toEndsWithRegEx(String value) {
		return ".*?" + toRegEx(value);
	}

	public String toStartsWithRegEx(String value) {
		return toRegEx(value) + ".*?";
	}

	public String toContainsRegEx(String value) {
		return ".*?" + toRegEx(value) + ".*?";
	}

	public String toRegEx(String value) {
		Matcher matcher = multipleIndexesSearcher.matcher(value);
		return multipleIndexesSearcher.splitAsStream(value)
			.map(piece ->
				piece + (matcher.find() ?
					"\\[(" +  String.join("|",  matcher.group(1).split(",")) + ")\\]":
					"")
			).collect(Collectors.joining())
			.replace(".", "\\.").replace("[]", "\\[.+?\\]");
	}

	public boolean isRoot(String path) {
		return Path.Segment.root.equals(path);
	}
	public interface Validation {
		public static class Context<S extends JsonSchema, T> {

			final org.burningwave.json.Validation.Context validationContext;
			final String path;
			final String name;
			final List<Integer> indexes;
			final S jsonSchema;
			final T rawValue;

			Context(org.burningwave.json.Validation.Context validationContext, String path, S jsonSchema, Object value) {
				this.validationContext = validationContext;
				this.path = path;
				this.jsonSchema = jsonSchema;
				this.name = Path.INSTANCE.getName(path);
				List<Integer> indexes = Path.INSTANCE.getIndexes(name);
				if (!indexes.isEmpty()) {
					this.indexes = Collections.unmodifiableList(indexes);
				} else {
					this.indexes = null;
				}
				String schemaDescription = jsonSchema.getDescription();
				if (org.burningwave.json.Validation.Context.MOCK_SCHEMA_LABEL.equals(schemaDescription) &&
					!validationContext.checkValue(jsonSchema, value)
				) {
					rejectValue("UNEXPECTED_TYPE", "unexpected type");
				}
				this.rawValue = (T)value;
			}

			public ObjectHandler getRootHandler() {
				return validationContext.getInputHandler();
			}

			public ObjectHandler getObjectHandler() {
				return getRootHandler().newFinder().findForPathEquals(this.path);
			}

			public void rejectValue(
				String checkType,
				String message,
				Object... messageArgs
			) {
				validationContext.rejectValue(this, checkType, message, messageArgs);
			}

			public static <S extends JsonSchema, T> java.util.function.Predicate<Path.Validation.Context<S, T>> predicateFor(
				Class<T> valueType,
				java.util.function.Predicate<Path.Validation.Context<S, T>> predicate
			) {
				return pathValidationContext ->
					(valueType == null || valueType.isInstance(pathValidationContext.rawValue)) &&
					predicate.test(pathValidationContext);
			}

			public boolean isFieldRequired() {
				return Optional.ofNullable(jsonSchema.getRequired()).orElseGet(() -> false);
			}

			public org.burningwave.json.Validation.Context getValidationContext() {
				return validationContext;
			}

			public S getJsonSchema() {
				return jsonSchema;
			}

			public String getPath() {
				return path;
			}

			public String getName() {
				return this.name;
			}

			public T getRawValue() {
				return rawValue;
			}

			public T getValue() {
				if (rawValue != null && !Classes.INSTANCE.isPrimitive(rawValue)) {
					getRootHandler().newValueFinder().findForPathEquals(this.path);
				}
				return rawValue;
			}

			public Integer getIndex() {
				return indexes != null ?
					indexes.get(indexes.size() - 1) : null;
			}

			public <V> V getParent() {
				return findValue(Path.Segment.parent);
			}

			public ObjectHandler getParentObjectHandler() {
				return findObjectHandler(Path.Segment.parent);
			}

			public <V> V findValue(String... pathSegmentsOrRelativePathSegments) {
				return getRootHandler().newValueFinder().findForPathEquals(
					resolvePath(pathSegmentsOrRelativePathSegments)
				);
			}

			public ObjectHandler findObjectHandler(String... pathSegmentsOrRelativePathSegments) {
				return getRootHandler().newFinder().findForPathEquals(
					resolvePath(pathSegmentsOrRelativePathSegments)
				);
			}

			public <V> V findValueAndConvert(Class<V> targetClass, String... pathSegmentsOrRelativePathSegments) {
				return getRootHandler().newValueFinderAndConverter(targetClass).findForPathEquals(
					resolvePath(pathSegmentsOrRelativePathSegments)
				);
			}

			protected String resolvePath(String... pathSegmentsOrRelativePathSegments) {
				String pathToFind = Path.of(pathSegmentsOrRelativePathSegments);
				if (!pathToFind.startsWith("/")) {
					return Path.INSTANCE.normalize(this.path, pathToFind);
				} else {
					return Path.INSTANCE.normalize(pathToFind);
				}
			}

			public boolean isRoot() {
				return Path.INSTANCE.isRoot(path);
			}

			@Override
			public String toString() {
				return
					path + " - " +
					jsonSchema.getClass().getSimpleName().replace("Schema", "") + ": " +
					Optional.ofNullable(rawValue).map(Object::toString).orElseGet(() -> "null");
			}

		}

	}

	abstract static class Predicate<P> implements java.util.function.Predicate<P> {
		Collection<Map.Entry<String, String>> pathForRegEx;

		private Predicate() {
			pathForRegEx = new ArrayList<>();
		}

		Predicate(String path, String pathRegEx) {
			this();
			pathForRegEx.add(new AbstractMap.SimpleEntry<>(path, pathRegEx));
		}

		@Override
		public Path.Predicate<P> and(java.util.function.Predicate<? super P> other) {
			return concat(other,
				input->
					test(input) && other.test(input)
			);
	    }

		@Override
		public Path.Predicate<P> or(java.util.function.Predicate<? super P> other) {
			return concat(other,
				input->
					test(input) || other.test(input)
			);
	    }

		@Override
		public Path.Predicate<P> negate() {
			return concat(null, input->
				!test(input)
			);
		}

		Path.Predicate<P> concat(java.util.function.Predicate<? super P> other, java.util.function.Predicate<? super P> finalPredicate) {
			Collection<Map.Entry<String, String>> otherPathForRegEx =
				(other instanceof  Path.Predicate)?
					((Path.Predicate<P>)other).pathForRegEx : null;

			Path.Predicate<P> pathPredicate = new Path.Predicate<P>() {
				@Override
				public boolean test(P input) {
					return finalPredicate.test(input);
				}
			};
			pathPredicate.pathForRegEx.addAll(this.pathForRegEx);
			if (otherPathForRegEx != null) {
				pathPredicate.pathForRegEx.addAll(otherPathForRegEx);
			}
			return pathPredicate;
		}
	}

}
