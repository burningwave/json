package org.burningwave.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.burningwave.Strings;
import org.burningwave.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.BooleanSchema;
import com.fasterxml.jackson.module.jsonSchema.types.IntegerSchema;
import com.fasterxml.jackson.module.jsonSchema.types.NumberSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

public class ValidationContext {
	static final String MOCK_SCHEMA_LABEL;
	protected static final Logger logger;

	static {
		logger = LoggerFactory.getLogger(ValidationContext.class);
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
				if (validationConfig.isErrorLoggingEnabled()) {
					logger.debug(
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