package org.jetscript.tools;

import java.io.File;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ToolManager {
	
	public static void initializeTools(File basePath,Bindings bindings, HttpServletRequest req, HttpServletResponse resp) {
		bindings.put("console",new Console());
		bindings.put("http", new HttpClient.Factory());
		bindings.put("db", new DbClient.Factory());
		bindings.put("_req",new JsServletRequest(req));
		bindings.put("_rsp",new JsServletResponse(resp));
	}

}
