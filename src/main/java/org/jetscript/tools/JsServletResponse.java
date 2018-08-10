package org.jetscript.tools;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

public class JsServletResponse {

	private HttpServletResponse httpResponse;

	public JsServletResponse(HttpServletResponse httpResponse) {
		this.httpResponse = httpResponse;
	}

	public void send(String contents) {
		try {
			PrintWriter pw = httpResponse.getWriter();
			pw.write(contents);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public JsServletResponse status(int status) {
		httpResponse.setStatus(status);
		return this;
	}

	public JsServletResponse set(String headerName, String headerValue) {
		httpResponse.setHeader(headerName, headerValue);
		return this;
	}
	
	public void setStatus(int status) {
		httpResponse.setStatus(status);
	}
	
	public void setContentType(String ctype) {
		httpResponse.setContentType(ctype);
	}

}