/**
 * 
 */
package org.opentoutatice.addon.quota.check.exception;

import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * @author dchevrier 
 *
 */
public class QuotaExceededException extends RecoverableClientException {

	private static final long serialVersionUID = 4947353778360167920L;

	public QuotaExceededException(String message, String localizedMessage, String[] params) {
		super(message, localizedMessage, params);
	}
	
	public QuotaExceededException(String message, String localizedMessage, String[] params, Throwable t) {
		super(message, localizedMessage, params, t);
	}

}
