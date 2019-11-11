package org.opentoutatice.addon.quota.service;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer;
import org.opentoutatice.addon.quota.check.util.QuotaResolver;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.service.quota.QuotaService;

public class QuotaServiceImpl implements QuotaService {

	@Override
	public Long getFreeSpace(CoreSession session, DocumentModel doc) {
		
		
		long quota = QuotaResolver.get().getQuotaFor(session, doc, false);
		
		if(quota > 1) {
			
			Filter workspaceFilter = new Filter() {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean accept(DocumentModel doc) {
					return doc.getType().equals("Workspace");
				}
			};
			
			DocumentModelList workspaces = ToutaticeDocumentHelper.getParentList(session, doc, workspaceFilter, true,
					true, true);
			DocumentModel workspace = workspaces.get(0);
			
			Long spaceOccupied = BlobsSizeComputer.get().getTreeSizeFrom(session, new PathRef(workspace.getPathAsString()));
			
			long freeSpace = quota - spaceOccupied;
			return (freeSpace > 0) ? freeSpace : 0; // if no space left, return 0
		}
		else return null;
	}
	
	
}
