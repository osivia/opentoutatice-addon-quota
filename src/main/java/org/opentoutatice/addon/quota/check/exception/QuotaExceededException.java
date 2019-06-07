/**
 * 
 */
package org.opentoutatice.addon.quota.check.exception;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class QuotaExceededException extends RecoverableClientException {

	private static final long serialVersionUID = 4947353778360167920L;

	public QuotaExceededException(Long quota_) {
		super("Quota exceeded", String.format("Quota exceeded (%d bytes)", quota_), new String[] {});
	}
	
}
