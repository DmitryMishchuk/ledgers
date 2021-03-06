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

import de.adorsys.ledgers.middleware.api.domain.account.AccountBalanceTO;
import de.adorsys.ledgers.middleware.api.domain.account.AccountDetailsTO;
import de.adorsys.ledgers.middleware.api.domain.account.FundsConfirmationRequestTO;
import de.adorsys.ledgers.middleware.api.domain.account.TransactionTO;
import de.adorsys.ledgers.middleware.api.exception.AccountNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.TransactionNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.UserNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.service.MiddlewareAccountManagementService;
import de.adorsys.ledgers.middleware.rest.exception.ConflictRestException;
import de.adorsys.ledgers.middleware.rest.exception.NotFoundRestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/accounts")
public class AccountResource {
    private static final Logger logger = LoggerFactory.getLogger(AccountResource.class);

    private final MiddlewareAccountManagementService middlewareAccountService;

	public AccountResource(MiddlewareAccountManagementService middlewareAccountService) {
		this.middlewareAccountService = middlewareAccountService;
	}

	@GetMapping("/{accountId}")
    public ResponseEntity<AccountDetailsTO> getAccountDetailsById(@PathVariable String accountId) {
        try {
            return ResponseEntity.ok(middlewareAccountService.getAccountDetailsByAccountId(accountId, LocalDateTime.MAX));
        } catch (AccountNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @GetMapping("/balances/{accountId}")
    public ResponseEntity<List<AccountBalanceTO>> getBalances(@PathVariable String accountId) {
        try {
        	AccountDetailsTO accountDetails = middlewareAccountService.getAccountDetailsByAccountId(accountId, LocalDateTime.MAX);
            return ResponseEntity.ok(accountDetails.getBalances());
        } catch (AccountNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @GetMapping("{accountId}/transactions/{transactionId}")
    public ResponseEntity<TransactionTO> getTransactionById(@PathVariable String accountId, @PathVariable String transactionId) {
        try {
            return ResponseEntity.ok(middlewareAccountService.getTransactionById(accountId, transactionId));
        } catch (AccountNotFoundMiddlewareException | TransactionNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @GetMapping("{accountId}/transactions")
    public ResponseEntity<List<TransactionTO>> getTransactionByDates(@PathVariable String accountId,
                                                                     @RequestParam @Nullable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
                                                                     @RequestParam @Nullable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo) {

        dateChecker(dateFrom, dateTo);
        try {
            List<TransactionTO> transactions = middlewareAccountService.getTransactionsByDates(accountId, validDate(dateFrom), validDate(dateTo));
            return ResponseEntity.ok(transactions);
        } catch (AccountNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @GetMapping("/users/{userLogin}")
    public ResponseEntity<List<AccountDetailsTO>> getListOfAccountDetailsByUserId(@PathVariable String userLogin) {
        try {
            return ResponseEntity.ok(middlewareAccountService.getAllAccountDetailsByUserLogin(userLogin));
        } catch (UserNotFoundMiddlewareException | AccountNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @GetMapping("/ibans/{iban}")
    public ResponseEntity<AccountDetailsTO> getAccountDetailsByIban(@PathVariable String iban) {
        try {
            return ResponseEntity.ok(middlewareAccountService.getAccountDetailsByIban(iban));
        } catch (AccountNotFoundMiddlewareException e) {
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    @PostMapping(value = "/funds-confirmation")
    public ResponseEntity<Boolean> fundsConfirmation(@RequestBody FundsConfirmationRequestTO request){
	    try {
	        boolean fundsAvailable = middlewareAccountService.confirmFundsAvailability(request);
	        return ResponseEntity.ok(fundsAvailable);
        } catch (AccountNotFoundMiddlewareException e){
            logger.error(e.getMessage(), e);
            throw new NotFoundRestException(e.getMessage()).withDevMessage(e.getMessage());
        }
    }

    private void dateChecker(LocalDate dateFrom, LocalDate dateTo) {
        if (!validDate(dateFrom).isEqual(validDate(dateTo))
                    && validDate(dateFrom).isAfter(validDate(dateTo))) {
            throw new ConflictRestException("Illegal request dates sequence, possibly swapped 'date from' with 'date to'");
        }
    }

    private LocalDate validDate(LocalDate date) {
        return Optional.ofNullable(date)
                       .orElseGet(LocalDate::now);
    }
}
