package cz.weborama.edoofox;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;

/** Only the exact school HTTPS origin belongs inside the app. */
public final class LinkPolicy {
    private final String host;
    public final String home;
    private static final List<String> EXTERNAL_SCHEMES = Arrays.asList("https", "http", "mailto", "tel", "sms", "geo");

    public LinkPolicy(String subdomain) {
        String normalized = normalizeSchool(subdomain);
        if (normalized == null) throw new IllegalArgumentException("Invalid school subdomain");
        host = normalized + ".edookit.net";
        home = "https://" + host + "/";
    }

    public String host() { return host; }

    public static String normalizeSchool(String input) {
        if (input == null) return null;
        String value = input.trim().toLowerCase(Locale.ROOT);
        return value.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?") ? value : null;
    }

    public boolean isInternal(String url) {
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host.equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (URISyntaxException | NullPointerException e) {
            return false;
        }
    }

    public static boolean canOpenExternally(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !EXTERNAL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) return false;
            if (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http")) {
                return uri.getHost() != null && uri.getRawUserInfo() == null;
            }
            return true;
        } catch (URISyntaxException | NullPointerException e) {
            return false;
        }
    }

    /** Edookit's verified identity service is part of sign-in, not a content link. */
    public static boolean isAuthentication(String url) {
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "uuidentity.plus4u.net".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && (uri.getPath().startsWith("/uu-oidc-maing02/")
                        || uri.getPath().startsWith("/uu-identitymanagement-maing01/"));
        } catch (URISyntaxException | NullPointerException e) {
            return false;
        }
    }

    public boolean isEmbedded(String url) {
        return isInternal(url) || isAuthentication(url);
    }
}
