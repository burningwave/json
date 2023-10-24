# Burningwave JSON

<a href="https://www.burningwave.org">
<img src="https://raw.githubusercontent.com/burningwave/burningwave.github.io/main/logo.png" alt="logo.png" height="180px" align="right"/>
</a>

**Burningwave JSON** is an advanced, free and open source JSON handler for Java.

And now we will see:
* [including Burningwave JSON in your project](#Including-Burningwave-JSON-in-your-project)
* [finding values ​​and paths in a JSON object](#Finding-values-and-paths-in-a-JSON-object)
* [validating values](#Validating-values)
* [**how to ask for assistance**](#Ask-for-assistance)

<br/>

# <a name="Including-Burningwave-JSON-in-your-project"></a>Including Burningwave JSON in your project 
To include Burningwave JSON library in your projects simply use with **Apache Maven**:

```xml
<dependency>
    <groupId>org.burningwave</groupId>
    <artifactId>json</artifactId>
    <version>0.8.0</version>
</dependency>
```

### Requiring the Burningwave JSON module

To use Burningwave JSON as a Java module you need to add the following to your `module-info.java`: 

```java
requires org.burningwave.json;
```

<br/>

# <a name="Finding-values-and-paths-in-a-JSON-object"></a>Finding values ​​and paths in a JSON object
The following example is available in the [ObjectHandlerTest class](https://github.com/burningwave/json/blob/main/src/test/java/org/burningwave/json/ObjectHandlerTest.java)).
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
First of all, to find values in the JSON we need lo load the JSON through the **ObjectHandler**. The ObjectHandler wraps the JSON and contains the path and the value of the node you are visiting within the JSON. To instantiate an ObjectHandler follow this code:

```java
Facade facade = Facade.create();
//Loading the JSON object 
Root jsonObject = facade.objectMapper().readValue(
    ObjectHandlerTest.class.getClassLoader().getResourceAsStream("quiz.json"),
    Root.class
);
ObjectHandler objectHandler = facade.newObjectHandler(jsonObject);
```

After loaded the JSON we need to instantiate a **Finder**. There are 3 kinds of Finder:
* the [**ObjectHandler.Finder**](#The-ObjectHandler.Finder) that which allows you to search for elements within the JSON ​​returning ObjectHandlers
* the **ObjectHandler.ValueFinder** that which allows you to search for elements within the JSON ​​directly returning the values
* the **ObjectHandler.ValueFinderAndConverter** that which allows you to search for elements within the JSON ​​and convert the values found

Now to load values and retrieve paths you can do the following (the full example is available in the [ObjectHandlerTest class](https://github.com/burningwave/json/blob/main/src/test/java/org/burningwave/json/ObjectHandlerTest.java)):

## The ObjectHandler.Finder
To obtain this kind of finder use this code:
```java
ObjectHandler.Finder finder = objectHandler.newFinder();
```
Once you obtained the finder you can use it to search items inside the JSON:
```java
//Searching for the first occurrence by path suffix
ObjectHandler sportOH = finder.findFirstForPathEndsWith("sport");
//Retrieving the path of the sport object ("quiz.sport")
String sportPath = sportOH.getPath();
//Retrieving the value of the sport object
sport = sportOH.getValue();
ObjectHandler option2OfSportQuestionOH = finder.findFirstForPathEndsWith(Path.of("sport", "q1", "options[1]"));
String option2OfSportQuestionOHPath = option2OfSportQuestionOH.getPath();
option2OfSportQuestion = option2OfSportQuestionOH.getValue();
ObjectHandler questionOneOH = finder.findForPathEquals(Path.of("quiz", "sport", "q1"));
String questionOnePath = questionOneOH.getPath();
questionOne = questionOneOH.getValue();
```
```java
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
```

<br />

# <a name="Validating-values"></a>Validating values
... Documentation in preparation

<br />

# <a name="Ask-for-assistance"></a>Ask for assistance
If this guide can't help you, you can:
* [open a discussion](https://github.com/burningwave/json/discussions) here on GitHub
* [report a bug](https://github.com/burningwave/json/issues) here on GitHub
* ask on [Stack Overflow](https://stackoverflow.com/search?q=burningwave)
