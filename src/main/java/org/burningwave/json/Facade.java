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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.burningwave.Executor;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Facade {

	private ObjectMapper objectMapper;
	private Validator validator;
	private SchemaHolder schemaHolder;

	private Facade() {
		this(new ObjectMapper());
	}

	private Facade(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.schemaHolder = new SchemaHolder(objectMapper);
		this.validator = new Validator(
			this.schemaHolder
		);
	}

	public static Facade create() {
		return new Facade();
	}

	public static Facade create(ObjectMapper objectMapper) {
		return new Facade(objectMapper);
	}

	public ObjectMapper objectMapper() {
		return objectMapper;
	}

	public ObjectHandler newObjectHandler(Object jsonObject) {
		return ObjectHandler.create(objectMapper, jsonObject);
	}

	public <T> ObjectHandler newObjectHandler(byte[] src, Class<T> type) {
		return Executor.get(() -> ObjectHandler.create(objectMapper, objectMapper.readValue(src, type)));
	}

	public <T> ObjectHandler newObjectHandler(File src, Class<T> type) {
		return Executor.get(() -> ObjectHandler.create(objectMapper, objectMapper.readValue(src, type)));
	}

	public <T> ObjectHandler newObjectHandler(InputStream src, Class<T> type) {
		return Executor.get(() -> ObjectHandler.create(objectMapper, objectMapper.readValue(src, type)));
	}

	public <T> ObjectHandler newObjectHandler(Reader src, Class<T> type) {
		return Executor.get(() -> ObjectHandler.create(objectMapper, objectMapper.readValue(src, type)));
	}

	public <T> ObjectHandler newObjectHandler(String src, Class<T> type) {
		return Executor.get(() -> ObjectHandler.create(objectMapper, objectMapper.readValue(src, type)));
	}

	public <T> ObjectHandler newObjectHandler(URL src, Class<T> type) {
		return Executor.get(() -> ObjectHandler.create(objectMapper, objectMapper.readValue(src, type)));
	}

	public Validator validator() {
		return this.validator;
	}

}
