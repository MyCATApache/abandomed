package io.mycat.net2;

public class ConnectionException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int code;
	private final String msg;

	public ConnectionException(int code, String msg) {
		super();
		this.code = code;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		return "ConnectionException [code=" + code + ", msg=" + msg + "]";
	}

}