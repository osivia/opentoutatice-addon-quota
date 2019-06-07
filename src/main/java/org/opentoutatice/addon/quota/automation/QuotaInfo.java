/**
 * 
 */
package org.opentoutatice.addon.quota.automation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
	public Object run(DocumentRef docRef) throws ClientException {
		
        JSONObject QuotaItems = new JSONObject();

		Long treeSize = BlobsSizeComputer.get().getTreeSizeFrom(this.session, docRef);
		QuotaItems.put("treesize", treeSize);
 
        return createBlob(QuotaItems);
	}
	
    private Blob createBlob(JSONObject json) {
        return new StringBlob(json.toString(), "application/json");
    }

}
