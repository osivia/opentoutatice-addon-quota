/**
 * 
 */
package org.opentoutatice.addon.quota.listener;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.opentoutatice.addon.quota.check.QuotaChecker;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.core.helper.ToutaticeSilentProcessRunnerHelper;

/**
 * Listener used to recompute the quota if a file is added or removed
 * @author Loïc Billon
 *
 */
public class QuotaRecomputeListener implements EventListener {

	@Override
	public void handleEvent(Event event) throws ClientException {
		
		if (event.getContext() instanceof DocumentEventContext) {
			
			DocumentEventContext evtCtx = (DocumentEventContext) event.getContext();

			long size = 0;
			
			DocumentModel document = evtCtx.getSourceDocument();

			// Get file size
            if( DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(event.getName()) &&  document.hasFacet("Folderish"))    {
                // folderish : get source size
                DocumentModel srcFolder = (DocumentModel) event.getContext().getArguments()[0];  
                size = QuotaChecker.get().getTreeSizeFor(evtCtx.getCoreSession(), new PathRef(srcFolder.getPath().toString()));
           }  else {
                // BlobHolder is defined for document having file 
                BlobHolder bHolder = document.getAdapter(BlobHolder.class);
                if (bHolder != null && bHolder.getBlob() != null) {
                    size = bHolder.getBlob().getLength();
                }   
            }
            
            // Recompute quota if filesize is more than 0 octet
            if (size > 0) {
            	
                // current doc may not exist and can not be found via session
                Path parentPath = document.getPath().removeLastSegments(1); 
                DocumentModel firstDoc = evtCtx.getCoreSession().getDocument(new PathRef(parentPath.toString()));

    			// Get workspace and request a new quota chack (also detete the last date check silently).
    			DocumentModelList parentDocuments = ToutaticeDocumentHelper.getParentList(evtCtx.getCoreSession(), firstDoc,  new WorkspaceDocumentFilter(), true);
    			
    			InnerSilentModifier runner = new InnerSilentModifier(evtCtx.getCoreSession(), parentDocuments.get(0));
    	        runner.silentRun(true);
    			
            }
			
		}
	}
	
	class WorkspaceDocumentFilter implements Filter {

		@Override
		public boolean accept(DocumentModel docModel) {
			return docModel.getType().equals("Workspace");
		}
		
	}
	
	private class InnerSilentModifier extends ToutaticeSilentProcessRunnerHelper {

		private DocumentModel workspace;

		public InnerSilentModifier(CoreSession session, DocumentModel workspace) {
			super(session);
			this.workspace = workspace;
		}

		@Override
		public void run() throws ClientException {
			
			workspace.setPropertyValue("qtc:lastDateCheck", null);
			session.saveDocument(workspace);
			
		}
		
		
	}
}
