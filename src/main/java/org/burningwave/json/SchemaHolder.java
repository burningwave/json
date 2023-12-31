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

import java.util.LinkedHashMap;
import java.util.Map;

import org.burningwave.Throwables;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;


@SuppressWarnings("unchecked")
public class SchemaHolder {
	private final Map<Class<?>, JsonSchema> jsonSchemasForClasses;
	final ObjectMapper objectMapper;
	final JsonSchemaGenerator jsonSchemaGenerator;

	public SchemaHolder(
		ObjectMapper objectMapper
	) {
		this.objectMapper = objectMapper;
		SchemaFactoryWrapper schemaFactoryWrapper = new SchemaFactoryWrapper();
	    schemaFactoryWrapper.setVisitorContext(new VisitorContext() {
	    	@Override
	        public String addSeenSchemaUri(JavaType aSeenSchema) {
	            return javaTypeToUrn(aSeenSchema);
	        }
	    });
	    jsonSchemaGenerator = new JsonSchemaGenerator(this.objectMapper, schemaFactoryWrapper);
	    jsonSchemasForClasses = new LinkedHashMap<>();
	}

	public <I> JsonSchema getJsonSchema(I jsonObject) {
		Class<I> jsonObjectClass = (Class<I>)jsonObject.getClass();
		JsonSchema jsonSchema = jsonSchemasForClasses.get(jsonObjectClass);
		if (jsonSchema == null) {
			synchronized (jsonSchemasForClasses) {
				if ((jsonSchema = jsonSchemasForClasses.get(jsonObjectClass)) == null) {
				    try {
				    	jsonSchemasForClasses.put(
			    			jsonObjectClass,
			    			jsonSchema = jsonSchemaGenerator.generateSchema(jsonObjectClass)
		    			);
					} catch (JsonMappingException exc) {
						return Throwables.INSTANCE.throwException(exc);
					}
				}
			}
		}
		return jsonSchema;
	}

}
