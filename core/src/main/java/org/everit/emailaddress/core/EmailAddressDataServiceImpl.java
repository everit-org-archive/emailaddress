package org.everit.emailaddress.core;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.everit.email.api.EmailSenderParam;
import org.everit.email.api.EmailService;
import org.everit.emailaddress.api.EmailAddressDataService;
import org.everit.emailaddress.api.dto.EmailVerificationResult;
import org.everit.emailaddress.api.enums.ConfirmationResult;
import org.everit.emailaddress.api.expections.InvalidEmailAddressExpection;
import org.everit.emailaddress.api.expections.NoSuchEmailAddressDataException;
import org.everit.emailaddress.entity.EmailAddressDataEntity;
import org.everit.emailaddress.entity.EmailAddressDataEntity_;
import org.everit.messaging.api.dto.MessagePart;
import org.everit.messaging.api.model.TenureType;
import org.everit.messaging.api.param.MessageTenureParam;
import org.everit.util.core.velocity.VelocityUtil;
import org.everit.verifiabledata.api.VerifyService;
import org.everit.verifiabledata.api.dto.VerifiableDataCreation;
import org.everit.verifiabledata.api.dto.VerificationRequest;
import org.everit.verifiabledata.api.dto.VerificationResult;
import org.everit.verifiabledata.api.enums.TokenUsageResult;
import org.everit.verifiabledata.api.enums.VerificationLengthBase;
import org.everit.verifiabledata.api.exceptions.NonPositiveVerificationLength;
import org.everit.verifiabledata.entity.VerifiableDataEntity;

/**
 * Implementation of {@link EmailAddressDataService}.
 */
public class EmailAddressDataServiceImpl implements EmailAddressDataService {

    /**
     * Email regular expression to validation.
     */
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    /**
     * EntityManager set by blueprint.
     */
    private EntityManager em;
    /**
     * The {@link VerifyService} instance.
     */
    private VerifyService verifyService;

    /**
     * The {@link EmailService} instance.
     */
    private EmailService emailService;

    @Override
    public void createVerificationRequest(final long emailAddressId, final String messageTemplate,
            final Date tokenValidityEndDate,
            final long verificationLength, final VerificationLengthBase verificationLengthBase) {
        EmailAddressDataEntity emailAddressDataEntity = findEmailAddressDataEntityById(emailAddressId);
        if ((tokenValidityEndDate == null) || (verificationLengthBase == null) || (messageTemplate == null)) {
            throw new IllegalArgumentException(
                    "The tokenValidityEndDate or verificationLengthBase or messageTemplate parameter is null. "
                            + "Cannot be null.");
        } else if (verificationLength <= 0.0) {
            throw new NonPositiveVerificationLength();
        } else if (emailAddressDataEntity == null) {
            throw new NoSuchEmailAddressDataException();
        } else {
            if (emailAddressDataEntity.getVerifiableData() != null) {
                VerificationRequest verificationRequest = verifyService.createVerificationRequest(
                        emailAddressDataEntity.getVerifiableData().getVerifiableDataId(),
                        tokenValidityEndDate,
                        verificationLength,
                        verificationLengthBase);
                if (verificationRequest != null) {
                    Map<String, String> senderParams = new HashMap<String, String>();
                    senderParams.put(EmailSenderParam.EMAIL_ADDRESS, "localhost@localhost.hu");
                    MessageTenureParam sender = MessageTenureParam.createSender(0L, senderParams);

                    Map<String, String> receiverParams = new HashMap<String, String>();
                    receiverParams.put(EmailSenderParam.EMAIL_ADDRESS, emailAddressDataEntity.getEmailAddress());
                    MessageTenureParam reciver = MessageTenureParam.createReceiver(TenureType.TO, 0L, receiverParams);

                    Map<String, Object> variables = new HashMap<String, Object>();
                    variables.put("acceptToken", verificationRequest.getVerifyTokenUUID());
                    variables.put("rejectToken", verificationRequest.getRejectTokenUUID());

                    String emailBody = VelocityUtil.processVelocityTemplateFromString(messageTemplate, "ERROR",
                            variables);
                    MessagePart[] messageParts = new MessagePart[] { MessagePart.createInlineHtml(emailBody) };

                    emailService.sendMessage(sender, "Verification email", messageParts, null, reciver);
                }
            } else {
                VerifiableDataCreation createVerifiableData = verifyService.createVerifiableData(tokenValidityEndDate,
                        verificationLength, verificationLengthBase);
                if (createVerifiableData != null) {
                    emailAddressDataEntity.setVerifiableData(em.getReference(VerifiableDataEntity.class,
                            createVerifiableData.getVerifiableDataId()));
                    em.merge(emailAddressDataEntity);
                    em.flush();

                    Map<String, String> senderParams = new HashMap<String, String>();
                    senderParams.put(EmailSenderParam.EMAIL_ADDRESS, "localhost@localhost.hu");
                    MessageTenureParam sender = MessageTenureParam.createSender(0L, senderParams);

                    Map<String, String> receiverParams = new HashMap<String, String>();
                    receiverParams.put(EmailSenderParam.EMAIL_ADDRESS, emailAddressDataEntity.getEmailAddress());
                    MessageTenureParam reciver = MessageTenureParam.createReceiver(TenureType.TO, 0L, receiverParams);

                    Map<String, Object> variables = new HashMap<String, Object>();
                    variables.put("acceptToken", createVerifiableData.getVerificationRequest().getVerifyTokenUUID());
                    variables.put("rejectToken", createVerifiableData.getVerificationRequest().getRejectTokenUUID());

                    String emailBody = VelocityUtil.processVelocityTemplateFromString(messageTemplate, "ERROR",
                            variables);
                    MessagePart[] messageParts = new MessagePart[] { MessagePart.createInlineHtml(emailBody) };

                    emailService.sendMessage(sender, "Verification email", messageParts, null, reciver);
                }
            }
        }

    }

