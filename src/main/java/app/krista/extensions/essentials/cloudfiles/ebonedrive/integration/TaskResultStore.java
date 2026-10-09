package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

import app.krista.extensions.util.KeyValueStore;
import com.google.gson.Gson;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * KeyValueStore-backed durable result cache for the Task-ID / WAIT_FOR_EVENT pattern this domain's
 * reused "List Files" / "List Folders" / "Get the Downloaded file" contract uses (see ManagementArea's
 * class Javadoc). A QUERY_SYSTEM/CHANGE_SYSTEM request stores its result here under a freshly minted
 * Task ID and fires an internal event carrying just that Task ID; the paired WAIT_FOR_EVENT request
 * loads the full result from here once its Task ID filter matches the delivered event.
 */
@Service
public class TaskResultStore {

    private static final Logger LOG = LoggerFactory.getLogger(TaskResultStore.class);
    private static final Gson GSON = new Gson();

    private final KeyValueStore keyValueStore;

    @Inject
    public TaskResultStore(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public void saveListResult(String taskId, ListJobResult result) {
        keyValueStore.put(taskId, GSON.toJson(result));
    }

    public ListJobResult loadListResult(String taskId) {
        Object stored = keyValueStore.get(taskId);
        if (stored == null) {
            return null;
        }
        try {
            return GSON.fromJson(String.valueOf(stored), ListJobResult.class);
        } catch (Exception e) {
            LOG.error("Failed to deserialize list job result for task {}", taskId, e);
            return null;
        }
    }

    public void saveDownloadResult(String taskId, DownloadJobResult result) {
        keyValueStore.put(taskId, GSON.toJson(result));
    }

    public DownloadJobResult loadDownloadResult(String taskId) {
        Object stored = keyValueStore.get(taskId);
        if (stored == null) {
            return null;
        }
        try {
            return GSON.fromJson(String.valueOf(stored), DownloadJobResult.class);
        } catch (Exception e) {
            LOG.error("Failed to deserialize download job result for task {}", taskId, e);
            return null;
        }
    }

    public void remove(String taskId) {
        keyValueStore.remove(taskId);
    }
}
