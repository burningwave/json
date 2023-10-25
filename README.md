# Burningwave JSON [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=%40burningwave_org%20JSON%2C%20a%20set%20of%20components%20for%20handling%20%23JSON%20documents%20%28works%20on%20%23Java8%20%23Java9%20%23Java10%20%23Java11%20%23Java12%20%23Java13%20%23Java14%20%23Java15%20%23Java16%20%23Java17%20%23Java18%20%23Java19%20%23Java20%20%23Java21%29&url=https://burningwave.github.io/json/)

<a href="https://www.burningwave.org">
<img src="https://raw.githubusercontent.com/burningwave/burningwave.github.io/main/logo.png" alt="logo.png" height="180px" align="right"/>
</a>

[![Maven Central with version prefix filter](https://img.shields.io/maven-central/v/org.burningwave/json/0)](https://maven-badges.herokuapp.com/maven-central/org.burningwave/json/)
[![GitHub](https://img.shields.io/github/license/burningwave/json)](https://github.com/burningwave/json/blob/master/LICENSE)

[![Platforms](https://img.shields.io/badge/platforms-Windows%2C%20Mac%20OS%2C%20Linux-orange)](https://github.com/burningwave/json/actions/runs/6636676680)

[![Supported JVM](https://img.shields.io/badge/supported%20JVM-8%2C%209+-blueviolet)](https://github.com/burningwave/json/actions/runs/6636676680)

[![Coveralls github branch](https://img.shields.io/coveralls/github/burningwave/json/master)](https://coveralls.io/github/burningwave/json?branch=master)
[![GitHub open issues](https://img.shields.io/github/issues/burningwave/json)](https://github.com/burningwave/json/issues)
[![GitHub closed issues](https://img.shields.io/github/issues-closed/burningwave/json)](https://github.com/burningwave/json/issues?q=is%3Aissue+is%3Aclosed)

[![Artifact downloads](https://www.burningwave.org/generators/generate-burningwave-artifact-downloads-badge.php?artifactId=json)](https://www.burningwave.org/artifact-downloads/?show-overall-trend-chart=false&artifactId=json&startDate=2023-10)
[![Repository dependents](https://badgen.net/github/dependents-repo/burningwave/json)](https://github.com/burningwave/json/network/dependents)
[![HitCount](https://www.burningwave.org/generators/generate-visited-pages-badge.php)](https://www.burningwave.org#bw-counters)

**Burningwave JSON** is an advanced, free and open source JSON handler for Java.
**The search and validation possibilities offered by this library are practically infinite** and this page will illustrate an overview of the components exposed by the library and some examples of basic operation: for any further help visit [the relevant section](#Ask-for-assistance).

And now we will see:
* [including Burningwave JSON in your project](#Including-Burningwave-JSON-in-your-project)
* [finding values ​​and paths in a JSON document](#Finding-values-and-paths-in-a-JSON-document)
* [validating values of a JSON document](#Validating-values-of-a-JSON-document)
* [**how to ask for assistance**](#Ask-for-assistance)

<br/>

# <a name="Including-Burningwave-JSON-in-your-project"></a>Including Burningwave JSON in your project 
To include Burningwave JSON library in your projects simply use with **Apache Maven**:

```xml
<dependency>
    <groupId>org.burningwave</groupId>
    <artifactId>json</artifactId>
    <version>0.12.0</version>
</dependency>
```

### Requiring the Burningwave JSON module

To use Burningwave JSON as a Java module you need to add the following to your `module-info.java`: 

```java
requires org.burningwave.json;
```

### Enabling the JVM Driver
Burningwave JSON uses the [Burningwave Reflection](https://burningwave.github.io/reflection/) library that, by default, doesn't use the the [Burningwave JVM Driver](https://burningwave.github.io/jvm-driver/): if you want to enable it [consult the relevant section on the Burningwave Reflection project](https://burningwave.github.io/reflection#enabling-the-jvm-driver).

<br/>

# <a name="Finding-values-and-paths-in-a-JSON-document"></a>Finding values ​​and paths in a JSON document
The following example is available in the [ObjectHandlerTest class](https://github.com/burningwave/json/blob/main/src/test/java/org/burningwave/json/ObjectHandlerTest.java).
Let's assume the following JSON document:

```json
{
    "quiz": {
        "sport": {
            "q1": {
                "question": "Which one is correct team name in NBA?",
                "options": [
                    "New York Bulls",
                    "Los Angeles Kings",
                    "Golden State Warriros",
                    "Huston Rocket"
                ],
                "answer": "Huston Rocket"
            }
        },
        "maths": {
            "q1": {
                "question": "5 + 7 = ?",
                "options": [
                    "10",
                    "11",
                    "12",
                    "13"
                ],
                "answer": "12"
            },
            "q2": {
                "question": "12 - 8 = ?",
                "options": [
                    "1",
                    "2",
                    "3",
                    "4"
                ],
                "answer": "4"
            }
        }
    }
}
```
First of all, to find values in a JSON document we need to load it via **ObjectHandler**. The ObjectHandler wraps the JSON document and contains the path and the value of the node you are visiting within the JSON. To instantiate an ObjectHandler follow this code:

```java
Facade facade = Facade.create();
//Loading the JSON object 
ObjectHandler objectHandler = facade.newObjectHandler(
	ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz.json"),
	Root.class
);
```

After loaded the JSON we need to instantiate a **Finder**. There are 3 kinds of Finder:
* the [**ObjectHandler.Finder**](#The-ObjectHandlerFinder) that which allows you to search for elements within the JSON ​​returning ObjectHandlers
* the [**ObjectHandler.ValueFinder**](#The-ObjectHandlerValueFinder) that which allows you to search for elements within the JSON ​​directly returning the values
* the [**ObjectHandler.ValueFinderAndConverter**](#The-ObjectHandlerValueFinderAndConverter) that which allows you to search for elements within the JSON ​​and convert the values found

<br/>

## The ObjectHandler.Finder
To obtain this kind of finder use this code:
```java
ObjectHandler.Finder finder = objectHandler.newFinder();
```
Once you obtained the finder you can use it to search items inside the JSON document:
```java
//Searching for the first occurrence by path suffix
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
```

<br/>

## The ObjectHandler.ValueFinder
To obtain this kind of finder use this code:
```java
ObjectHandler.Finder finder = objectHandler.newValueFinder();
```
Once you obtained the finder you can use it to search items inside the JSON document:

```java
ObjectHandler.ValueFinder finder = objectHandler.newValueFinder();
//Searching for the first occurrence by path suffix
Sport sport = finder.findFirstForPathEndsWith("sport");
String option2OfSportQuestion = finder.findFirstForPathEndsWith(Path.of("sport", "q1", "options[1]"));
Question questionOne = finder.findForPathEquals(Path.of("quiz", "sport", "q1"));
```

<br/>

## The ObjectHandler.ValueFinderAndConverter
To obtain this kind of finder use this code:
```java
ObjectHandler.Finder finderAndConverter = objectHandler.newValueFinderAndConverter();
```
Once you obtained the finder you can use it to search items inside the JSON document and **convert them**:
```java
//Searching for the first occurrence by path suffix and convert it
Map<String, Object> sportAsMap = finderAndConverter.findFirstForPathEndsWith("sport");
```
<br />

# <a name="Validating-values-of-a-JSON-document"></a>Validating values of a JSON document
The following example is available in the [ValidatorTest class](https://github.com/burningwave/json/blob/main/src/test/java/org/burningwave/json/ValidatorTest.java).
To validate a JSON document we need to obtain the **Validator** and then register the checks:
```java
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
```
Once registered the checks, to execute the validation we must call the `validate` method:
```java
//Loading the JSON object
ObjectHandler objectHandler = facade.newObjectHandler(
	ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz.json"),
	Root.class
);
Collection<Throwable> exceptions =
	facade.validator().validate(
		Validation.Config.forJsonObject(objectHandler.getValue())
		//By calling this method the validation will be performed on the entire document,
		//otherwise the validation will stop at the first exception thrown
		.withCompleteValidation()
	);
```

<br />

# <a name="Ask-for-assistance"></a>Ask for assistance
If this guide can't help you, you can:
* [open a discussion](https://github.com/burningwave/json/discussions) here on GitHub
* [report a bug](https://github.com/burningwave/json/issues) here on GitHub
* ask on [Stack Overflow](https://stackoverflow.com/search?q=burningwave)
