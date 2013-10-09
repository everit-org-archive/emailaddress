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
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.everit.email.api.EmailSenderParam;
import org.everit.email.api.EmailService;
import org.everit.emailaddress.api.EmailAddressDataService;
import org.everit.emailaddress.api.dto.EmailVerificationResult;
import org.everit.emailaddress.api.enums.ConfirmationResult;
import org.everit.emailaddress.api.exceptions.InvalidEmailAddressException;
import org.everit.emailaddress.api.exceptions.NoSuchEmailAddressDataException;
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
import org.everit.verifiabledata.entity.VerifiableDataEntity_;

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
        if ((tokenValidityEndDate == null) || (verificationLengthBase == null) || (messageTemplate == null)) {
            throw new IllegalArgumentException(
                    "The tokenValidityEndDate or verificationLengthBase or messageTemplate parameter is null. "
                            + "Cannot be null.");
        }

        if (verificationLength <= 0.0) {
            throw new NonPositiveVerificationLength();
        }

        if (!existEmailAddressData(emailAddressId)) {
            throw new NoSuchEmailAddressDataException();
        }

        EmailAddressDataEntity emailAddressDataEntity = findEmailAddressDataEntityById(emailAddressId);
        if (emailAddressDataEntity.getVerifiableData() != null) {
            VerificationRequest verificationRequest = verifyService.createVerificationRequest(
                    emailAddressDataEntity.getVerifiableData().getVerifiableDataId(),
                    tokenValidityEndDate,
                    verificationLength,
                    verificationLengthBase);
            if (verificationRequest != null) {
                sendEmail(emailAddressDataEntity.getEmailAddress(),
                        verificationRequest.getVerifyTokenUUID(),
                        verificationRequest.getRejectTokenUUID(),
                        messageTemplate);
            }
        } else {
            VerifiableDataCreation createVerifiableData = verifyService.createVerifiableData(tokenValidityEndDate,
                    verificationLength, verificationLengthBase);
            if (createVerifiableData != null) {
                emailAddressDataEntity.setVerifiableData(em.getReference(VerifiableDataEntity.class,
                        createVerifiableData.getVerifiableDataId()));
                em.merge(emailAddressDataEntity);
                em.flush();

                sendEmail(emailAddressDataEntity.getEmailAddress(),
                        createVerifiableData.getVerificationRequest().getVerifyTokenUUID(),
                        createVerifiableData.getVerificationRequest().getRejectTokenUUID(),
                        messageTemplate);
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
     * Checks the email address data is exist or not.
     * 
     * @param emailAddressId
     *            the id of the email address.
     * @return <code>true</code> if exist, otherwise <code>false</code>.
     */
    private boolean existEmailAddressData(final long emailAddressId) {
        String emailAddressByEmailAddressId = getEmailAddressByEmailAddressId(emailAddressId);
        if (emailAddressByEmailAddressId != null) {
            return true;
        }
        return false;
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
     * Get email address based on email address id.
     * 
     * @param emailAddressId
     *            the id of the email address.
     * @return the email address. If not exist email address data return <code>null</code>.
     */
    private String getEmailAddressByEmailAddressId(final long emailAddressId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = cb.createQuery(String.class);

        Root<EmailAddressDataEntity> root =
                criteriaQuery.from(EmailAddressDataEntity.class);

        criteriaQuery.select(root.get(EmailAddressDataEntity_.emailAddress));

        Predicate predicate = cb.equal(root.get(EmailAddressDataEntity_.emailAddressDataId), emailAddressId);

        criteriaQuery.where(predicate);
        List<String> resultList = em.createQuery(criteriaQuery).getResultList();
        if (resultList.size() == 1) {
            return resultList.get(0);
        }
        return null;
    }

    /**
     * Get email address id based on verifiable data id.
     * 
     * @param verifiableDataId
     *            the id of the verifiable data.
     * @return the email address id if exist, otherwise return <code>null</code>.
     */
    private Long getEmailAddressIdByVerifiableDataId(final long verifiableDataId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = cb.createQuery(Long.class);

        Root<EmailAddressDataEntity> root =
                criteriaQuery.from(EmailAddressDataEntity.class);

        criteriaQuery.select(root.get(EmailAddressDataEntity_.emailAddressDataId));

        Predicate predicate = cb.equal(root.get(EmailAddressDataEntity_.verifiableData),
                em.getReference(VerifiableDataEntity.class, verifiableDataId));

        criteriaQuery.where(predicate);
        List<Long> resultList = em.createQuery(criteriaQuery).getResultList();
        if (resultList.size() == 1) {
            return resultList.get(0);
        }
        return null;
    }

    /**
     * Get the verifiable data id based on email address id.
     * 
     * @param emailAddressId
     *            the id of the email address data.
     * @return the verifiable data if exist, otherwise return <code>null</code>.
     */
    private Long getVerifiableDataIdByEmailAddressId(final long emailAddressId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = cb.createQuery(Long.class);
        Root<EmailAddressDataEntity> root =
                criteriaQuery.from(EmailAddressDataEntity.class);
        Join<EmailAddressDataEntity, VerifiableDataEntity> vde =
                root.join(EmailAddressDataEntity_.verifiableData);
        criteriaQuery.select(vde.get(VerifiableDataEntity_.verifiableDataId));
        Predicate predicate = cb.equal(root.get(EmailAddressDataEntity_.emailAddressDataId), emailAddressId);
        criteriaQuery.where(predicate);
        List<Long> resultList = em.createQuery(criteriaQuery).getResultList();
        if (resultList.size() == 1) {
            return resultList.get(0);
        }
        return null;
    }

    /**
     * Get the verification end date based on email address data id.
     * 
     * @param emailAddressId
     *            the id of the email address data.
     * @return the verification end date. If no result return <code>null</code>. <b>Important</b> the verification end
     *         date itself may be <code>null</code>.
     */
    private Date getVerificationEndDateByEmailAddressId(final long emailAddressId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Date> criteriaQuery = cb.createQuery(Date.class);
        Root<EmailAddressDataEntity> root =
                criteriaQuery.from(EmailAddressDataEntity.class);
        Join<EmailAddressDataEntity, VerifiableDataEntity> vde =
                root.join(EmailAddressDataEntity_.verifiableData);
        criteriaQuery.select(vde.get(VerifiableDataEntity_.verifiedUntil));
        Predicate predicate = cb.equal(root.get(EmailAddressDataEntity_.emailAddressDataId), emailAddressId);
        criteriaQuery.where(predicate);
        List<Date> resultList = em.createQuery(criteriaQuery).getResultList();
        if (resultList.size() == 1) {
            return resultList.get(0);
        }
        return null;
    }

    @Override
    public void invalidateEmailAddress(final long emailAddressId) {
        if (!existEmailAddressData(emailAddressId)) {
            throw new NoSuchEmailAddressDataException();
        }
        Long verifiableDataId = getVerifiableDataIdByEmailAddressId(emailAddressId);
        if (verifiableDataId != null) {
            em.lock(em.getReference(VerifiableDataEntity.class, verifiableDataId), LockModeType.PESSIMISTIC_WRITE);
            verifyService.invalidateData(verifiableDataId);
        }
        em.remove(em.getReference(EmailAddressDataEntity.class, emailAddressId));
    }

    @Override
    public boolean isEmailAddressVerified(final long emailAddressId) {
        boolean result = false;
        if (!existEmailAddressData(emailAddressId)) {
            throw new NoSuchEmailAddressDataException();
        }
        Date verificationEndByEmailAddressId = getVerificationEndDateByEmailAddressId(emailAddressId);
        Date actualDate = new Date();
        if ((verificationEndByEmailAddressId != null)
                && (actualDate.getTime() < verificationEndByEmailAddressId.getTime())) {
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
            throw new InvalidEmailAddressException();
        }
        return result;
    }

    /**
     * Sending email which contains the tokens.
     * 
     * @param receiverEmailAddress
     *            the receiver email address.
     * @param verifyToken
     *            the verify token.
     * @param rejectToken
     *            the reject token.
     * @param messageTemplate
     *            the message template. Replacing the $rejectToken variable the reject token and $acceptToken variable
     *            the accept token.
     */
    private void sendEmail(final String receiverEmailAddress, final String verifyToken, final String rejectToken,
            final String messageTemplate) {
        Map<String, String> senderParams = new HashMap<String, String>();
        senderParams.put(EmailSenderParam.EMAIL_ADDRESS, "localhost@localhost.hu");
        MessageTenureParam sender = MessageTenureParam.createSender(0L, senderParams);

        Map<String, String> receiverParams = new HashMap<String, String>();
        receiverParams.put(EmailSenderParam.EMAIL_ADDRESS, receiverEmailAddress);
        MessageTenureParam reciver = MessageTenureParam.createReceiver(TenureType.TO, 0L, receiverParams);

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("acceptToken", verifyToken);
        variables.put("rejectToken", rejectToken);

        String emailBody = VelocityUtil.processVelocityTemplateFromString(messageTemplate, "ERROR",
                variables);
        MessagePart[] messageParts = new MessagePart[] { MessagePart.createInlineHtml(emailBody) };

        emailService.sendMessage(sender, "Verification email", messageParts, null, reciver);
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
            Long emailAddressId = getEmailAddressIdByVerifiableDataId(verifyData.getVerifiableDataId());
            if (emailAddressId != null) {
                result = new EmailVerificationResult(emailAddressId,
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
