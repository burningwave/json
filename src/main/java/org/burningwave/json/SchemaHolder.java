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
			    			jsonSchema = jsonSchemaGenerator.generateSchema(jsonObjectClass)//NOSONAR
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
