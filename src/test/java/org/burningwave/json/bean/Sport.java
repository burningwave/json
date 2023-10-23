package org.burningwave.json.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Sport {
	Q1 q1;

	@JsonProperty("q1")
	public Q1 getQ1() {
		return this.q1;
	}

	public void setQ1(Q1 q1) {
		this.q1 = q1;
	}
}
