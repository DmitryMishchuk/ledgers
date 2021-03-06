/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.ledgers.middleware.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.adorsys.ledgers.middleware.api.exception.UserNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.service.MiddlewareUserManagementService;
import de.adorsys.ledgers.middleware.rest.exception.NotFoundRestException;

@RestController
@RequestMapping(UserManagementResource.USERS)
public class UserManagementResource {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementResource.class);
    static final String USERS = "/users";
    private final MiddlewareUserManagementService middlewareUserService;

    public UserManagementResource(MiddlewareUserManagementService middlewareUserService) {
        this.middlewareUserService = middlewareUserService;
    }

    @PostMapping("/authorise")
    public boolean authorise(@RequestParam("login")String login, @RequestParam("pin") String pin){
        try {
            return middlewareUserService.authorise(login, pin);
        } catch (UserNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }
}
