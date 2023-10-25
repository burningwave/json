package org.burningwave.json;

import java.util.Map;

import org.burningwave.json.bean.Question;
import org.burningwave.json.bean.Root;
import org.burningwave.json.bean.Sport;
import org.junit.jupiter.api.Test;

class ObjectHandlerTest extends BaseTest {
	static final Facade facade = Facade.create();

	@Test
	void findFirstTestOne() {
		testNotNull(() -> {
			//Loading the JSON object
			ObjectHandler objectHandler = facade.newObjectHandler(
				ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz.json"),
				Root.class
			);

			ObjectHandler.Finder finder = objectHandler.newFinder();
			ObjectHandler sportOH = finder.findFirstForPathEndsWith("sport");
			//Retrieving the path of the sport object ("quiz.sport")
			String sportPath = sportOH.getPath();
			//Retrieving the value of the sport object
			Sport sport = sportOH.getValue();
			ObjectHandler option2OfSportQuestionOH = finder.findFirstForPathEndsWith(Path.of("sport", "q1", "options[1]"));
			String option2OfSportQuestionOHPath = option2OfSportQuestionOH.getPath();
			String option2OfSportQuestion = option2OfSportQuestionOH.getValue();
			ObjectHandler questionOneOH = finder.findForPathEquals(Path.of("quiz", "sport", "q1"));
			String questionOnePath = questionOneOH.getPath();
			Question questionOne = questionOneOH.getValue();
			return questionOne;
		});
	}

	@Test
	void findFirstValueTestOne() {
		testNotNull(() -> {
			//Loading the JSON object
			ObjectHandler objectHandler = facade.newObjectHandler(
				ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz.json"),
				Root.class
			);

			ObjectHandler.ValueFinder finder = objectHandler.newValueFinder();
			Sport sport = finder.findFirstForPathEndsWith("sport");
			String option2OfSportQuestion = finder.findFirstForPathEndsWith(Path.of("sport", "q1", "options[1]"));
			Question questionOne = finder.findForPathEquals(Path.of("quiz", "sport", "q1"));
			return questionOne;
		});
	}

	@Test
	void findFirstValueAndConvertItTestOne() {
		testNotNull(() -> {
			//Loading the JSON object
			ObjectHandler objectHandler = facade.newObjectHandler(
				ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz.json"),
				Root.class
			);

			ObjectHandler.ValueFinderAndConverter finderAndConverter = objectHandler.newValueFinderAndConverter(Map.class);
			Map<String, Object> sportAsMap = finderAndConverter.findFirstForPathEndsWith("sport");

			return sportAsMap;
		});
	}

}
