package app.krista.extensions.essentials.cloudfiles.ebonedrive.auth;

import app.krista.extensions.util.KeyValueStore;
import app.krista.sdk.msauth.token.ITokenStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * The ONE persistent token store this extension uses — backed by the platform KeyValueStore. Must be
 * injected everywhere an ITokenStore is needed (EbOnedriveAuthResource's OAuth callback/testConnection
 * AND the Graph client provider used for real business logic) — see Trap 0.11 in
 * adding-krista-msauth-tabs/SKILL.md. Never construct a separate `new InMemoryTokenStore()`.
 */
@Service
public class EbOnedriveTokenStore implements ITokenStore {

    private final KeyValueStore keyValueStore;

    @Inject
    public EbOnedriveTokenStore(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    @Override
    public void save(String key, String value) {
        keyValueStore.put(key, value);
    }

    @Override
    public String get(String key) {
        Object raw = keyValueStore.get(key, Object.class);
        return raw != null ? raw.toString() : null;
    }

    @Override
    public void delete(String key) {
        keyValueStore.remove(key);
    }
}
