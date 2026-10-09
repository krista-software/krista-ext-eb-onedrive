package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import app.krista.extensions.essentials.cloudfiles.ebonedrive.catalog.entities.CustomFileEntity;

import java.util.List;

/**
 * The stored result of a List Files / List Folders job, keyed by Task ID in {@link TaskResultStore} and
 * consumed by ManagementArea#getResult (the WAIT_FOR_EVENT request). Plain data holder — Gson
 * serializes/deserializes it directly.
 */
public class ListJobResult {
    public boolean success;
    public String message;
    public List<CustomFileEntity> items;

    public ListJobResult() {
    }

    public ListJobResult(boolean success, String message, List<CustomFileEntity> items) {
        this.success = success;
        this.message = message;
        this.items = items;
    }
}
