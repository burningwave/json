package org.burningwave.json;

public class TerminateIterationException extends RuntimeException {
	private static final long serialVersionUID = 5521807052810893461L;
	public static final TerminateIterationException INSTANCE;

	static {
		INSTANCE = new TerminateIterationException();
	}

	private TerminateIterationException() {}

	public <T> T throwIt() {
		throw INSTANCE;
	}

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}