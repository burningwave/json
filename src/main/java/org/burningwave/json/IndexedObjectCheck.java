package org.burningwave.json;

import java.util.List;
import java.util.function.Predicate;

import org.burningwave.json.Path.ValidationContext;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;

public class IndexedObjectCheck<I> extends Check.Abst<ArraySchema, List<I>, IndexedObjectCheck<I>> {
	Class<? extends JsonSchema> itemsSchemaClass;
	
	IndexedObjectCheck(
		Class<? extends JsonSchema> itemsSchemaClass,
		Predicate<Path.ValidationContext<ArraySchema, List<I>>> predicate
	) {
		super(ArraySchema.class, predicate);
		this.itemsSchemaClass = itemsSchemaClass;
	}
	
	@Override
	Predicate<ValidationContext<ArraySchema, List<I>>> buildBasicPredicate(Class<? extends JsonSchema> jsonSchemaClass) {
		return pathValidationContext ->
			pathValidationContext.jsonSchema instanceof ArraySchema &&
				(itemsSchemaClass == null || itemsSchemaClass.isInstance((pathValidationContext.jsonSchema).getItems().asSingleItems().getSchema())) ;
	}
	
}
