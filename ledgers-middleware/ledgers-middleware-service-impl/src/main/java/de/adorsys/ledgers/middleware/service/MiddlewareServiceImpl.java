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

package de.adorsys.ledgers.middleware.service;


import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.adorsys.ledgers.deposit.api.domain.BalanceBO;
import de.adorsys.ledgers.deposit.api.domain.DepositAccountBO;
import de.adorsys.ledgers.deposit.api.domain.PaymentBO;
import de.adorsys.ledgers.deposit.api.domain.TransactionStatusBO;
import de.adorsys.ledgers.deposit.api.exception.DepositAccountNotFoundException;
import de.adorsys.ledgers.deposit.api.exception.PaymentNotFoundException;
import de.adorsys.ledgers.deposit.api.exception.PaymentProcessingException;
import de.adorsys.ledgers.deposit.api.service.DepositAccountPaymentService;
import de.adorsys.ledgers.deposit.api.service.DepositAccountService;
import de.adorsys.ledgers.middleware.converter.AccountDetailsMapper;
import de.adorsys.ledgers.middleware.converter.PaymentConverter;
import de.adorsys.ledgers.middleware.converter.SCAMethodTOConverter;
import de.adorsys.ledgers.middleware.service.domain.account.AccountBalanceTO;
import de.adorsys.ledgers.middleware.service.domain.account.AccountDetailsTO;
import de.adorsys.ledgers.middleware.service.domain.payment.PaymentProductTO;
import de.adorsys.ledgers.middleware.service.domain.payment.PaymentTypeTO;
import de.adorsys.ledgers.middleware.service.domain.payment.TransactionStatusTO;
import de.adorsys.ledgers.middleware.service.domain.sca.SCAMethodTO;
import de.adorsys.ledgers.middleware.service.exception.AccountNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.AuthCodeGenerationMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.PaymentNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.PaymentProcessingMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.SCAMethodNotSupportedMiddleException;
import de.adorsys.ledgers.middleware.service.exception.SCAOperationExpiredMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.SCAOperationNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.SCAOperationUsedOrStolenMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.SCAOperationValidationMiddlewareException;
import de.adorsys.ledgers.middleware.service.exception.UserNotFoundMiddlewareException;
import de.adorsys.ledgers.postings.api.exception.LedgerAccountNotFoundException;
import de.adorsys.ledgers.sca.exception.AuthCodeGenerationException;
import de.adorsys.ledgers.sca.exception.SCAMethodNotSupportedException;
import de.adorsys.ledgers.sca.exception.SCAOperationExpiredException;
import de.adorsys.ledgers.sca.exception.SCAOperationNotFoundException;
import de.adorsys.ledgers.sca.exception.SCAOperationUsedOrStolenException;
import de.adorsys.ledgers.sca.exception.SCAOperationValidationException;
import de.adorsys.ledgers.sca.service.SCAOperationService;
import de.adorsys.ledgers.um.api.domain.AccessTypeBO;
import de.adorsys.ledgers.um.api.domain.AccountAccessBO;
import de.adorsys.ledgers.um.api.domain.ScaUserDataBO;
import de.adorsys.ledgers.um.api.exception.UserNotFoundException;
import de.adorsys.ledgers.um.api.service.UserService;

@Service
public class MiddlewareServiceImpl implements MiddlewareService {
    private static final Logger logger = LoggerFactory.getLogger(MiddlewareServiceImpl.class);
    private final DepositAccountPaymentService paymentService;
    private final SCAOperationService scaOperationService;
    private final DepositAccountService accountService;
    private final UserService userService;
    private final PaymentConverter paymentConverter;
    private final AccountDetailsMapper detailsMapper;
    private final SCAMethodTOConverter scaMethodTOConverter;

