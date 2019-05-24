/**
 * 
 */
package org.opentoutatice.addon.quota.automation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
@Operation(id = QuotaInfo.ID)
public class QuotaInfo {

	public static final String ID = "Quota.GetInfo";

	private static final Log log = LogFactory.getLog(QuotaInfo.class);

	@Context
	protected CoreSession session;

	@OperationMethod
	public String run(DocumentRef docRef) throws ClientException {

		Long treeSize = BlobsSizeComputer.get().getTreeSizeFrom(this.session, docRef);

		return treeSize != null ? String.valueOf(treeSize) : null;
	}

}
