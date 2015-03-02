package au.org.ala.commonui.headertails;

import au.org.ala.cas.util.AuthenticationCookieUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import java.io.File;
import java.io.InputStream;
import java.security.Principal;
import java.util.Properties;

/**
 * Util class to hold common code for both the banner/header and footer tag classes
 *
 * @author Nick dos Remedios (nick.dosremedios@csiro.au)
 */
public class HeaderAndTailUtil {
    // these fields can be overridden by a properties file (see below)
    protected static String bannerHtmlUrl = "http://www2.ala.org.au/commonui-bs2/banner.html";
    protected static String footerHtmlUrl = "http://www2.ala.org.au/commonui-bs2/footer.html";
    protected static String menuHtmlUrl = "http://www2.ala.org.au/commonui/menu.html";
    protected static String googleAnalyticsHtmlUrl = "http://www2.ala.org.au/commonui/analytics.html";
    // dynamic fields
    protected PageContext pageContext;
    protected Boolean populateSearchBox = true;
    protected String returnUrlPath = "";
    protected String returnLogoutUrlPath = "";
    protected String logoutControllerUrlPath = "";
    protected Boolean loggedIn = false;
    // template-style substitution variables
    protected static String headerFooterServerTag = "::headerFooterServer::";
    protected static String centralServerTag = "::centralServer::";
    protected static String casServerTag = "::casServerR::";
    protected static String loginLogoutListItemTag = "::loginLogoutListItem::";
    protected static String searchServerTag = "::searchServer::";
    protected static String searchPathTag = "::searchPath::";
    protected static String queryTag = "::query::";
    protected static String googleAnalyticsKeyTag = "::googleAnalyticsKey::";
    protected static String hideSearchFormTag = "id=\"header-search\"";
    // replacement variables
    protected static String googleAnalyticsKey = "UA-4355440-1";
    protected static String defaultHeaderFooterServer = "http://www2.ala.org.au/commonui-bs2";
    protected static String defaultCasServer = "https://auth.ala.org.au";
    protected static String defaultCentralServer = "http://www.ala.org.au";
    protected static String defaultSearchServer = "http://bie.ala.org.au";
    protected static String searchPath = "/search";
    protected static String defaultQuery = "Search the Atlas";
    protected static String hideSearchForm = "id=\"header-search\" style=\"display:none;\"";
    // logging
    private final static Logger logger = Logger.getLogger(HeaderAndTailUtil.class);
    protected static Properties configProperties;
    /**
     * Look for field overrides in properties file
     */
    static {
        Properties prop = new Properties();
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("headerTails.properties");

        try {
            prop.load(in);
            if (prop.getProperty("include.bannerUrl") != null) {
                bannerHtmlUrl = prop.getProperty("include.bannerUrl");
            }
            if (prop.getProperty("include.footerUrl") != null) {
                footerHtmlUrl = prop.getProperty("include.footerUrl");
            }
            in.close();
        } catch (Exception e) {
            logger.debug("Error loading properties file: " + e, e);
        }
        /* NC 2013-08-20 - We are actually changing this to use an JNDI environment property for "configPropFile"
                           
         
         */

        try{
            javax.naming.Context ctx = new javax.naming.InitialContext();
            String filename =(String)ctx.lookup("java:comp/env/configPropFile");
            configProperties = new Properties();
            configProperties.load(new java.io.FileInputStream(new java.io.File(filename)));
        } catch(Exception e){
            //don't do anything obviously can't find the value
        }

        if(configProperties == null){
            configProperties = prop;
        }
        logger.info("The configProperties " + configProperties);
    }

    /**
     * Constructor for login/logout tags
     *
     * @param pageContext
     * @param returnUrlPath
     * @param returnLogoutUrlPath
     */
    public HeaderAndTailUtil(PageContext pageContext, String returnUrlPath, String returnLogoutUrlPath, String logoutControllerUrlPath, Boolean populateSearchBox) {
        this.pageContext = pageContext;
        this.returnUrlPath = returnUrlPath;
        this.returnLogoutUrlPath = returnLogoutUrlPath;
        this.logoutControllerUrlPath = logoutControllerUrlPath;
        this.populateSearchBox = populateSearchBox;
        this.readPropsFromContext();
    }

    /**
     * Contructor for non login/logout tags
     *
     * @param pageContext
     */
    public HeaderAndTailUtil(PageContext pageContext) {
        this.pageContext = pageContext;
        this.readPropsFromContext();
    }
    /**
     * NC: 2013-08-13
     * Reads the property from the properties file before check to see if it has been provided in the web.xml as a init param. 
     */
    private String getPropertyValue(String property){        
        return configProperties.getProperty(property, pageContext.getServletContext().getInitParameter(property));
    }

