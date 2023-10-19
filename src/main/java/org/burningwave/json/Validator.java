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

import static org.burningwave.json.Path.Segment.root;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.burningwave.Strings;
import org.burningwave.Throwables;
import org.json.JSONException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema.Items;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema.SingleItems;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

@SuppressWarnings({"unchecked"})
public class Validator {
	private static final String DEFAULT_LEAF_CHECKS_ID;
	private static final String DEFAULT_OBJECT_CHECKS_ID;
	private static final String DEFAULT_INDEXED_OBJECT_CHECKS_ID;
	protected final static Object logger;

	static {
		DEFAULT_LEAF_CHECKS_ID = Strings.INSTANCE.toStringWithRandomUUIDSuffix("defaultLeafChecks");
		DEFAULT_OBJECT_CHECKS_ID = Strings.INSTANCE.toStringWithRandomUUIDSuffix("defaultObjectChecks");
		DEFAULT_INDEXED_OBJECT_CHECKS_ID = Strings.INSTANCE.toStringWithRandomUUIDSuffix("defaultIndexedObjectChecks");
		logger = SLF4J.INSTANCE.tryToInitLogger(Validator.class);
	}



	protected final ObjectMapper objectMapper;
	protected final SchemaHolder schemaHolder;
	protected final Function<Path.ValidationContext<?, ?>, Function<String, Function<Object[], Throwable>>> exceptionBuilder;
	protected final Map<String, Collection<ObjectCheck>> objectChecks;
	protected final Map<String, Collection<IndexedObjectCheck<?>>> indexedObjectChecks;
	protected final Map<String, Collection<LeafCheck<?, ?>>> leafChecks;
	protected final Collection<LeafCheck<?, ?>> defaultLeafChecks;
	protected final Collection<ObjectCheck> defaultObjectChecks;
	protected final Collection<IndexedObjectCheck<?>> defaultIndexedObjectChecks;

	public Validator(
		SchemaHolder schemaHolder,
		Function<Path.ValidationContext<?, ?>, Function<String, Function<Object[], Throwable>>> exceptionBuilder
	) {
		this(
			schemaHolder.objectMapper,
			schemaHolder,
			exceptionBuilder
		);
	}

	public Validator(
		ObjectMapper objectMapper,
		SchemaHolder schemaHolder,
		Function<Path.ValidationContext<?, ?>, Function<String, Function<Object[], Throwable>>> exceptionBuilder
	) {
		this.objectMapper = objectMapper;
		this.schemaHolder = schemaHolder;
		this.exceptionBuilder = exceptionBuilder;
		this.leafChecks = new LinkedHashMap<>();
		this.leafChecks.put(
			DEFAULT_LEAF_CHECKS_ID,
			this.defaultLeafChecks = new ArrayList<>() //NOSONAR
		);
		this.objectChecks = new LinkedHashMap<>();
		this.objectChecks.put(
			DEFAULT_OBJECT_CHECKS_ID,
			this.defaultObjectChecks = new ArrayList<>() //NOSONAR
		);
		this.indexedObjectChecks = new LinkedHashMap<>();
		this.indexedObjectChecks.put(
			DEFAULT_INDEXED_OBJECT_CHECKS_ID,
			this.defaultIndexedObjectChecks = new ArrayList<>() //NOSONAR
		);
	}

	public synchronized void registerCheck(Check<?, ?, ?>... items) {
		registerCheck(null, items);
	}

	/*
	 * L'ordine di esecuzione controlli rispetta l'ordine in cui vengono registrati
	 * qualora l'input fornito sia di tipo raw (Map, Collection o tipi primitivi).
	 * Qualora l'input è invece rappresentato da un'oggetto di una classe
	 * non raw l'ordine in cui vengono eseguiti i controlli segue l'ordine
	 * in cui vengono definiti i campi all'interno di tale classe
	 */
	public synchronized void registerCheck(
		String checkGroupId,
		Check<?, ?, ?>... items
	) {
		for (Check<?, ?, ?> item : items) {
			Map<String, ?> checkMap = null;
			String defaultCheckListName = null;
			if (item instanceof ObjectCheck) {
				checkMap = this.objectChecks;
				defaultCheckListName = DEFAULT_OBJECT_CHECKS_ID;
			} else if (item instanceof IndexedObjectCheck) {
				checkMap = this.indexedObjectChecks;
				defaultCheckListName = DEFAULT_INDEXED_OBJECT_CHECKS_ID;
			} else if (item instanceof LeafCheck) {
				checkMap = this.leafChecks;
				defaultCheckListName = DEFAULT_LEAF_CHECKS_ID;
			}
			if (item instanceof Check.Group) {
				for (Check<?, ?, ?> nestedItem : ((Check.Group)item).items) {
					registerCheck(checkGroupId, nestedItem);
				}
			} else if (checkMap != null) {
				String classNameForRegistration = checkGroupId != null? checkGroupId : defaultCheckListName;
				Collection<Check<?, ?, ?>> checkList =
					((Map<String, Collection<Check<?, ?, ?>>>)checkMap)
						.computeIfAbsent(classNameForRegistration, clsName -> new ArrayList<>());
				checkList.add(item);
			} else {
				throw new IllegalArgumentException(item + " is not a valid check type");
			}
		}
	}

