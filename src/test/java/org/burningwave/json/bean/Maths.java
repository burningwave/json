package org.burningwave.json.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Maths {
	Q1 q1;
	Q2 q2;

	@JsonProperty("q1")
	public Q1 getQ1() {
		return this.q1;
	}

	@JsonProperty("q2")
	public Q2 getQ2() {
		return this.q2;
	}

	public void setQ1(Q1 q1) {
		this.q1 = q1;
	}

	public void setQ2(Q2 q2) {
		this.q2 = q2;
	}
}
