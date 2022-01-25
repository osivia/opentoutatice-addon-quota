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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.opentoutatice.addon.quota.check.util.BlobSizeInfos;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer;
import org.opentoutatice.addon.quota.check.util.QuotaResolver;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author dchevrier 
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
        
        // TODO : check standard errors (404,403)
        DocumentModel document = session.getDocument(docRef);
        
        BlobSizeInfos infos = BlobsSizeComputer.get().getTreeSizeFrom(this.session, docRef);


        QuotaItems.put("number", infos.getNumber());
        
		QuotaItems.put("treesize", infos.getSize());

		long quotaValue = QuotaResolver.get().getQuotaFor(session, document, true);
		QuotaItems.put("quota", quotaValue);
 
        return createBlob(QuotaItems);
	}
	
    private Blob createBlob(JSONObject json) {
        return new StringBlob(json.toString(), "application/json");
    }

}