	public <I> Collection<Throwable> validate(
		ValidationConfig<I> config
	) {
		try {
			Object jsonObject = config.jsonObjectOrSupplier;
			if (jsonObject instanceof Supplier) {
				jsonObject = ((Supplier<?>)config.jsonObjectOrSupplier).get();
			}
			ObjectHandler objectHandler;
			if (jsonObject instanceof ObjectHandler) {
				objectHandler = (ObjectHandler)jsonObject;
				jsonObject = objectHandler.getRawValue();//NOSONAR
			} else {
				objectHandler = ObjectHandler.create(objectMapper, jsonObject);
			}
			ValidationContext validationContext = createValidationContext(
				config,
				objectHandler
			);
			if (ObjectHandler.isConvertible(objectHandler.getValue())) {
				validate(
					root,
					schemaHolder.getJsonSchema(objectHandler.getValue()),
					objectHandler.getRawValue(),
					validationContext
				);
			} else {
				validateRaw(
					validationContext
				);
			}
			return validationContext.exceptions;
		} catch (JSONException exc) {
			return Throwables.INSTANCE.throwException(exc);
		}
	}

	protected void validateRaw(ValidationContext validationContext) {
		validateRaw(
			validationContext,
			validationContext.objectChecks,
			check -> buildSchemaMock(check.schemaClass, null)
		);
		validateRaw(
			validationContext,
			(Collection)validationContext.indexedObjectChecks,
			(Function)check -> buildSchemaMock(((IndexedObjectCheck)check).schemaClass, ((IndexedObjectCheck)check).itemsSchemaClass)
		);
		validateRaw(
			validationContext,
			(Collection)validationContext.leafChecks,
			(Function)check -> buildSchemaMock(((LeafCheck)check).schemaClass, null)
		);
	}

	protected <S extends JsonSchema, T, C extends Check.Abst<S, T, C>> void validateRaw(
		ValidationContext validationContext,
		Collection<C> checkList,
		Function<C, S> schemaMockBuilder
	) {
		for (C check : checkList) {
			if (check.predicate instanceof Path.Predicate) {
				processPathCheck(validationContext, schemaMockBuilder, check);
			} else {
				processCheck(validationContext, schemaMockBuilder, check);
			}
		}
	}

	protected <S extends JsonSchema, T, C extends Check.Abst<S, T, C>> void processPathCheck(
		ValidationContext validationContext,
		Function<C, S> schemaMockBuilder,
		C check
	) {
		Path.Predicate<Path.ValidationContext<S,T>> pathPredicate =
			(Path.Predicate<Path.ValidationContext<S,T>>) check.predicate;
		Collection<ObjectHandler> objectHandlers = new ArrayList<>();
		Collection<Map.Entry<String, String>> pathForRegExColl = pathPredicate.pathForRegEx;
		for (Map.Entry<String, String> pathForRegEx : pathForRegExColl) {
			objectHandlers.addAll(validationContext.inputHandler.newFinder().findForPathMatches(pathForRegEx.getValue()));
		}
		if (!objectHandlers.isEmpty()) {
			for (ObjectHandler objectHandler : objectHandlers) {
				Path.ValidationContext<S,T> pathValidationContext =
					new Path.ValidationContext<>(
						validationContext,
						objectHandler.path,
						schemaMockBuilder.apply(check),
						objectHandler.rawValue
					);
				if (pathPredicate.test(pathValidationContext)) {
					check.action.accept(pathValidationContext);
				}
			}
		} else {
			for (Map.Entry<String, String> pathForRegEx : pathForRegExColl) {
				Path.ValidationContext<S,T> pathValidationContext =
					new Path.ValidationContext<>(validationContext, pathForRegEx.getKey(), schemaMockBuilder.apply(check), null);
				check.action.accept(pathValidationContext);
			}
		}
	}

