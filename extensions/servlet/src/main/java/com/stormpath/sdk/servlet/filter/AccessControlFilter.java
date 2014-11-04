/*
 * Copyright 2014 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.sdk.servlet.filter;

import com.stormpath.sdk.lang.Assert;
import com.stormpath.sdk.lang.Strings;
import com.stormpath.sdk.servlet.config.UriCleaner;
import com.stormpath.sdk.servlet.util.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;

public abstract class AccessControlFilter extends HttpFilter {

    protected String loginUrl;

    @Override
    protected void onInit() throws ServletException {
        super.onInit();
        this.loginUrl = UriCleaner.INSTANCE.clean(getConfig().getLoginUrl());
    }

    /**
     * Returns <code>true</code> if the request is allowed to proceed through the filter normally, or
     * <code>false</code>
     * if the request should be handled by the
     * {@link #onAccessDenied(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * onAccessDenied}
     * method instead.
     *
     * @param request  the incoming <code>ServletRequest</code>
     * @param response the outgoing <code>ServletResponse</code>
     * @return <code>true</code> if the request should proceed through the filter normally, <code>false</code> if the
     * request should be processed by this filter's
     * {@link #onAccessDenied(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * onAccessDenied} method instead.
     * @throws Exception if an error occurs during processing.
     */
    protected abstract boolean isAccessAllowed(HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * Processes requests where the subject was denied access as determined by the
     * {@link #isAccessAllowed(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * isAccessAllowed}.
     *
     * @param request  the incoming <code>ServletRequest</code>
     * @param response the outgoing <code>ServletResponse</code>
     * @return <code>true</code> if the request should continue to be processed; false if the subclass will
     * handle/render the response directly.
     * @throws Exception if there is an error processing the request.
     * @since 1.0
     */
    protected abstract boolean onAccessDenied(HttpServletRequest request, HttpServletResponse response) throws Exception;

    @Override
    protected boolean isContinue(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return isAccessAllowed(request, response) || onAccessDenied(request, response);
    }

    protected boolean isLoginRequest(HttpServletRequest request) {
        String contextRelativeUri = ServletUtils.getContextRelativeUri(request);
        return loginUrl.equals(contextRelativeUri);
    }

    protected boolean redirectToLogin(HttpServletRequest request, HttpServletResponse response, String status) throws Exception {

        Assert.notNull(status, "status argument cannot be null.");

        //not authenticated, so we'll redirect the user the login url and the 'next' parameter will be equal
        //to the currently requested URL *if* the request is a GET request.  POST requests are rarely safe to
        //automatically execute automatically (not idempotent, etc), so we just return to the default login 'nextUrl'
        //if not a GET

        String redirectUrl = getConfig().getLoginUrl();
        String query = null;

        int i = redirectUrl.indexOf('?');
        if (i != -1) {
            if (i == redirectUrl.length() - 1) {
                query = Strings.EMPTY_STRING;
            } else {
                query = redirectUrl.substring(i+1);
            }
        }

        if (query == null) {
            redirectUrl += "?status=" + status;
        } else if (!query.contains("status")) {

            if (!query.equals(Strings.EMPTY_STRING)) {
                redirectUrl += "&";
            }

            redirectUrl += "status=" + status;
        }

        String method = request.getMethod();
        if (method.equalsIgnoreCase("GET")) {

            String currentUrlString = request.getRequestURL().toString();
            query = request.getQueryString();
            if (query != null) {
                currentUrlString += "?" + query;
            }

            String encodedCurrentUrlString = URLEncoder.encode(currentUrlString, "UTF-8");

            redirectUrl += "&next=" + encodedCurrentUrlString;
        }

        ServletUtils.issueRedirect(request, response, redirectUrl, null, true, true);

        return false;
    }
}
