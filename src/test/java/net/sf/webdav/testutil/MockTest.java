package net.sf.webdav.testutil;

import java.io.ByteArrayInputStream;
import java.util.Date;

import net.sf.webdav.StoredObject;
import net.sf.webdav.locking.LockedObject;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.methods.TestingOutputStream;

import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.DelegatingServletInputStream;

public abstract class MockTest {

    protected Mockery _mockery;

    protected static boolean readOnly = true;

    protected static int TEMP_TIMEOUT = 10;
    protected static boolean TEMPORARY = true;

    protected TestingOutputStream tos = new TestingOutputStream();
    protected static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l',
            'l', 'o', '/', '>' };
    protected static ByteArrayInputStream bais = new ByteArrayInputStream(
            resourceContent);
    protected static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);
    protected static long resourceLength = resourceContent.length;

    protected static String exclusiveLockRequest = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
            + "<D:lockinfo xmlns:D='DAV:'>"
            + "<D:lockscope><D:exclusive/></D:lockscope>"
            + "<D:locktype><D:write/></D:locktype>"
            + "<D:owner><D:href>I'am the Lock Owner</D:href></D:owner>"
            + "</D:lockinfo>";
    protected static byte[] exclusiveLockRequestByteArray = exclusiveLockRequest
            .getBytes();
    protected ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
            exclusiveLockRequestByteArray);
    protected DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
            baisExclusive);

    protected static String sharedLockRequest = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
            + "<D:lockinfo xmlns:D='DAV:'>"
            + "<D:lockscope><D:shared/></D:lockscope>"
            + "<D:locktype><D:write/></D:locktype>"
            + "<D:owner><D:href>I'am the Lock Owner</D:href></D:owner>"
            + "</D:lockinfo>";
    protected static byte[] sharedLockRequestByteArray = sharedLockRequest
            .getBytes();
    protected ByteArrayInputStream baisShared = new ByteArrayInputStream(
            sharedLockRequestByteArray);
    protected DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(
            baisShared);

    protected static String tmpFolder = "/tmp/tests";

    protected static String sourceCollectionPath = tmpFolder + "/sourceFolder";
    protected static String destCollectionPath = tmpFolder + "/destFolder";
    protected static String sourceFilePath = sourceCollectionPath
            + "/sourceFile";
    protected static String destFilePath = destCollectionPath + "/destFile";

    protected static String overwritePath = destCollectionPath
            + "/sourceFolder";

    protected static String[] sourceChildren = new String[] { "sourceFile" };
    protected static String[] destChildren = new String[] { "destFile" };

    @After
    public final void assertSatisfiedMockery() throws Exception {
        _mockery.assertIsSatisfied();
    }

    @Before
    public void setUpBeforeClass() {
        _mockery = new Mockery();
    }

    public static StoredObject initFolderStoredObject() {
        StoredObject so = initStoredObject(true, null);

        return so;
    }

    public static StoredObject initFileStoredObject(byte[] resourceContent) {
        StoredObject so = initStoredObject(false, resourceContent);

        return so;
    }

    private static StoredObject initStoredObject(boolean isFolder,
            byte[] resourceContent) {
        StoredObject so = new StoredObject();
        so.setFolder(isFolder);
        so.setCreationDate(new Date());
        so.setLastModified(new Date());
        if (!isFolder) {
            // so.setResourceContent(resourceContent);
            so.setResourceLength(resourceContent.length);
        } else {
            so.setResourceLength(0L);
        }

        return so;
    }

    public static StoredObject initLockNullStoredObject() {
        StoredObject so = new StoredObject();
        so.setNullResource(true);
        so.setFolder(false);
        so.setCreationDate(null);
        so.setLastModified(null);
        // so.setResourceContent(null);
        so.setResourceLength(0);

        return so;
    }

    public static LockedObject initLockNullLockedObject(ResourceLocks resLocks,
            String path) {

        LockedObject lo = new LockedObject(resLocks, path, false);
        lo.setExclusive(true);

        return lo;
    }
}
