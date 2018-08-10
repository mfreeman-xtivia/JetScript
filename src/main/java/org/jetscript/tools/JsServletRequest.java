package org.jetscript.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class JsServletRequest {

	private HttpServletRequest httpRequest;
	private String body = null;

	public JsServletRequest(HttpServletRequest httpRequest) {
		this.httpRequest = httpRequest;
	}

	@SuppressWarnings("rawtypes")
	public Map getHeaders() {
		Map<String, String> headersMap = new HashMap<>();
		Enumeration enumeration = this.httpRequest.getHeaderNames();
		while (enumeration.hasMoreElements()) {
			String headerName = enumeration.nextElement().toString();
			String headerValue = this.httpRequest.getHeader(headerName);
			headersMap.put(headerName, headerValue);
		}
		return headersMap;
	}

	public String getQueryString() {
		return httpRequest.getQueryString();
	}

	public String getBody() {

		if (body != null)
			return body;

		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		try {
			InputStream inputStream = httpRequest.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				return null;
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		String _body = stringBuilder.toString().trim();
		this.body = _body.length() != 0 ? _body : "";
		return this.body;
	}

	public String getMethod() {
		return this.httpRequest.getMethod().toUpperCase();
	}
	
	public String getPathInfo() {
		return this.httpRequest.getPathInfo();
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getParams() {
		return this.httpRequest.getParameterMap();
	}

	public String get(String key) {
		return this.httpRequest.getHeader(key);
	}
	
	public HttpServletRequest getHttpRequest() {
		return this.httpRequest;
	}
	
}