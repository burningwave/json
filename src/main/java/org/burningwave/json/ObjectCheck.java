package org.burningwave.json;

import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;

public class ObjectCheck extends Check.Abst<ObjectSchema, Map<String, Object>, ObjectCheck> {

	ObjectCheck(
		Predicate<Path.ValidationContext<ObjectSchema, Map<String, Object>>> predicate
	) {
		super(ObjectSchema.class, predicate);
	}

}
