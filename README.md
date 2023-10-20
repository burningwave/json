# Burningwave JSON

<a href="https://www.burningwave.org">
<img src="https://raw.githubusercontent.com/burningwave/burningwave.github.io/main/logo.png" alt="logo.png" height="180px" align="right"/>
</a>

**Burningwave JSON** is an advanced, free and open source Java JSON handler.

And now we will see:
* [including Burningwave JSON in your project](#Including-Burningwave-JSON-in-your-project)
* [**how to ask for assistance**](#Ask-for-assistance)

<br/>

# <a name="Including-Burningwave-JSON-in-your-project"></a>Including Burningwave JSON in your project 
To include Burningwave JSON library in your projects simply use with **Apache Maven**:

```xml
<dependency>
    <groupId>org.burningwave</groupId>
    <artifactId>json</artifactId>
    <version>0.6.3</version>
</dependency>
```

### Requiring the Burningwave JSON module

To use Burningwave JSON as a Java module you need to add the following to your `module-info.java`: 

```java
requires org.burningwave.json;
```

### find values ​​in JSON
For this purpose is necessary the use of  **ObjectHandler**. Let's assume the following JSON document:

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

<br />

# <a name="Ask-for-assistance"></a>Ask for assistance
If this guide can't help you, you can:
* [open a discussion](https://github.com/burningwave/json/discussions) here on GitHub
* [report a bug](https://github.com/burningwave/json/issues) here on GitHub
* ask on [Stack Overflow](https://stackoverflow.com/search?q=burningwave)
