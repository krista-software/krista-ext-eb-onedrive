package app.krista.extensions.essentials.cloudfiles.ebonedrive.auth;

import app.krista.extensions.util.KeyValueStore;
import com.google.gson.Gson;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * Persists EbOnedriveMsAuthAttributes as JSON in the platform KeyValueStore.
 *
 * Two uses:
 *  - save(attrs, invokerId) / load(invokerId): the permanent per-invoker config.
 *  - save(attrs) -> temporary "authContextId_<uuid>" key: carries the candidate config through a
 *    browser OAuth round-trip (see Trap 0.10 in adding-krista-msauth-tabs/SKILL.md); the caller
 *    removes the key in a finally block.
 *
 * KeyValueStore handles encryption at rest — do not add a separate encryption layer here.
 */
@Service
public class EbOnedriveAttributeStore {

    private static final Logger LOG = LoggerFactory.getLogger(EbOnedriveAttributeStore.class);
    private static final Gson GSON = new Gson();

    private final KeyValueStore keyValueStore;

    @Inject
    public EbOnedriveAttributeStore(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    /** Permanent: keyed by invoker id. */
    public void save(EbOnedriveMsAuthAttributes attributes, String invokerId) {
        keyValueStore.put(invokerId, GSON.toJson(attributes.toMap()));
        LOG.info("Saved attributes for invoker {}", invokerId);
    }

    /** Transient: random authContextId key (remove in a finally block). */
    public String save(EbOnedriveMsAuthAttributes attributes) {
        String key = "authContextId_" + UUID.randomUUID();
        keyValueStore.put(key, GSON.toJson(attributes.toMap()));
        return key;
    }

    @SuppressWarnings("unchecked")
    public EbOnedriveMsAuthAttributes load(String key) {
        if (key == null) {
            return null;
        }
        Object stored = keyValueStore.get(key);
        if (stored == null) {
            return null;
        }
        try {
            Map<String, Object> map = GSON.fromJson(String.valueOf(stored), Map.class);
            return EbOnedriveMsAuthAttributes.create(map);
        } catch (Exception e) {
            LOG.error("Failed to deserialize attributes for key {}", key, e);
            return null;
        }
    }

    public void remove(String key) {
        keyValueStore.remove(key);
    }
}
