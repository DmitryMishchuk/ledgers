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

import de.adorsys.ledgers.middleware.api.domain.sca.AuthCodeDataTO;
import de.adorsys.ledgers.middleware.api.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.adorsys.ledgers.middleware.api.service.MiddlewareService;
import de.adorsys.ledgers.middleware.rest.domain.SCAGenerationResponse;
import de.adorsys.ledgers.middleware.rest.domain.SCAValidationRequest;
import de.adorsys.ledgers.middleware.rest.exception.ConflictRestException;
import de.adorsys.ledgers.middleware.rest.exception.NotFoundRestException;
import de.adorsys.ledgers.middleware.rest.exception.ValidationRestException;

@RestController
@RequestMapping(AuthCodeResource.AUTH_CODES)
public class AuthCodeResource {
    private final static Logger logger = LoggerFactory.getLogger(AuthCodeResource.class);
    static final String AUTH_CODES = "/auth-codes";

    private final MiddlewareService middlewareService;

    public AuthCodeResource(MiddlewareService middlewareService) {
		this.middlewareService = middlewareService;
	}

	@SuppressWarnings("PMD.IdenticalCatchBranches")
    @PostMapping(value = "/generate")
    public SCAGenerationResponse generate(@RequestBody AuthCodeDataTO data) {
        try {
            String opId = middlewareService.generateAuthCode(data);
            logger.debug("Operation id={} was generated for user={}", opId, data.getUserLogin());
            return new SCAGenerationResponse(opId);
        } catch (AuthCodeGenerationMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new ValidationRestException(e.getMessage()).withDevMessage(e.getMessage());
        } catch (SCAMethodNotSupportedMiddleException e) {
            logger.error(e.getMessage(), e);
            throw new ConflictRestException(e.getMessage()).withDevMessage(e.getMessage());
        } catch (UserNotFoundMiddlewareException | UserScaDataNotFoundMiddlewareException e) {
            logger.error(e.getMessage());
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @SuppressWarnings("PMD.IdenticalCatchBranches")
    @PostMapping("/{opId}/validate")
    public boolean validate(@PathVariable String opId, @RequestBody SCAValidationRequest request) {
        try {
            boolean valid = middlewareService.validateAuthCode(opId, request.getData(), request.getAuthCode());
            logger.debug("The validation of operation with id={} was {}", opId, valid ? "successful" : "failed");
            return valid;
        } catch (SCAOperationValidationMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new ValidationRestException(e.getMessage()).withDevMessage(e.getMessage());
        } catch (SCAOperationNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        } catch (SCAOperationUsedOrStolenMiddlewareException | SCAOperationExpiredMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new ConflictRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }
}
