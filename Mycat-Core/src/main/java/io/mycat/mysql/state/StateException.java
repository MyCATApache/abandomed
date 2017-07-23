package io.mycat.mysql.state;

public class StateException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5142560187842259909L;

	public StateException() {
    }

    public StateException(String message) {
        super(message);
    }

    public StateException(String message, Throwable cause) {
        super(message, cause);
    }

    public StateException(Throwable cause) {
        super(cause);
    }

    public StateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
