/*
 * Copyright 2015 Stormpath, Inc.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @since 1.0.RC3
 */
public class AuthenticationFilter extends AccessControlFilter {

    @Override
    protected boolean isAccessAllowed(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return request.getRemoteUser() != null //non-null if authenticated
               || isLoginRequest(request); //allow them to visit the login URL otherwise they might not be able to login :)
    }

    @Override
    protected boolean onAccessDenied(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return getUnauthenticatedHandler().onAuthenticationRequired(request, response);
    }
}