    /**
     * Determine the confirmation result.
     * 
     * @param tokenUsageResult
     *            the {@link TokenUsageResult} object.
     * @return the {@link ConfirmationResult} object.
     */
    private ConfirmationResult determineConfirmationResult(final TokenUsageResult tokenUsageResult) {
        ConfirmationResult result = null;
        if (tokenUsageResult.equals(TokenUsageResult.VERIFIED)) {
            result = ConfirmationResult.SUCCESS;
        } else if (tokenUsageResult.equals(TokenUsageResult.REJECTED)) {
            result = ConfirmationResult.REJECTED;
        } else {
            result = ConfirmationResult.FAILED;
        }
        return result;
    }

    /**
     * Finds email address data in the database.
     * 
     * @param emailAddressId
     *            the id of the email address data.
     * @return the {@link EmailAddressDataEntity} object if exist, otherwise <code>null</code>.
     */
    private EmailAddressDataEntity findEmailAddressDataEntityById(final long emailAddressId) {
        return em.find(EmailAddressDataEntity.class, emailAddressId);
    }

    /**
     * Finds the email address data bases on verifiable data.
     * 
     * @param verifiableDataId
     *            the id of the verifiable data.
     * @return the {@link EmailAddressDataEntity} object if only exist one, otherwise <code>null</code>.
     */
    private EmailAddressDataEntity findEmailAddressDataEntityByVerifiableDataId(final long verifiableDataId) {
        EmailAddressDataEntity result = null;
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EmailAddressDataEntity> criteriaQuery = cb.createQuery(EmailAddressDataEntity.class);
        Root<EmailAddressDataEntity> root = criteriaQuery
                .from(EmailAddressDataEntity.class);
        Predicate predicate = cb.equal(root.get(EmailAddressDataEntity_.verifiableData),
                em.getReference(VerifiableDataEntity.class, verifiableDataId));
        criteriaQuery.where(predicate);
        List<EmailAddressDataEntity> list = em.createQuery(criteriaQuery).getResultList();
        if (list.size() == 1) {
            result = list.get(0);
        }
        return result;
    }

    @Override
    public void invalidateEmailAddress(final long emailAddressId) {
        EmailAddressDataEntity emailAddressDataEntity = findEmailAddressDataEntityById(emailAddressId);
        if (emailAddressDataEntity == null) {
            throw new NoSuchEmailAddressDataException();
        }
        if (emailAddressDataEntity.getVerifiableData() != null) {
            em.lock(emailAddressDataEntity.getVerifiableData(), LockModeType.PESSIMISTIC_WRITE);
            verifyService.invalidateData(emailAddressDataEntity.getVerifiableData().getVerifiableDataId());
        }
        em.remove(emailAddressDataEntity);
    }

    @Override
    public boolean isEmailAddressVerified(final long emailAddressId) {
        boolean result = false;
        EmailAddressDataEntity emailAddressDataEntity = findEmailAddressDataEntityById(emailAddressId);
        if (emailAddressDataEntity == null) {
            throw new NoSuchEmailAddressDataException();
        }
        VerifiableDataEntity verifiableData = emailAddressDataEntity.getVerifiableData();
        Date actualDate = new Date();
        if ((verifiableData != null) && (verifiableData.getVerifiedUntil() != null)
                && (actualDate.getTime() < verifiableData.getVerifiedUntil().getTime())) {
            result = true;
        }
        return result;
    }

    @Override
    public long saveEmailAddress(final String emailAddress) {
        if (emailAddress == null) {
            throw new IllegalArgumentException("The emailAddress parameter is null. Cannot be null.");
        }
        long result = 0L;
        if (validateEmailAddress(emailAddress)) {
            EmailAddressDataEntity emailAddressDataEntity = new EmailAddressDataEntity();
            emailAddressDataEntity.setEmailAddress(emailAddress);
            em.persist(emailAddressDataEntity);
            em.flush();
            result = emailAddressDataEntity.getEmailAddressDataId();
        } else {
            throw new InvalidEmailAddressExpection();
        }
        return result;
    }

    public void setEm(final EntityManager em) {
        this.em = em;
    }

    public void setEmailService(final EmailService emailService) {
        this.emailService = emailService;
    }

    public void setVerifyService(final VerifyService verifyService) {
        this.verifyService = verifyService;
    }

    /**
     * Validate email address with regular expression.
     * 
     * @param emailAddress
     *            the email address for validation.
     * @return <code>true</code> if valid the email address, otherwise <code>false</code>.
     */
    private boolean validateEmailAddress(final String emailAddress) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(emailAddress);
        return matcher.matches();
    }

    @Override
    public EmailVerificationResult verifyEmailAddress(final String tokenUUID) {
        if (tokenUUID == null) {
            throw new IllegalArgumentException("The tokenUUID is null. Cannot be null the paramater.");
        }
        EmailVerificationResult result = null;
        VerificationResult verifyData = verifyService.verifyData(tokenUUID);
        if (verifyData != null) {
            EmailAddressDataEntity entity = findEmailAddressDataEntityByVerifiableDataId(verifyData
                    .getVerifiableDataId());
            if (entity != null) {
                result = new EmailVerificationResult(entity.getEmailAddressDataId(),
                        determineConfirmationResult(verifyData.getTokenUsageResult()));
            } else {
                result = new EmailVerificationResult(null, ConfirmationResult.FAILED);
            }
        } else {
            result = new EmailVerificationResult(null, ConfirmationResult.FAILED);
        }
        return result;
    }
}