	protected <S extends JsonSchema, T, C extends Check.Abst<S, T, C>> void processCheck(
		ValidationContext validationContext,
		Function<C, S> schemaMockBuilder,
		C check
	) {
		Collection<ObjectHandler> objectHandlers = validationContext.inputHandler.newFinder().findForPathMatches(".*?");
		S schemaMock = schemaMockBuilder.apply(check);
		for (ObjectHandler objectHandler : objectHandlers) {
			if (validationContext.checkValue(schemaMock, objectHandler.rawValue)) {
				Path.ValidationContext<S,T> pathValidationContext =
					new Path.ValidationContext<>(
						validationContext,
						objectHandler.path,
						schemaMock,
						objectHandler.rawValue
					);
				if (check.predicate.test(pathValidationContext)) {
					check.action.accept(pathValidationContext);
				}
			}
		}
	}

	protected <S extends JsonSchema> S buildSchemaMock(Class<S> schemaClass, Class<? extends JsonSchema> nestedTypeSchema) {
		S schema = null;
		if (schemaClass != null) {
			if (schemaClass.equals(ObjectSchema.class)) {
				schema = (S)new ObjectSchema();
			} else if (schemaClass.equals(StringSchema.class)) {
				schema = (S)new StringSchema();
			} else if (schemaClass.equals(IntegerSchema.class)) {
				schema = (S)new IntegerSchema();
			} else if (schemaClass.equals(NumberSchema.class)) {
				schema = (S)new NumberSchema();
			} else if (schemaClass.equals(BooleanSchema.class)) {
				schema = (S)new BooleanSchema();
			} else if (schemaClass.equals(ArraySchema.class)) {
				ArraySchema arraySchema = new ArraySchema();
				SingleItems singleItems = new SingleItems(buildSchemaMock(nestedTypeSchema, null));
				arraySchema.setItems(new Items() {
					@Override
					public SingleItems asSingleItems() {
						return singleItems;
					}
				});
				schema = (S)arraySchema;
			}
			if (schema != null) {
				schema.setDescription(ValidationContext.MOCK_SCHEMA_LABEL);
			}
		}
		return schema;
	}

	protected <I> void validate(
		String path,
		JsonSchema jsonSchema,
		Object jsonObject,
		ValidationContext validationContext
	) {
		if (jsonSchema instanceof ObjectSchema) {
			validate(
				path,
				(ObjectSchema)jsonSchema,
				(Map<String, Object>)jsonObject,
				validationContext
			);
		} else if (jsonSchema instanceof ArraySchema) {
			validate(
				path,
				(ArraySchema)jsonSchema,
				(Collection<I>)jsonObject,
				validationContext
			);
		} else {
			validateValue(
				path,
				jsonSchema,
				jsonObject,
				validationContext
			);
		}
	}

	protected void validate(
		String path,
		ObjectSchema jsonSchema,
		Map<String, Object> jSonObject,
		ValidationContext validationContext
	) {
		Path.ValidationContext<ObjectSchema,Map<String, Object>> pathValidationContext =
			new Path.ValidationContext<>(validationContext, path, jsonSchema, jSonObject);
		if (validationContext.validationConfig.pathFilter.test(pathValidationContext)) {
			tryToExecuteChecks(
				validationContext.objectChecks,
				pathValidationContext
			);
			for (Map.Entry<String, JsonSchema> descriptor : jsonSchema.getProperties().entrySet()) {
				JsonSchema iteratedItemSchema = descriptor.getValue();
				String iteratedPath = (!path.isEmpty() ?
					path + "." :
					path
				) + descriptor.getKey();
				validate(
					iteratedPath,
					iteratedItemSchema,
					getObject(
						() -> jSonObject::get,
						descriptor.getKey()
					),
					validationContext
				);
			}
		} else {
			logSkippingValidation(pathValidationContext);
		}
	}

	protected <I> I getObject(Supplier<Function<String, I>> fieldRetriever, String fieldName) {
		try {
			return fieldRetriever.get().apply(fieldName);
		} catch (NullPointerException exc) {//NOSONAR

		}
		return null;
	}

	protected <I, S extends JsonSchema, T, C extends Check.Abst<S, T, C>> void validate(
		String path,
		ArraySchema jsonSchema,
		Collection<I> jSonObject,
		ValidationContext validationContext
	) {
		Path.ValidationContext<ArraySchema,?> pathValidationContext =
			new Path.ValidationContext<>(validationContext, path, jsonSchema, jSonObject);
		if (validationContext.validationConfig.pathFilter.test(pathValidationContext)) {
			Stream<I> indexedObjectStream = jSonObject != null?
				(jSonObject).stream():
				Stream.of((I)null);
			Collection<C> checkList = (Collection<C>)validationContext.indexedObjectChecks;
			tryToExecuteChecks(
				checkList,
				(Path.ValidationContext<S, T>)pathValidationContext
			);
			JsonSchema itemSchema = jsonSchema.getItems().asSingleItems().getSchema();
			AtomicInteger index = new AtomicInteger(0);
			indexedObjectStream.forEach(value ->
				validate(
					path +"[" + index.getAndIncrement() + "]",
					itemSchema,
					value,
					validationContext
				)
			);
		} else {
			logSkippingValidation(pathValidationContext);
		}
	}

