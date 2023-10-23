package org.burningwave.json.bean;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Q2 {
	String answer;
	List<String> options;
	String question;

	@JsonProperty("answer")
	public String getAnswer() {
		return this.answer;
	}

	@JsonProperty("options")
	public List<String> getOptions() {
		return this.options;
	}

	@JsonProperty("question")
	public String getQuestion() {
		return this.question;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public void setOptions(ArrayList<String> options) {
		this.options = options;
	}

	public void setQuestion(String question) {
		this.question = question;
	}
}