    public MiddlewareServiceImpl(DepositAccountPaymentService paymentService, SCAOperationService scaOperationService, DepositAccountService accountService, UserService userService, PaymentConverter paymentConverter, AccountDetailsMapper detailsMapper, SCAMethodTOConverter scaMethodTOConverter) {
        this.paymentService = paymentService;
        this.scaOperationService = scaOperationService;
        this.accountService = accountService;
        this.userService = userService;
        this.paymentConverter = paymentConverter;
        this.detailsMapper = detailsMapper;
        this.scaMethodTOConverter = scaMethodTOConverter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TransactionStatusTO getPaymentStatusById(String paymentId) throws
            PaymentNotFoundMiddlewareException {
        try {
            TransactionStatusBO paymentStatus = paymentService.getPaymentStatusById(paymentId);
            return TransactionStatusTO.valueOf(paymentStatus.name());
        } catch (PaymentNotFoundException e) {
            logger.error("Payment with id=" + paymentId + " not found", e);
            throw new PaymentNotFoundMiddlewareException(e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("PMD.IdenticalCatchBranches")
    public String generateAuthCode(String userLogin, SCAMethodTO scaMethod, String opData, String userMessage,
                                   int validitySeconds) throws AuthCodeGenerationMiddlewareException, SCAMethodNotSupportedMiddleException {
        try {
            ScaUserDataBO scaUserData = scaMethodTOConverter.toScaUserDataBO(scaMethod);
            return scaOperationService.generateAuthCode(userLogin, scaUserData, opData, userMessage, validitySeconds);
        } catch (AuthCodeGenerationException e) {
            logger.error(e.getMessage(), e);
            throw new AuthCodeGenerationMiddlewareException(e);
        } catch (SCAMethodNotSupportedException e) {
            logger.error(e.getMessage(), e);
            throw new SCAMethodNotSupportedMiddleException(e);
        }
    }

    @Override
    @SuppressWarnings("PMD.IdenticalCatchBranches")
    public boolean validateAuthCode(String opId, String opData, String authCode) throws
            SCAOperationNotFoundMiddlewareException, SCAOperationValidationMiddlewareException, SCAOperationExpiredMiddlewareException, SCAOperationUsedOrStolenMiddlewareException {
        try {
            return scaOperationService.validateAuthCode(opId, opData, authCode);
        } catch (SCAOperationNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new SCAOperationNotFoundMiddlewareException(e);
        } catch (SCAOperationValidationException e) {
            logger.error(e.getMessage(), e);
            throw new SCAOperationValidationMiddlewareException(e);
        } catch (SCAOperationExpiredException e) {
            logger.error(e.getMessage(), e);
            throw new SCAOperationExpiredMiddlewareException(e);
        } catch (SCAOperationUsedOrStolenException e) {
            logger.error(e.getMessage(), e);
            throw new SCAOperationUsedOrStolenMiddlewareException(e);
        }
    }

    @Override
    public AccountDetailsTO getAccountDetailsByAccountId(String accountId) throws AccountNotFoundMiddlewareException {
        try {
            DepositAccountBO account = accountService.getDepositAccountById(accountId);
            List<BalanceBO> balances = accountService.getBalances(account.getIban());
            return detailsMapper.toAccountDetailsTO(account, balances);
        } catch (DepositAccountNotFoundException | LedgerAccountNotFoundException e) {
            logger.error("Deposit Account with id=" + accountId + "not found", e);
            throw new AccountNotFoundMiddlewareException(e.getMessage(), e);
        }
    }

    @Override
    public List<AccountDetailsTO> getAllAccountDetailsByUserLogin(String userLogin) throws UserNotFoundMiddlewareException {
        logger.info("Retrieving accounts by user login {}", userLogin);
        try {
            List<AccountAccessBO> accountAccess = userService.getAccountAccessByUserLogin(userLogin);
            logger.info("{} accounts were retrieved", accountAccess.size());

            List<String> ibans = accountAccess.stream()
                                         .filter(a -> a.getAccessType() == AccessTypeBO.OWNER)
                                         .map(AccountAccessBO::getIban)
                                         .collect(Collectors.toList());
            logger.info("{} were accounts were filtered as OWN", ibans.size());

            List<DepositAccountBO> accounts = accountService.getDepositAccountsByIBAN(ibans);
            logger.info("{} deposit accounts were found", accounts.size());

            return detailsMapper.toAccountDetailsListTO(accounts);
        } catch (UserNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new UserNotFoundMiddlewareException(e.getMessage());
        }
    }

    @Override
    public <T> Object initiatePayment(T payment, PaymentTypeTO paymentType) {
        PaymentBO paymentInitiationResult = paymentService.initiatePayment(paymentConverter.toPaymentBO(payment, paymentType.getPaymentClass()));
        return paymentConverter.toPaymentTO(paymentInitiationResult);
    }

    @Override
    public List<SCAMethodTO> getSCAMethods(String userLogin) throws UserNotFoundMiddlewareException {
        try {
            List<ScaUserDataBO> userScaData = userService.getUserScaData(userLogin);
            return scaMethodTOConverter.toSCAMethodListTO(userScaData);
        } catch (UserNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new UserNotFoundMiddlewareException(e.getMessage());
        }
    }

    @Override
    public List<AccountBalanceTO> getBalances(String accountId) throws AccountNotFoundMiddlewareException {
        try {
            DepositAccountBO account = accountService.getDepositAccountById(accountId);
            List<BalanceBO> balances = accountService.getBalances(account.getIban());
            return detailsMapper.toAccountBalancesTO(balances);
        } catch (DepositAccountNotFoundException | LedgerAccountNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new AccountNotFoundMiddlewareException(e.getMessage(), e);
        }
    }
    @Override
    public TransactionStatusTO executePayment(String paymentId) throws PaymentProcessingMiddlewareException {
        try {
            TransactionStatusBO executePayment = paymentService.executePayment(paymentId);
            return TransactionStatusTO.valueOf(executePayment.name());
        } catch (PaymentNotFoundException | PaymentProcessingException e) {
            throw new PaymentProcessingMiddlewareException(paymentId, e);
        }
    }

    @Override //TODO Consider refactoring to avoid unchecked cast warnings
    public Object getPaymentById(PaymentTypeTO paymentType, PaymentProductTO paymentProduct, String paymentId) throws PaymentNotFoundMiddlewareException {
        try {
            PaymentBO paymentResult = paymentService.getPaymentById(paymentId);
            return paymentConverter.toPaymentTO(paymentResult);
        } catch (PaymentNotFoundException e) {
            logger.error("Payment with id={} not found", paymentId, e);
            throw new PaymentNotFoundMiddlewareException(e.getMessage(), e);
        }
    }
}