	protected <S extends JsonSchema, T, C extends Check.Abst<S, T, C>> void validateValue(
		String path,
		JsonSchema jsonSchema,
		Object value,
		ValidationContext validationContext
	) {
		Path.ValidationContext<?, ?> pathValidationContext = new Path.ValidationContext<>(validationContext, path, jsonSchema, value);
		if (validationContext.validationConfig.pathFilter.test(pathValidationContext)) {
			Collection<C> checkList = (Collection<C>)validationContext.leafChecks;
			tryToExecuteChecks(
				checkList,
				(Path.ValidationContext<S, T>)pathValidationContext
			);
		} else {
			logSkippingValidation(pathValidationContext);
		}
	}

	protected <S extends JsonSchema, T, C extends Check.Abst<S, T, C>> void tryToExecuteChecks(//NOSONAR
		Collection<C> checkList,
		Path.ValidationContext<S, T> pathValidationContext
	) {
		int executedChecks = 0;
		for (C check : checkList) {
			if (pathValidationContext.validationContext.validationConfig.checkFilter.apply(check).test(pathValidationContext) &&
				check.predicate.test(pathValidationContext)
			) {
				if (executedChecks++ == 0 && logger != null && pathValidationContext.validationContext.validationConfig.isDeepLoggingEnabled()) {
					((org.slf4j.Logger)logger).debug(
						"Starting validation of path {} with value {}",
						pathValidationContext.path,
						pathValidationContext.validationContext.inputHandler.valueToString(pathValidationContext.rawValue)//NOSONAR
					);
				}
				check.action.accept(pathValidationContext);
			}
		}
		if (executedChecks > 0) {
			if (pathValidationContext.validationContext.exceptions == null || pathValidationContext.validationContext.exceptions.isEmpty()) {
				if (logger != null && pathValidationContext.validationContext.validationConfig.isDeepLoggingEnabled()) {
					((org.slf4j.Logger)logger).debug(
						"Validation of path {} successfully completed",
						pathValidationContext.path
					);
				}
			} else if (logger != null && pathValidationContext.validationContext.validationConfig.isErrorLoggingEnabled()) {
				((org.slf4j.Logger)logger).debug(
					"Validation of path {} completed with errors",
					pathValidationContext.path
				);
			}
		} else if (logger != null && pathValidationContext.validationContext.validationConfig.isDeepLoggingEnabled()){
			((org.slf4j.Logger)logger).debug(
				"No custom check executed for path {} with value {}",
				pathValidationContext.path,
				pathValidationContext.validationContext.inputHandler.valueToString(pathValidationContext.rawValue)//NOSONAR
			);
		}
	}

	protected <I> ValidationContext createValidationContext(
		ValidationConfig<I> config,
		ObjectHandler objectHandler
	) {
		return new ValidationContext(
			exceptionBuilder,
			config,
			objectHandler,
			computeChecks(config.getGroupIds(), objectChecks, defaultObjectChecks),
			computeChecks(config.getGroupIds(), (Map)indexedObjectChecks, (Collection)defaultIndexedObjectChecks),
			computeChecks(config.getGroupIds(), (Map)leafChecks, (Collection)defaultLeafChecks)
		);
	}

	protected <C extends Check<?, ?, C>> Collection<C> computeChecks(
		Collection<String> groupIds,
		Map<String, Collection<C>> registeredChecks,
		Collection<C> defaultChecks
	) {
		return (Collection<C>)groupIds.stream().map(registeredChecks::get)
		.collect(
			Collectors.collectingAndThen(
				Collectors.toList(),
				list ->
					list.isEmpty() ? defaultChecks : list
			)
		);
	}

	protected void logSkippingValidation(
		Path.ValidationContext<?, ?> pathValidationContext
	) {
		if (logger != null && pathValidationContext.validationContext.validationConfig.isDeepLoggingEnabled()){
			((org.slf4j.Logger)logger).debug(
				"Skipping validation of path {} with value {}",
				pathValidationContext.path,
				pathValidationContext.validationContext.inputHandler.valueToString(pathValidationContext.rawValue)//NOSONAR
			);
		}
	}

}
