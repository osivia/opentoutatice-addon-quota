/**
 * 
 */
package org.opentoutatice.addon.quota.test;

import java.io.File;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.opentoutatice.addon.quota.automation.QuotaInfo;
import org.opentoutatice.addon.quota.check.exception.QuotaExceededException;

import com.google.inject.Inject;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.test", "org.nuxeo.elasticsearch.core.test",
		"org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.platform.mimetype.core",
		"org.nuxeo.ecm.platform.types.core" })
@LocalDeploy({ "org.opentoutatice.addon.quota",
		"org.opentoutatice.addon.quota:elasticsearch-config-test.xml",
		"org.opentoutatice.addon.quota:usermanger-test.xml", "org.opentoutatice.addon.quota:log4j.xml" })
@Jetty(port = 18080)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class QuotaTest {

	protected static final Log log = LogFactory.getLog(QuotaTest.class);

	static final String[][] users = { { "VirtualAdministrator", "secret" }, { "Administrator", "Administrator" } };
	static final int nb_docs_ = 10;
	// FIXME: replace part with current ws path
	static final String blobs_folder_ = "/home/david/work/projects/osv/ws/opentoutatice-addons/opentoutatice-addon-quota/src/test/resources/blobs/";
	static final String[] blobs_ = { blobs_folder_.concat("01.pdf"), blobs_folder_.concat("02.odt") };
	
	private long importedBlobsLength = 0;
	private DocumentModel quota;
	private DocumentModel testContainer;
	
	@Inject
	protected ElasticSearchAdmin esService;

	@Inject
	protected FileManager fileManager;

	@Inject
	protected CoreSession session;

	@Inject
	protected Session automationSession;

	/**
	 * Create docs.
	 * 
	 * @throws InterruptedException
	 */
	@Before
	public void prepareRepository() throws Exception {
		log.debug("Starting populate repo...");
		// FIXME: silent mode!!

		Transaction inTx = TransactionHelper.requireNewTransaction();
		ElasticSearchInlineListener.useSyncIndexing.set(true);
		try {

			// Populate repo & index
			DocumentModel modelToCreate_0 = this.session.createDocumentModel("/default-domain/workspaces",
					"ws_container_0", "Workspace");
			DocumentModel container_0 = this.session.createDocument(modelToCreate_0);
			this.quota = container_0;

			for (String b_ : blobs_) {
				Blob blob = new FileBlob(new File(b_));
				this.importedBlobsLength += blob.getLength();
				
				this.fileManager.createDocumentFromBlob(this.session, blob, container_0.getPathAsString(), true, b_);
			}
			
			// Second level
			DocumentModel modelToCreate_1 = this.session.createDocumentModel("/default-domain/workspaces/ws_container_0",
					"ws_container_1", "Workspace");
			DocumentModel container_1 = this.session.createDocument(modelToCreate_1);
			this.testContainer = container_1;

			for (String b_ : blobs_) {
				Blob blob = new FileBlob(new File(b_));
				this.importedBlobsLength += blob.getLength();
				
				this.fileManager.createDocumentFromBlob(this.session, blob, container_1.getPathAsString(), true, b_);
			}
			
			log.debug(String.format("Imported: [%d] bytes.", this.importedBlobsLength));
			
			this.session.save();

			TransactionHelper.commitOrRollbackTransaction();
			TransactionHelper.resumeTransaction(inTx);

			// Waiting Es indexation
			Thread.sleep(500); // Commit time
			while(this.esService.isIndexingInProgress()) {
				Thread.sleep(500);
			}
			this.esService.refreshRepositoryIndex(this.session.getRepositoryName());
			
		} finally {
			ElasticSearchInlineListener.useSyncIndexing.set(false);
		}

		log.debug("Repo populated.");
	}

	@Test
	public void testTreeSize() throws Exception {
		// FIXME: return type changed: String -> FileBlob
		String size = (String) this.automationSession.getClient().getSession(users[0][0], users[0][1])
				.newRequest(QuotaInfo.ID).setInput("/default-domain").execute();

//		FileBlob res = (FileBlob) this.automationSession.getClient().getSession(users[0][0], users[0][1])
//				.newRequest(QuotaInfo.ID).setInput("/default-domain").execute();
		
		Assert.assertNotNull(size);
		Long long_ = null;
		try {
			long_ = Long.valueOf(size);
		} catch (Exception e) {
			log.debug(e);
		}
		Assert.assertNotNull(long_);
		Assert.assertEquals(this.importedBlobsLength, (long) long_);

		log.debug(String.format("Tree size: [%d] bytes.", long_));
	}
	
	@Test
	public void testQuotaNotExceeded() throws InterruptedException {
		Assert.assertTrue(this.quota.hasFacet("Quota"));
		
		this.quota.setPropertyValue("qt:maxSize", 700000);
		this.session.saveDocument(this.quota);
		
		boolean exc = false;
		Blob smallB = new FileBlob(new File(blobs_folder_.concat("small.txt")));
		try {
			this.fileManager.createDocumentFromBlob(this.session, smallB, this.testContainer.getPathAsString(), true, "small.txt");
		} catch (Exception e) {
			exc = true;
		}
		Assert.assertTrue(!exc);
		
		DocumentModel createdDoc = this.session.getDocument(new PathRef(this.testContainer.getPathAsString().concat("/").concat("small.txt")));
		Assert.assertNotNull(createdDoc);
	}
	
	@Test
	public void testQuotaExceeded() {
		Assert.assertTrue(this.quota.hasFacet("Quota"));
		
		this.quota.setPropertyValue("qt:maxSize", 700000);
		this.session.saveDocument(this.quota);
		
		boolean exc = false;
		Blob smallB = new FileBlob(new File(blobs_folder_.concat("big.pdf")));
		try {
			this.fileManager.createDocumentFromBlob(this.session, smallB, this.testContainer.getPathAsString(), true, "big.pdf");
		} catch (Exception e) {
			exc = true;
			Assert.assertTrue(e instanceof QuotaExceededException);
		}
		Assert.assertTrue(exc);
		
		DocumentModel createdDoc = null;
		try {
			createdDoc = this.session.getDocument(new PathRef(this.testContainer.getPathAsString().concat("/").concat("big.pdf")));
		} catch (Exception e) {
			Assert.assertTrue(e.getCause() instanceof NoSuchDocumentException);
		}
		
		Assert.assertNull(createdDoc);
	}

}
