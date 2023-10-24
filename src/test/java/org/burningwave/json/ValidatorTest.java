package org.burningwave.json;

import java.util.Collection;

import org.burningwave.json.bean.Root;
import org.junit.jupiter.api.Test;

class ValidatorTest extends BaseTest {

	static final Facade facade = Facade.create();

	@Test
	void validateTestOne() {
		testThrow(() -> {
			facade.validator().registerCheck(
				//Checking whether a value in any field marked as required (e.g.: @JsonProperty(value = "answer", required = true)) is null
				Check.forAll().checkMandatory(),
				//Checking whether a string value in any field is empty
				Check.forAllStringValues().execute(pathValidationContext -> {
					if (pathValidationContext.getValue() != null && pathValidationContext.getValue().trim().equals("")) {
						pathValidationContext.rejectValue("IS_EMPTY", "is empty");
					}
				})
			);

			//Loading the JSON object
			Root jsonObject = facade.objectMapper().readValue(
				ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz-to-be-validated.json"),
				Root.class
			);
			Collection<Throwable> exceptions = facade.validator().validate(
				Validation.Config.forJsonObject(jsonObject)
				//By calling this method the validation will be performed on the entire document,
				//otherwise the validation will stop at the first exception thrown
				.withCompleteValidation()
			);
			for (Throwable exc : exceptions) {
				System.err.println(exc.getMessage());
			};
			throw exceptions.iterator().next();
		});

	}

}
