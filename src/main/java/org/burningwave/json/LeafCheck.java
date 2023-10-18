package org.burningwave.json;

import java.util.function.Predicate;

import org.burningwave.json.Path.ValidationContext;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;

public class LeafCheck<S extends JsonSchema, T> extends Check.Abst<S, T, LeafCheck<S, T>> {
	
	LeafCheck(Class<S> jsonSchemaClass, Predicate<Path.ValidationContext<S,T>> predicate) {
		super(jsonSchemaClass, predicate);
	}
	
	public static class OfString extends LeafCheck<StringSchema, String>{

		OfString(Predicate<ValidationContext<StringSchema, String>> predicate) {
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