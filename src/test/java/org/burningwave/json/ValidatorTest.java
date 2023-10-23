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
				//Check
				Check.forAll().checkMandatory(),
				Check.forAllStringValues().execute(validationContext -> {

				})
			);

			//Loading the JSON object
			Root jsonObject = facade.objectMapper().readValue(
				ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz-to-be-validated.json"),
				Root.class
			);
			ObjectHandler objectHandler = facade.newObjectHandler(jsonObject);
			Collection<Throwable> exceptions = facade.validator().validate(Validation.Config.forJsonObject(jsonObject).withCompleteValidation());
			for (Throwable exc : exceptions) {
				System.err.println(exc.getMessage());
			};
			throw exceptions.iterator().next();
		});

	}

}