    /**
     * Init method to override some fields via init params in web.xml
     */
    public void readPropsFromContext() {
        // Read some properties from the web.xml file via servlet context
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        String searchServer = getPropertyValue("searchServerName");
        if (StringUtils.isNotBlank(searchServer)) {
            defaultSearchServer = searchServer;
        }

        String casServer = getPropertyValue("casServerName");
        if (StringUtils.isNotBlank(casServer)) {
            defaultCasServer = casServer;
        }

        String centralServer = getPropertyValue("centralServer");
        if (StringUtils.isNotBlank(centralServer)) {
            defaultCentralServer = centralServer;
        }

        String headerFooterServer = getPropertyValue("headerFooterServer");
        if (StringUtils.isNotBlank(headerFooterServer)) {
            defaultHeaderFooterServer = headerFooterServer;
        }

        String query = request.getParameter("q");
        String queryAvoid = request.getParameter("xq");
        if (!populateSearchBox || StringUtils.isNotBlank(queryAvoid) || StringUtils.isBlank(query)) {
            defaultQuery = "Search the Atlas";
        }

        // if a return path isn't supplied, construct one from current request
        if (StringUtils.isBlank(returnUrlPath)) {
            StringBuffer requestURL = request.getRequestURL();
            String queryString = request.getQueryString();
            if (queryString != null || "".equals(queryString)) {
                requestURL.append('?');
                requestURL.append(queryString.replaceAll("\\+", "%2B"));
            }
            returnUrlPath = requestURL.toString();
        }

        // if no returnLogoutUrlPath , then use the returnUrlPath
        if (this.returnLogoutUrlPath == null || this.returnLogoutUrlPath.equals("")) {
            this.returnLogoutUrlPath = this.returnUrlPath;
        }

        // Check authentication status
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            loggedIn = true;
        } else {
            loggedIn = AuthenticationCookieUtils.isUserLoggedIn(request);
        }

    }

    /**
     * Get header HTML
     *
     * @deprecated
     * @param loggedIn
     * @param returnUrlPath
     * @return
     * @throws Exception
     */
    public static String getHeader(boolean loggedIn, String returnUrlPath) throws Exception {
        String output = null;
        
        output = getHeader(loggedIn, defaultHeaderFooterServer, defaultCentralServer, defaultCasServer, defaultSearchServer, returnUrlPath, defaultQuery);
        
        return output;
    }

    /**
     * Get banner HTML
     *
     * @return
     * @throws Exception
     */
    public String getBanner() throws Exception {
        String context = "";
        if (StringUtils.isNotEmpty(logoutControllerUrlPath)) {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            context = request.getContextPath();
        }
        return getBanner(loggedIn, defaultHeaderFooterServer, defaultCentralServer, defaultCasServer, defaultSearchServer, returnUrlPath, returnLogoutUrlPath, context + logoutControllerUrlPath, defaultQuery, populateSearchBox);
    }

    /**
     * Get header HTML (no longer used directly)
     *
     * @deprecated
     * @param loggedIn
     * @param centralServer
     * @param casServer
     * @param searchServer
     * @param returnUrlPath
     * @param query
     * @return
     * @throws Exception
     */
    public static String getHeader(boolean loggedIn, String headerFooterServer, String centralServer, String casServer, String searchServer, String returnUrlPath, String query) throws Exception {
        return getHeader(loggedIn, headerFooterServer, centralServer, casServer, searchServer, returnUrlPath, null, query);
    }

    /**
     * Get header HTML
     *
     * @deprecated
     * @param loggedIn
     * @param centralServer
     * @param casServer
     * @param searchServer
     * @param returnUrlPath
     * @param returnLogoutUrlPath
     * @param query
     * @return
     * @throws Exception
     */
    public static String getHeader(boolean loggedIn, String headerFooterServer, String centralServer, String casServer, String searchServer, String returnUrlPath, String returnLogoutUrlPath, String query) throws Exception {
        String banner = getBanner(loggedIn, headerFooterServer, centralServer, casServer, searchServer, returnUrlPath, returnLogoutUrlPath, "", query, true);

        return banner;
    }

    /**
     * Get banner HTML
     *
     * @param loggedIn
     * @param centralServer
     * @param casServer
     * @param searchServer
     * @param returnUrlPath
     * @param returnLogoutUrlPath
     * @param logoutControllerUrlPath
     * @param query
     * @return
     * @throws Exception
     */
    public static String getBanner(boolean loggedIn, String headerFooterServer, String centralServer, String casServer, String searchServer,
            String returnUrlPath, String returnLogoutUrlPath, String logoutControllerUrlPath, String query, Boolean populateSearchBox) throws Exception {

        logger.debug("getBanner params: loggedIn=" + loggedIn + ", centralServer=" + centralServer + ", casServer=" + casServer + ", searchServer=" + searchServer + ", returnUrlPath=" + returnUrlPath + ", returnLogoutUrlPath=" + returnLogoutUrlPath + ", logoutControllerUrlPath=" + logoutControllerUrlPath + ", query=" + query + ", populateSearchBox" + populateSearchBox);

        if (StringUtils.isEmpty(returnLogoutUrlPath)) {
            returnLogoutUrlPath = returnUrlPath;
        }

        String loginLogoutListItem;
        if (loggedIn) {
            if (StringUtils.isEmpty(logoutControllerUrlPath)) {
                loginLogoutListItem = "<a href='" + casServer + "/cas/logout?url=" + returnLogoutUrlPath + "'>Log out</a>";
            } else {
                loginLogoutListItem = "<a href='" + logoutControllerUrlPath + "?casUrl=" + casServer + "/cas/logout&appUrl=" + returnLogoutUrlPath + "'>Log out</a>";
            }
        } else {
            loginLogoutListItem = "<a href='" + casServer + "/cas/login?service=" + returnUrlPath + "'>Log in</a>";
        }


        // load the top banner
        String banner = GetWebContent.getInstance().getContent(bannerHtmlUrl);

        banner = banner.replaceAll(headerFooterServerTag, headerFooterServer);
        banner = banner.replaceAll(centralServerTag, centralServer);
        banner = banner.replaceAll(casServerTag, casServer);
        banner = banner.replaceAll(loginLogoutListItemTag, loginLogoutListItem);
        banner = banner.replaceAll(searchServerTag, searchServer);
        banner = banner.replaceAll(searchPathTag, searchPath);
        banner = banner.replaceAll(queryTag, query);

        if (!populateSearchBox) {
            // hide search form
            banner = banner.replaceAll(hideSearchFormTag, hideSearchForm);
        }

        return banner;
    }

    /**
     * Get menu HTML
     *
     * @return
     * @throws Exception
     */
    public String getMenu() throws Exception {
        return getMenu(defaultCentralServer);
    }

    /**
     * Get menu HTML
     *
     * @param centralServer
     * @return
     * @throws Exception
     */
    public static String getMenu(String centralServer) throws Exception {
        // load the menu
        String menu = GetWebContent.getInstance().getContent(menuHtmlUrl);
        menu = menu.replaceAll(centralServerTag, centralServer);

        return menu;
    }

    /**
     * Get footer HTML
     *
     * @return
     * @throws Exception
     */
    public String getFooterHtml() throws Exception {
        return getFooterHtml(defaultCentralServer, defaultHeaderFooterServer);
    }

    /**
     * Get footer JS HTML
     *
     * @return
     * @throws Exception
     */
    public String getFooterJs() throws Exception {
        return getFooterJs(defaultCentralServer);
    }

    /**
     * Get footer HTML
     *
     * @param centralServer
     * @return
     * @throws Exception
     */
    public static String getFooterHtml(String centralServer, String headerFooterServer) throws Exception {
        String output = GetWebContent.getInstance().getContent(footerHtmlUrl);
        output = output.replaceAll(centralServerTag, centralServer);
        output = output.replaceAll(headerFooterServerTag, headerFooterServer);

        return output;
    }

    /**
     * Get footer JS HTML
     *
     * @param centralServer
     * @return
     * @throws Exception
     */
    public static String getFooterJs(String centralServer) throws Exception {
        String analytics = GetWebContent.getInstance().getContent(googleAnalyticsHtmlUrl);
        analytics = analytics.replaceAll(googleAnalyticsKeyTag, googleAnalyticsKey);

        return analytics;
    }

    /**
     * Get footer HTML (both footer and analytics JS)
     *
     * @deprecated
     * @param centralServer
     * @param headerFooterServer
     * @return
     * @throws Exception
     */
    public static String getFooter(String centralServer, String headerFooterServer) throws Exception {
        return getFooterHtml(centralServer, headerFooterServer) + getFooterJs(centralServer);
    }
}
