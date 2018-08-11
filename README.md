##JetScript
A lightweight environment for JavaScript in Liferay DXP

JetScript is a tool for "Node-ish" Javascript-based development on Liferay DXP. **JetScript is not intended for production use** -- rather it is seen as a easy-to-use tool/environment for prototyping services and SPAs,and for writing quick administration and development tools.

### Installing

To install JetScript simply download a copy of the latest JAR from the releases page and drop into your Liferay DXP installation's deploy directly. That's it --- JetScript is now fully installed and ready for action.

After installing JetScript you will notice a new, empty "jetscript" directory has been created in the root of your DXP base directory. This directory  will serve as the "root" for all subsequent JetScript development.

### Hello World

This example is one that you can begin with immediately after installing jetscript. Simply create a new file named helloworld.js in the newly created jetscript directory that looks like the code below:

	function doGet() {
	  _rsp.status(200);
	  _rsp.contentType("application/text");
	  _rsp.send("Hello World");
	}
	
Now open your browser and type in http://localhost:8080/o/jetscript/helloworld and you should see "Hello World" text returned. Now rename the file to echo.js, and enter the URL http://localhost:8080/o/jetscript/echo and you will see the same content. (The old URL should now return a 404). This demonstrates that by design the JetScript environment loads changes to file names (and file content) that occur between requests.