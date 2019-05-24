/**
 * 
 */
package org.opentoutatice.addon.quota.check.exception;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class QuotaExceededException extends ClientException {

	private static final long serialVersionUID = 4947353778360167920L;

	public QuotaExceededException(Long quota_) {
		super(String.format("Quota exceeded (%d bytes)", quota_));
	}
	
}
