package org.burningwave.json;

import org.burningwave.json.bean.Q1;
import org.burningwave.json.bean.Root;
import org.burningwave.json.bean.Sport;
import org.junit.jupiter.api.Test;

public class ObjectHandlerTest extends BaseTest {
	static final Facade facade = Facade.create();

	@Test
	void findFirstTestOne() {
		testNotNull(() -> {
			//Loading the JSON object
			Root jsonObject = facade.objectMapper().readValue(
				ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz.json"),
				Root.class
			);
			ObjectHandler objectHandler = facade.newObjectHandler(jsonObject);

			ObjectHandler.ValueFinder valueFinder = objectHandler.newValueFinder();
			Sport sport = valueFinder.findFirstForPathEndsWith("sport");
			String option2OfSportQuestion = valueFinder.findFirstForPathEndsWith(Path.of("sport", "q1", "options[1]"));
			Q1 questionOne = valueFinder.findForPathEquals(Path.of("quiz", "sport", "q1"));

			ObjectHandler.Finder objectHandlerFinder = objectHandler.newFinder();
			ObjectHandler sportOH = objectHandlerFinder.findFirstForPathEndsWith("sport");
			//Retrieving the path of the sport object ("quiz.sport")
			String sportPath = sportOH.getPath();
			//Retrieving the value of the sport object
			sport = sportOH.getValue();
			ObjectHandler option2OfSportQuestionOH = objectHandlerFinder.findFirstForPathEndsWith(Path.of("sport", "q1", "options[1]"));
			String option2OfSportQuestionOHPath = option2OfSportQuestionOH.getPath();
			option2OfSportQuestion = option2OfSportQuestionOH.getValue();
			ObjectHandler questionOneOH = objectHandlerFinder.findForPathEquals(Path.of("quiz", "sport", "q1"));
			String questionOnePath = questionOneOH.getPath();
			questionOne = questionOneOH.getValue();

			return questionOne;
		});
	}

}
