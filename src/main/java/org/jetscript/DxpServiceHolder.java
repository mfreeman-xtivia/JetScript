package org.jetscript;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.PortalUtil;

public class DxpServiceHolder {
	
	private User user;
	private Map<String,Object> services = new HashMap<>();
		
	public DxpServiceHolder(HttpServletRequest request) {
		try {
		    this.user = PortalUtil.getUser(request);
		} catch (NullPointerException npe) {
		    /* Right now the Equinox HttpRequest wrapper class, for some bizarre
			   reason throws an NPE on this call if the user is not logged in....
			   ....Maybe related to https://bugs.eclipse.org/bugs/show_bug.cgi?id=479115??
			   ...So for now catch the NPE and return null
			 */
			this.user = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setServices(Map<String, Object> services) {
		this.services.putAll(services);
	}
	
	public User getUser() {
		return user;
	}

	public Map<String,Object> getServices() {
		return services;
	}

}
