/**
 * 
 */
package org.opentoutatice.addon.quota.ui.portal.view.seam;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage.Severity;
import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * Manages global Exceptions, i.e. not associated with a given JSF.
 * E.g., case of bubbled Exception thrown by a listener.
 * 
 * @author dchevrier
 *
 */
@Name("pvGlobalExceptions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.DEPLOYMENT)
public class GlobalExceptionManager implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/** Localized message.*/
	private String localizedMessage = null;
	
	/**
	 * @return true if RecoverableClientException exists in request.
	 */
	public boolean toHandle() {
		// Result
		boolean toHandle = false;
		
		// Get current global Exceptions 
		FacesContext facesCtx = FacesContext.getCurrentInstance();
		Iterator<FacesMessage> messages = facesCtx.getMessages(null);
		
		if(messages != null && messages.hasNext()) {
			StringBuffer localizedMsgBuff = new StringBuffer();
			
			while (messages.hasNext()) {
				FacesMessage message = messages.next();
				
				// Keep only error level 
				if(FacesMessage.SEVERITY_ERROR.equals(message.getSeverity())) {
				
					String summary = message.getSummary();
					localizedMsgBuff.append(summary);
					
					if(messages.hasNext()) {
						localizedMsgBuff.append("<br/>");
					}
				}
			}
			
			this.localizedMessage = localizedMsgBuff.toString();
			
			// Result
			toHandle = true;
		}
		
		return toHandle;
	}
	
	public String getLocalizedMessage() {
		return localizedMessage;
	}

	public void setLocalizedMessage(String localizedMessage) {
		this.localizedMessage = localizedMessage;
	}
	
	

}
