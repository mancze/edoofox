# Privacy and service boundaries

Edoofox loads the school website selected by the user in Android WebView.
That website and Plus4U handle login and school data; their privacy terms apply.
Requests go to those services and any resources used by their pages.

The app stores the school subdomain on the device. WebView stores cookies and
website data, including session information. The website controls session
duration. No separate app-owned account or backend is operated by this project,
and no analytics SDK is included.

The app does not intentionally extract credentials from the sign-in page.
Credentials entered into the page are processed by the website. JavaScript and
DOM storage are enabled because the service needs them. Only the exact selected
school HTTPS origin and specified Plus4U authentication paths are allowed for
in-app top-level navigation. This does not restrict every subresource loaded
by a web page.

Other supported links open external applications. Downloads are handed to the
browser and may require a separate login. Those apps have their own privacy
settings and parental controls. Family Link behavior is not guaranteed or
certified by this project.

Switching schools resets page navigation but retains cookies scoped to their
originating domains. It is not a logout or a full browser-data reset.
Android app backup is disabled. Uninstalling removes local app data; it does not
delete data held by the school or service providers.

Bug reports and public screenshots must not contain student names, grades,
messages, tokens, cookies, or login credentials. Please use fictional fixtures.
