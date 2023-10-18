package org.burningwave.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

import org.burningwave.json.Path.ValidationContext;

public class ValidationConfig<I> {
	private static final Predicate<Path.ValidationContext<?, ?>> DEFAULT_PATH_FILTER;
	private static final Function<Check<?, ?, ?>, Predicate<Path.ValidationContext<?, ?>>> DEFAULT_CHECK_FILTER;
	
	static {
		DEFAULT_PATH_FILTER =
			pathValidationContext ->
				true;
		DEFAULT_CHECK_FILTER = check -> 
			pathValidationContext ->
				true;
	}
	
	I jsonObjectOrSupplier;
	Predicate<ValidationContext<?, ?>> pathFilter;
	Function<Check<?, ?, ?>, Predicate<ValidationContext<?, ?>>> checkFilter;
	boolean validateAll;
	int logMode;
	Collection<String> checkGroupIds;
	
	private ValidationConfig(I jsonObject) {
		this.jsonObjectOrSupplier = jsonObject;
		this.pathFilter = DEFAULT_PATH_FILTER;
		this.checkFilter = DEFAULT_CHECK_FILTER;
		this.logMode = 1;
		this.checkGroupIds = new ArrayList<>();
	}
	
	public static <I> ValidationConfig<I> forJsonObject(I jsonObject) {
		return new ValidationConfig<>(jsonObject);
	}

	public ValidationConfig<I> withPathFilter(Predicate<ValidationContext<?, ?>> pathFilter) {
		this.pathFilter = pathFilter;
		return this;
	}
	
	public ValidationConfig<I> withCheckFilter(Function<Check<?, ?, ?>, Predicate<ValidationContext<?, ?>>> checkFilter) {
		this.checkFilter = checkFilter;
		return this;
	}	

	public ValidationConfig<I> withCompleteValidation() {
		this.validateAll = true;
		return this;
	}
	
	public ValidationConfig<I> withExitStrategyAtFirstError() {
		this.validateAll = false;
		return this;
	}
	
	public ValidationConfig<I> withTheseChecks(String... checkGroupIds) {
		this.checkGroupIds.addAll(Arrays.asList(checkGroupIds));
		return this;
	}
	
	public ValidationConfig<I> disableLogging() {
		this.logMode = 0;
		return this;
	}
	
	public ValidationConfig<I> enableDeepLogging() {
		this.logMode = 2;
		return this;
	}
	
	boolean isDeepLoggingEnabled() {
		return logMode == 2;
	}
	
	boolean isErrorLoggingEnabled() {
		return isDeepLoggingEnabled() || logMode == 1;
	}

	Collection<String> getGroupIds() {
		return checkGroupIds;
	}
}