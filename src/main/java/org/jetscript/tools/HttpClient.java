package org.jetscript.tools;

import com.github.kevinsawicki.http.HttpRequest;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpClient {

    private Map<String, String> headers = new HashMap<String,String>();
    private HttpRequest request;
    private String baseUrl;
    private int status = 0;

    private HttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void get(String uri) throws ScriptException {
        try {
            request = HttpRequest.get(String.format("%s%s", baseUrl, uri));
            if (this.headers.size() > 0) request.headers(headers);
            request.followRedirects(false);
            status = request.code();
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public void post(String uri, String data) throws ScriptException {
        try {
            request = HttpRequest.post(String.format("%s%s",baseUrl,uri));
            if (this.headers.size() > 0 ) request.headers(headers);
            request.followRedirects(false);
            request.send(data);
            status = request.code();
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public void put(String uri, String data) throws ScriptException {
        try {
            request = HttpRequest.put(String.format("%s%s", baseUrl, uri));
            if (this.headers.size() > 0) request.headers(headers);
            request.followRedirects(false);
            request.send(data);
            status = request.code();
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public void del(String uri) throws ScriptException {
        try {
            request = HttpRequest.delete(String.format("%s%s", baseUrl, uri));
            if (this.headers.size() > 0) request.headers(headers);
            request.followRedirects(false);
            status = request.code();
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public String getBody() throws ScriptException {
        try {
            if (request == null) return "";
            return request.body();
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public void addRequestHeader(String key, String value) {
        headers.put(key,value);
    }

    public void clearRequestHeaders() {
        headers.clear();
    }

    public Map<String,String> getResponseHeaders() throws ScriptException {
        try {
            if (request == null) return new HashMap<>();
            Map<String, String> results = new HashMap<>();
            Map<String, List<String>> _headers = request.headers();
            _headers.forEach((key, value) -> {
                results.put(key, value.get(0));
            });
            return results;
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }

    public static class Factory {
        // a little bit of wonkiness needed right now because of class loader
        // issues in OSGi and Nashorn land
        public HttpClient connect(String baseUrl) {
            return new HttpClient(baseUrl);
        }
    }

}