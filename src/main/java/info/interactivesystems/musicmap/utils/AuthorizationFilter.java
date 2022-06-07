package info.interactivesystems.musicmap.utils;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.musicmap.UserBean;

/**
 * WebFilter that checks whether a user is logged in via Spotify and, if not, redirects to Spotify's login page.
 * 
 * @author Johannes Kunkel
 *
 */
@WebFilter("*")
public class AuthorizationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Inject
    private UserBean userBean;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
	HttpServletRequest request = (HttpServletRequest) req;
	HttpServletResponse response = (HttpServletResponse) res;

	String url = request.getRequestURL().toString();
	String userCode = request.getParameter("code");
	String caseId = request.getParameter("ci");
	if (caseId != null) {
	    logger.info("Got case id from parameter: {}", caseId);
	    userBean.setCaseId(Integer.parseInt(caseId));
	}
	String condition = request.getParameter("uc");
	if (condition != null) {
	    userBean.setCondition(Integer.parseInt(condition));
	} else {
	    if (url.contains("list.jsf")) {
		userBean.setCondition(1);
	    } else if (url.contains("treemap.jsf")) {
		userBean.setCondition(2);
	    } else if (url.contains("map.jsf")) {
		userBean.setCondition(3);
	    }
	}

	if (!isUserLoggedIn() && userCode == null) { // 1st client request (no session started)
	    userBean.setConditionUrl(url);
	    response.sendRedirect(response.encodeRedirectURL(SpotifyUtils.getAuthorizationCodeUriRequest()));
	    return;
	} else if (!isUserLoggedIn() && userCode != null) { // 2nd request: Spotify code is available, user is not retrieved yet
	    userBean.authorizeUser(userCode);
	    response.sendRedirect(response.encodeRedirectURL(userBean.getConditionUrl()));
	    return;
	}
	chain.doFilter(request, response);
    }

    private boolean isUserLoggedIn() {
	boolean alreadyLoggedIn = (userBean != null) && (userBean.getUser() != null);
	return alreadyLoggedIn;
    }

    @Override
    public void destroy() {
    }

}
