package org.jetscript;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jetscript.tools.ToolManager;
import org.jetscript.resources.ResourceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;

@Component(immediate = true, name = "com.xtivia.tools.jetscript", service = ScriptingServlet.class)
public final class ScriptingServlet extends HttpServlet {

	public static final long serialVersionUID = -1;

	private static final String GET_METHOD      = "GET";
	private static final String POST_METHOD     = "POST";
	private static final String PUT_METHOD      = "PUT";
	private static final String DELETE_METHOD   = "DELETE";
	private static final String GET_FUNCTION    = "doGet";
	private static final String POST_FUNCTION   = "doPost";
	private static final String PUT_FUNCTION    = "doPut";
	private static final String DELETE_FUNCTION = "doDelete";
	
	private static final String SVCPATH               = "jetscript";
	private static final String REQUIRE_IMPL_RESOURCE = "/jvm-npm.js";
	private static final String APP_IMPL_SCRIPT       = "index.js";
	private static final String APP_IMPL_FUNC         = "handleRequest";
	//private static final String SKIP_APP_PARAM_NAME   = "noapp";

	private File basePath;
	private String requireCode;
	private ScriptEngine engine;
	private Map<String,Object> liferayServices = new HashMap<>();

	private static Log logger = LogFactoryUtil.getLog(ScriptingServlet.class);

	@Activate
	public void start(Map<String, Object> properties) throws ServletException {
		try (InputStream is = this.getClass().getResourceAsStream(REQUIRE_IMPL_RESOURCE)) {

			// init the Nashorn/JS scripting environment
			ScriptEngineManager manager = new ScriptEngineManager();
			this.engine = manager.getEngineByName("JavaScript");
			logger.info("Scripting engine loaded=" + engine);

			// create/seed the base directory as required (including Require support)
			initializeBaseDirectory();

			// create/attach the JetScript servlet so that the o/jetscript path is activated
			httpService.registerServlet("/" + SVCPATH, this, null, httpService.createDefaultHttpContext());
			logger.info("JetScript servlet is started at /" + SVCPATH);

			// load the require/modules implementation code into a string for later evaluation
			this.requireCode = convertStreamToString(is);
			
			// set up the liferay services we will make available to scripts
			liferayServices.put("users",this.userService);
			liferayServices.put("orgs",this.orgService);
			liferayServices.put("roles",this.roleService);
			
			// initialize the global parts of the scripting system
			engine.put("SCRIPT_BASE", basePath.getAbsolutePath());
			engine.eval(this.requireCode);

		} catch (Exception e) {
			logger.error("Error initializing JetScript servlet", e);
		}
	}

	@Deactivate
	protected void stop(Map<String, Object> properties) {
		httpService.unregister("/" + SVCPATH);
		logger.info("JetScript servlet is stopped.");
	}

	protected void initializeBaseDirectory() throws IOException {
		String liferayHome = PropsUtil.get(PropsKeys.LIFERAY_HOME);
		File liferayHomeDir = new File(liferayHome);
		basePath = new File(liferayHomeDir, SVCPATH);
		if (!basePath.exists()) {
			Files.createDirectory(basePath.toPath());
			logger.info("JetScript created new script basedir at location=" + basePath.getAbsolutePath());
		}
		logger.info("JetScript (re)set script location=" + basePath.getAbsolutePath());
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
 
		// attempt to find the requested URL as a static resource
		boolean foundFile = ResourceManager.findResource(request, response, basePath);
		if (foundFile) return;

		// if the JS env contains an application, then invoke its route() function for the request
		// unless we find a specifically requested file--
		// --in that case use mechanism that maps uri/httpmethod to scriptpath/function
		
		String path = getPath(request);
		File targetJs = new File(path);

		if (targetJs.exists()) {
			super.service(request, response);
		} else {
			File application = new File(basePath, APP_IMPL_SCRIPT);
			if (!application.exists()) {
				response.setStatus(404);
				try {
					response.getWriter().write(String.format("Could not locate the required file=%s\n", path));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return;
			}
			runScript(application.getAbsolutePath(),APP_IMPL_FUNC,request,response);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doProcess(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doProcess(request, response);
	}

	private void doProcess(HttpServletRequest request, HttpServletResponse response) 
			throws FileNotFoundException, ServletException {

		String path = getPath(request);
		String function = getFunction(request); // map to doGet, doPost etc based on http method
		runScript(path, function, request, response);
	}
	
	private void runScript(String path, String function, HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			
		   // create a new scripting context/binding for exclusive use in this thread/request
			ScriptContext threadContext = new SimpleScriptContext();
			Bindings threadBindings = engine.createBindings();
			threadBindings.putAll(engine.getBindings(ScriptContext.ENGINE_SCOPE));  //copy default bindings
			
			DxpServiceHolder liferayDataHolder = new DxpServiceHolder(req);
			liferayDataHolder.setServices(this.liferayServices);
			
			threadBindings.put("dxp",liferayDataHolder);	
			ToolManager.initializeTools(basePath,threadBindings,req,resp);
			threadContext.setBindings(threadBindings, ScriptContext.ENGINE_SCOPE);

			// load/parse/evaluate the targeted script into thread-specific context
			engine.eval("load('" + path + "');", threadContext);
			Invocable inv = (Invocable) engine;

			// execute the targeted function
			synchronized (this) {
				Bindings savedBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
				try {
					engine.setBindings(threadBindings, ScriptContext.ENGINE_SCOPE);
					inv.invokeFunction(function);
				} finally {
					engine.setBindings(savedBindings, ScriptContext.ENGINE_SCOPE);
				}
			}

		} catch (Exception ex) {
			logger.error(ex);
			resp.setStatus(404);
			try {
			  resp.getWriter().write(ex.getMessage());
			} catch (Exception e) {e.printStackTrace();}
			return;
		}		
	}
	
	// turn the HTTP path info into a fully qualified file system path

	private String getPath(final HttpServletRequest request) {

		String servletPath = request.getPathInfo();
		if (servletPath == null) servletPath = "index";
		String realPath = new File(basePath, servletPath + ".js").getAbsolutePath();
		return realPath;
	}
	
	// map the HTTP method into an expected JS function name

	private String getFunction(final HttpServletRequest request) throws ServletException {

		String method = request.getMethod();
		switch (method) {
		case GET_METHOD:
			return GET_FUNCTION;
		case POST_METHOD:
			return POST_FUNCTION;
		case PUT_METHOD:
			return PUT_FUNCTION;
		case DELETE_METHOD:
			return DELETE_FUNCTION;
		default:
			throw new ServletException(String.format("Unsupported method '%s'", method));
		}
	}
	
	// utility function to turn an input stream into a string

	@SuppressWarnings("resource")
	static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

   // OSGi Http Service reference

	@Reference
	protected void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}
	protected HttpService httpService;
	
	// Liferay DXP service references
	
	@Reference
	protected void setUserLocalService(UserLocalService userService) {
		this.userService = userService;
	}
	protected UserLocalService userService;

	@Reference
	protected void setOrganizationLocalService(OrganizationLocalService orgService) {
		this.orgService = orgService;
	}
	protected OrganizationLocalService orgService;
	
	@Reference
	protected void setRoleLocalService(RoleLocalService roleService) {
		this.roleService = roleService;
	}
	protected RoleLocalService roleService;
	
	// dummy Servlet methods
	
	@Override
	public void init(ServletConfig config) throws ServletException {}

	@Override
	public ServletConfig getServletConfig() {return null;}

	@Override
	public String getServletInfo() {return null;}
	
	@Override
	public void destroy() {}
}
