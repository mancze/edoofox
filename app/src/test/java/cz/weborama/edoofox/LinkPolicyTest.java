package cz.weborama.edoofox;

import org.junit.Test;
import static org.junit.Assert.*;

public class LinkPolicyTest {
    private final LinkPolicy policy = new LinkPolicy("zsslovanak");
    @Test public void validatesAndNormalizesSubdomains() {
        assertEquals("my-school", LinkPolicy.normalizeSchool(" MY-School "));
        assertEquals("a", LinkPolicy.normalizeSchool("a"));
        assertNotNull(LinkPolicy.normalizeSchool(new String(new char[63]).replace('\0', 'a')));
        for (String input : new String[]{null, "", " ", "-school", "school-", "school_name", "two words",
                "school.edookit.net", "https://school.edookit.net/", "school/other", "user@school", "škola",
                new String(new char[64]).replace('\0', 'a')}) {
            assertNull(String.valueOf(input), LinkPolicy.normalizeSchool(input));
        }
    }

    @Test public void selectedSchoolDeterminesHomeAndInternalLinks() {
        LinkPolicy other = new LinkPolicy("another-school");
        assertEquals("https://another-school.edookit.net/", other.home);
        assertTrue(other.isInternal("https://another-school.edookit.net/messages"));
        assertFalse(other.isInternal(policy.home));
        assertFalse(policy.isInternal(other.home));
        assertFalse(other.isInternal("https://another-school.edookit.net.evil.test/"));
        assertTrue(other.isEmbedded("https://uuidentity.plus4u.net/uu-oidc-maing02/client/auth"));
    }

    @Test(expected = IllegalArgumentException.class) public void invalidSchoolCannotCreatePolicy() {
        new LinkPolicy("school.edookit.net");
    }
    @Test public void exactSchoolHttpsOriginStaysInside() {
        assertTrue(policy.isInternal(policy.home));
        assertTrue(policy.isInternal("https://ZSSLOVANAK.EDOOKIT.NET:443/messages?q=one#two"));
    }

    @Test public void externalAndDeceptiveOriginsNeverStayInside() {
        for (String url : new String[]{"https://edookit.net", "https://other.edookit.net/",
                "https://zsslovanak.edookit.net.evil.test/", "https://zsslovanak.edookit.net@evil.test/",
                "http://zsslovanak.edookit.net/", "https://zsslovanak.edookit.net:444/",
                "https://user@zsslovanak.edookit.net/", "//zsslovanak.edookit.net/", "javascript:alert(1)", null}) {
            assertFalse(String.valueOf(url), policy.isInternal(url));
        }
    }

    @Test public void onlySupportedExternalSchemesAreDispatched() {
        for (String url : new String[]{"https://example.com/", "http://example.com/", "mailto:a@example.com", "tel:+420123456789"}) {
            assertTrue(url, LinkPolicy.canOpenExternally(url));
        }
        for (String url : new String[]{"javascript:alert(1)", "file:///etc/passwd", "content://private/data",
                "intent://anything", "data:text/html,hello", "https://", "https://user:secret@example.com", null}) {
            assertFalse(String.valueOf(url), LinkPolicy.canOpenExternally(url));
        }
    }

    @Test public void signInExceptionIsLimitedToVerifiedIdentityPaths() {
        assertTrue(policy.isEmbedded("https://uuidentity.plus4u.net/uu-oidc-maing02/client/oidc/authorize"));
        assertTrue(policy.isEmbedded("https://uuidentity.plus4u.net/uu-identitymanagement-maing01/client/login"));
        for (String url : new String[]{"https://uuidentity.plus4u.net/unrelated", "https://plus4u.net/",
                "https://evil.plus4u.net/uu-oidc-maing02/", "https://uuidentity.plus4u.net.evil.test/uu-oidc-maing02/",
                "https://user@uuidentity.plus4u.net/uu-oidc-maing02/", "http://uuidentity.plus4u.net/uu-oidc-maing02/"}) {
            assertFalse(url, LinkPolicy.isAuthentication(url));
        }
    }
}
