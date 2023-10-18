package org.burningwave.json;

public class ArgumentNotValidException extends RuntimeException {

	private static final long serialVersionUID = 3374330455682651577L;
	
	protected final String key;

	public ArgumentNotValidException(String key, String message) {
		super(message);
		this.key = key;
	}

	public String getKey() {
		return key;
	}

}