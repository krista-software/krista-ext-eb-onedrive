package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.FileEntity;

/**
 * The stored result of a background download job, keyed by Task Id in {@link TaskResultStore} and
 * consumed by ManagementArea#getTheDownloadedFile (the WAIT_FOR_EVENT request). Nothing in this build
 * currently produces one (see ManagementArea's Javadoc) — the class exists so the request's own
 * contract is fully, correctly implemented and ready for a future producer.
 */
public class DownloadJobResult {
    public boolean success;
    public String message;
    public FileEntity file;

    public DownloadJobResult() {
    }

    public DownloadJobResult(boolean success, String message, FileEntity file) {
        this.success = success;
        this.message = message;
        this.file = file;
    }
}
