package app.krista.extensions.essentials.cloudfiles.ebonedrive.integration;

/**
 * Pure path/filter helpers shared by every ManagementArea request — kept framework-free so they can be
 * unit-tested directly (see krista-extension-testing's "extract for testability" rule).
 */
public final class OneDriveFilters {

    private OneDriveFilters() {
    }

    /** Case-insensitive substring match. A null/blank search term matches everything. */
    public static boolean matchesSearch(String itemName, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        if (itemName == null) {
            return false;
        }
        return itemName.toLowerCase().contains(search.toLowerCase());
    }

    /** True when itemEpochMillis is on/after fromDateEpochMillis. A null From Date matches everything. */
    public static boolean isOnOrAfter(Long itemEpochMillis, Long fromDateEpochMillis) {
        if (fromDateEpochMillis == null) {
            return true;
        }
        if (itemEpochMillis == null) {
            return false;
        }
        return itemEpochMillis >= fromDateEpochMillis;
    }

    /** Joins a parent folder path and a child name into a single absolute path, normalizing slashes. */
    public static String joinPath(String parentPath, String name) {
        String parent = parentPath == null ? "" : parentPath.trim();
        String child = name == null ? "" : name.trim();
        if (parent.isEmpty() || "/".equals(parent)) {
            return "/" + child;
        }
        if (parent.endsWith("/")) {
            return parent + child;
        }
        return parent + "/" + child;
    }

    /** The portion of a file name before the last '.', or the whole name if there is no extension. */
    public static String baseName(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** The portion of a file name after the last '.', or "" if there is no extension. */
    public static String extension(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1 ? name.substring(dot + 1) : "";
    }

    /** Normalizes a user-supplied folder path/id input: blank or "/" means the drive root. */
    public static boolean isRoot(String sourcePathOrId) {
        return sourcePathOrId == null || sourcePathOrId.isBlank() || "/".equals(sourcePathOrId.trim());
    }

    /** OneDrive's path-based ("colon") addressing needs each path SEGMENT URL-encoded, but the '/'
     * separators themselves preserved — plain URL-encoding the whole path would also encode '/'. */
    public static String encodePathSegments(String path) {
        String trimmed = path.trim();
        String noLeadingSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        String[] segments = noLeadingSlash.split("/");
        StringBuilder out = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('/');
            }
            out.append(java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return out.toString();
    }
}
