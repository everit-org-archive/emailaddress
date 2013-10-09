package org.everit.emailaddress.itests.core;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import junit.framework.Assert;

import org.everit.emailaddress.api.EmailAddressDataService;
import org.everit.emailaddress.api.dto.EmailVerificationResult;
import org.everit.emailaddress.api.enums.ConfirmationResult;
import org.everit.emailaddress.api.exceptions.InvalidEmailAddressException;
import org.everit.emailaddress.api.exceptions.NoSuchEmailAddressDataException;
import org.everit.util.core.mail.greenmail.GreenmailService;
import org.everit.verifiabledata.api.enums.VerificationLengthBase;
import org.everit.verifiabledata.api.exceptions.NonPositiveVerificationLength;

import com.icegreen.greenmail.util.GreenMail;

/**
 * Implementation of {@link EmailAddressDataServiceTest}.
 */
public class EmailAddressDataServiceTestImpl implements EmailAddressDataServiceTest {

    /**
     * Contains the invalid email addresses.
     */
    private static final List<String> INVALID_EMAILS = Arrays.asList("test", "test@.com.my",
            "test123@gmail.a", "test123@.com", "test123@.com.com",
            ".test@test.com", "test()*@gmail.com", "test@%*.com",
            "test..2002@gmail.com", "test.@gmail.com",
            "test@test@gmail.com", "test@gmail.com.1a");

    /**
     * The maximum value of the random.
     */
    private static final int MAX_RANDOM_VALUE = 1000000;

    /**
     * Contains the valid email addresses.
     */
    private static final List<String> VALID_EMAILS = Arrays.asList("test@yahoo.com",
            "test-100@yahoo.com", "test.100@yahoo.com",
            "test111@test.com", "test-100@test.net",
            "test.100@test.com.au", "test@1.com",
            "test@gmail.com.com", "test+100@gmail.com",
            "test-100@yahoo-test.com");

    /**
     * The message number.
     */
    private static int massageNumber = 0;

    /**
     * The {@link EmailAddressDataService} instance.
     */
    private EmailAddressDataService emailAddressDataService;

    /**
     * The {@link GreenmailService} instance.
     */
    private GreenmailService greenmailService;

    /**
     * The last Mimemessages email bodies.
     */
    private List<String> lastMimeMessage = new ArrayList<String>();

    /**
     * Creating email address data in the database.
     * 
     * @return the email address id's in list.
     */
    private List<Long> createEmailAddress() {
        List<Long> result = new ArrayList<Long>();
        try {
            emailAddressDataService.saveEmailAddress(null);
            Assert.fail("Expect IllegalArgumentException, but the method not throws.");
        } catch (IllegalArgumentException e) {
            Assert.assertNotNull(e);
        }
        for (String email : VALID_EMAILS) {
            long saveEmailAddress = emailAddressDataService.saveEmailAddress(email);
            Assert.assertTrue(saveEmailAddress > 0L);
            result.add(saveEmailAddress);
        }

        for (String email : INVALID_EMAILS) {
            try {
                emailAddressDataService.saveEmailAddress(email);
                Assert.fail("Expect InvalidEmailAddressException, but the method not throws.");
            } catch (InvalidEmailAddressException e) {
                Assert.assertNotNull(e);
            }
        }
        return result;
    }

    /**
     * Creating email addresses to testing expired token.
     * 
     * @return the email address ids in list.
     */
    private List<Long> createEmailAddressToExpired() {
        GreenMail greenMail = greenmailService.getGreenMail();
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        Assert.assertNotNull(receivedMessages);
        Assert.assertEquals(massageNumber, receivedMessages.length);
        List<Long> createEmailAddressToExpired = createEmailAddress();
        createEmailAddressToExpired.addAll(createEmailAddress());
        return createEmailAddressToExpired;
    }

    /**
     * Creating email addresses to testing not expired tokens.
     * 
     * @return the email address ids in list.
     */
    private List<Long> createEmailAddressToNotExpired() {
        GreenMail greenMail = greenmailService.getGreenMail();
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        Assert.assertNotNull(receivedMessages);
        Assert.assertEquals(massageNumber, receivedMessages.length);
        List<Long> createEmailAddress = createEmailAddress();
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        createEmailAddress.addAll(createEmailAddress());
        return createEmailAddress;
    }

    /**
     * Creating verification requests.
     * 
     * @param emailAddressId
     *            the id of the email address data.
     */
    private int createVerificationRequest(final long emailAddressId) {
        Random random = new Random();
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 2);
        emailAddressDataService.createVerificationRequest(emailAddressId,
                "$acceptToken\n$rejectToken", c.getTime(), random.nextInt(MAX_RANDOM_VALUE) + 1,
                getRandomVerificationLengthBase());
        massageNumber++;

        try {
            emailAddressDataService.createVerificationRequest(0L,
                    "$acceptToken\n$rejectToken", c.getTime(), random.nextInt(MAX_RANDOM_VALUE) + 1,
                    getRandomVerificationLengthBase());
            Assert.fail("Expect NoSuchEmailAddressDataException, but the method not throws.");
        } catch (NoSuchEmailAddressDataException e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.createVerificationRequest(-1L,
                    "$acceptToken\n$rejectToken", c.getTime(), random.nextInt(MAX_RANDOM_VALUE) + 1,
                    getRandomVerificationLengthBase());
            Assert.fail("Expect NoSuchEmailAddressDataException, but the method not throws.");
        } catch (NoSuchEmailAddressDataException e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.createVerificationRequest(emailAddressId,
                    null, c.getTime(), random.nextInt(MAX_RANDOM_VALUE) + 1,
                    getRandomVerificationLengthBase());
            Assert.fail("Expect IllegalArgumentException, but the method not throws.");
        } catch (IllegalArgumentException e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.createVerificationRequest(emailAddressId,
                    "$acceptToken\n$rejectToken", null, random.nextInt(MAX_RANDOM_VALUE) + 1,
                    getRandomVerificationLengthBase());
            Assert.fail("Expect IllegalArgumentException, but the method not throws.");
        } catch (IllegalArgumentException e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.createVerificationRequest(emailAddressId,
                    "$acceptToken\n$rejectToken", c.getTime(), 0L,
                    getRandomVerificationLengthBase());
            Assert.fail("Expect NonPositiveVerificationLength, but the method not throws.");
        } catch (NonPositiveVerificationLength e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.createVerificationRequest(emailAddressId,
                    "$acceptToken\n$rejectToken", c.getTime(), random.nextInt(MAX_RANDOM_VALUE) + 1,
                    null);
            Assert.fail("Expect IllegalArgumentException, but the method not throws.");
        } catch (IllegalArgumentException e) {
            Assert.assertNotNull(e);
        }

        return massageNumber;
    }

    /**
     * Creating verification requests.
     * 
     * @param emailAddressId
     *            the id of the email address data.
     */
    private int createVerificationRequestExpired(final long emailAddressId) {
        Random random = new Random();
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MILLISECOND, 500);
        emailAddressDataService.createVerificationRequest(emailAddressId,
                "$acceptToken\n$rejectToken", c.getTime(), random.nextInt(MAX_RANDOM_VALUE) + 1,
                getRandomVerificationLengthBase());
        massageNumber++;

        try {
            emailAddressDataService.createVerificationRequest(emailAddressId,
                    "", c.getTime(), random.nextInt(MAX_RANDOM_VALUE) + 1,
                    getRandomVerificationLengthBase());
            Assert.assertTrue(true);
            massageNumber++;
        } catch (IllegalArgumentException e) {
            Assert.assertNull(e);
        }

        return massageNumber;
    }

    /**
     * Getting email bodies with the expired email addresses. Frist create the verification request then getting the
     * email bodies and add to the list.
     * 
     * @param expiredEmailAddress
     *            the expired email address ids.
     * @return the email bodies in list.
     */
    private List<String> getExpiredEmailBodiesWhenCreateVerificationRequest(final List<Long> expiredEmailAddress) {
        List<String> expiredEmailBodies = new ArrayList<String>();
        GreenMail greenMail = greenmailService.getGreenMail();
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        for (Long emailAddressId : expiredEmailAddress) {
            int createVerificationRequest = createVerificationRequestExpired(emailAddressId);
            boolean emailAddressVerified = emailAddressDataService.isEmailAddressVerified(emailAddressId);
            Assert.assertFalse(emailAddressVerified);

            receivedMessages = greenMail.getReceivedMessages();
            Assert.assertNotNull(receivedMessages);
            Assert.assertEquals(createVerificationRequest, receivedMessages.length);
            String lastEmailBody = getLastEmailBody(Arrays.asList(receivedMessages));
            if ((lastEmailBody != null) && !lastEmailBody.trim().equals("")) {
                expiredEmailBodies.add(lastEmailBody);
            }
        }
        return expiredEmailBodies;
    }

    /**
     * Return the last email body. If not decided which the last email return empty string.
     * 
     * @param receivedMessagesList
     *            the all MimeMessage.
     * @return the last email body, if not decided which the last email return empty string.
     */
    private String getLastEmailBody(final List<MimeMessage> receivedMessagesList) {
        List<String> recivedEmailBodyList = new ArrayList<String>();
        List<String> recivedEmailBodyListTmp = new ArrayList<String>();
        for (MimeMessage mimeMessage : receivedMessagesList) {
            try {
                recivedEmailBodyList.add((String) ((Multipart) mimeMessage.getContent()).getBodyPart(0).getContent());
            } catch (IOException e) {
                Assert.assertNull(e);
            } catch (MessagingException e) {
                Assert.assertNull(e);
            }
        }
        for (String body : recivedEmailBodyList) {
            if (!lastMimeMessage.contains(body)) {
                recivedEmailBodyListTmp.add(body);
            }
        }
        lastMimeMessage = recivedEmailBodyList;
        if (recivedEmailBodyListTmp.size() == 1) {
            return recivedEmailBodyListTmp.get(0);
        } else {
            return "";
        }
    }

    /**
     * Select random {@link VerificationLengthBase}.
     * 
     * @return the random {@link VerificationLengthBase}.
     */
    private VerificationLengthBase getRandomVerificationLengthBase() {
        VerificationLengthBase result = null;
        Random random = new Random();
        int number = random.nextInt(2);
        if (number == 0) {
            result = VerificationLengthBase.REQUEST_CREATION;
        } else {
            result = VerificationLengthBase.VERIFICATION;
        }
        return result;
    }

    /**
     * Testing the invalidateEmailAddress and isEmailAddressVerified methods mainly concentrate the errors.
     * 
     * @param emailAddressId
     *            the id of the email address data.
     */
    private void internalTestInvalidateEmailAddressAndIsEmailAddressVerifiedErrors(final long emailAddressId) {
        emailAddressDataService.invalidateEmailAddress(emailAddressId);

        try {
            emailAddressDataService.isEmailAddressVerified(emailAddressId);
            Assert.fail("Expect NoSuchEmailAddressDataException, but the method not throws.");
        } catch (NoSuchEmailAddressDataException e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.invalidateEmailAddress(emailAddressId);
            Assert.fail("Expect NoSuchEmailAddressDataException, but the method not throws.");
        } catch (NoSuchEmailAddressDataException e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.invalidateEmailAddress(0L);
            Assert.fail("Expect NoSuchEmailAddressDataException, but the method not throws.");
        } catch (NoSuchEmailAddressDataException e) {
            Assert.assertNotNull(e);
        }

        try {
            emailAddressDataService.invalidateEmailAddress(-1L);
            Assert.fail("Expect NoSuchEmailAddressDataException, but the method not throws.");
        } catch (NoSuchEmailAddressDataException e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * Testing the verifyEmailAddress method with the expired email addresses. Important the email addresses really
     * expired.
     * 
     * @param expiredEmailBodies
     *            the expired email bodies which belongs to the email addresses.
     */
    private void internalTestVerifyEmailAddressWithExpiredEmailAddresses(final List<String> expiredEmailBodies) {
        boolean change = true;
        for (String emailBody : expiredEmailBodies) {
            String[] splitEmailBody = emailBody.split("\\n");
            if (change) {
                EmailVerificationResult verifyEmailAddress = emailAddressDataService
                        .verifyEmailAddress(splitEmailBody[0].replace("\n", "").replace("\r", ""));
                Assert.assertNotNull(verifyEmailAddress);
                Assert.assertEquals(ConfirmationResult.FAILED, verifyEmailAddress.getResult());
                Assert.assertNull(verifyEmailAddress.getEmailAddressId());
                change = !change;
            } else {
                EmailVerificationResult verifyEmailAddress = emailAddressDataService
                        .verifyEmailAddress(splitEmailBody[1].replace("\n", "").replace("\r", ""));
                Assert.assertNotNull(verifyEmailAddress);
                Assert.assertEquals(ConfirmationResult.FAILED, verifyEmailAddress.getResult());
                Assert.assertNull(verifyEmailAddress.getEmailAddressId());
                change = !change;
            }
        }
    }

    public void setEmailAddressDataService(final EmailAddressDataService emailAddressDataService) {
        this.emailAddressDataService = emailAddressDataService;
    }

    public void setGreenmailService(final GreenmailService greenmailService) {
        this.greenmailService = greenmailService;
    }

    @Override
    public void testCreations() {
        GreenMail greenMail = greenmailService.getGreenMail();
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        List<Long> createEmailAddressToExpired = createEmailAddressToExpired();
        List<Long> createEmailAddress = createEmailAddressToNotExpired();

        for (Long emailAddressId : createEmailAddress) {
            int createVerificationRequest = createVerificationRequest(emailAddressId);
            receivedMessages = greenMail.getReceivedMessages();
            Assert.assertNotNull(receivedMessages);
            Assert.assertEquals(createVerificationRequest, receivedMessages.length);

            createVerificationRequest = createVerificationRequest(emailAddressId);
            receivedMessages = greenMail.getReceivedMessages();
            Assert.assertNotNull(receivedMessages);
            Assert.assertEquals(createVerificationRequest, receivedMessages.length);
        }

        for (Long emailAddressId : createEmailAddressToExpired) {
            int createVerificationRequestExpired = createVerificationRequestExpired(emailAddressId);
            receivedMessages = greenMail.getReceivedMessages();
            Assert.assertNotNull(receivedMessages);
            Assert.assertEquals(createVerificationRequestExpired, receivedMessages.length);

            createVerificationRequestExpired = createVerificationRequestExpired(emailAddressId);
            receivedMessages = greenMail.getReceivedMessages();
            Assert.assertNotNull(receivedMessages);
            Assert.assertEquals(createVerificationRequestExpired, receivedMessages.length);
        }

    }

    @Override
    public void testMissingVerifying() {
        GreenMail greenMail = greenmailService.getGreenMail();
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        Assert.assertNotNull(receivedMessages);
        Assert.assertEquals(massageNumber, receivedMessages.length);

        List<Long> onlySaveEmailAddress = createEmailAddress();
        onlySaveEmailAddress.addAll(createEmailAddress());
        onlySaveEmailAddress.addAll(createEmailAddress());

        List<MimeMessage> receivedMessagesList = Arrays.asList(greenMail.getReceivedMessages());
        getLastEmailBody(receivedMessagesList);
        for (Long emailAddressId : onlySaveEmailAddress) {
            try {
                emailAddressDataService.verifyEmailAddress(null);
                Assert.fail("Expect IllegalArgumentException, but the method not throws.");
            } catch (IllegalArgumentException e) {
                Assert.assertNotNull(e);
            }

            EmailVerificationResult verifyEmailAddress = emailAddressDataService
                    .verifyEmailAddress("test-uuid-0124sf3");
            Assert.assertNotNull(verifyEmailAddress);
            Assert.assertEquals(ConfirmationResult.FAILED, verifyEmailAddress.getResult());
            Assert.assertNull(verifyEmailAddress.getEmailAddressId());
            int createVerificationRequest = createVerificationRequest(emailAddressId);
            receivedMessages = greenMail.getReceivedMessages();
            Assert.assertNotNull(receivedMessages);
            Assert.assertEquals(createVerificationRequest, receivedMessages.length);

            receivedMessagesList = Arrays.asList(receivedMessages);
            String emailBody = getLastEmailBody(receivedMessagesList);
            String[] splitEmailBody = emailBody.split("\\n");
            verifyEmailAddress = emailAddressDataService
                    .verifyEmailAddress(splitEmailBody[0].replace("\n", "").replace("\r", ""));
            Assert.assertNotNull(verifyEmailAddress);
            Assert.assertEquals(ConfirmationResult.SUCCESS, verifyEmailAddress.getResult());
            verifyEmailAddress.getResult();
        }

    }

    @Override
    public void testVerificationAndInvalidatedOnExpiredVerificationEmails() {
        List<Long> createEmailAddressToExpired = createEmailAddressToExpired();
        List<String> expiredEmailBodies = getExpiredEmailBodiesWhenCreateVerificationRequest(
                createEmailAddressToExpired);
        internalTestVerifyEmailAddressWithExpiredEmailAddresses(expiredEmailBodies);
        for (Long emailAddressId : createEmailAddressToExpired) {
            internalTestInvalidateEmailAddressAndIsEmailAddressVerifiedErrors(emailAddressId);
        }
    }

    @Override
    public void testVerificationAndInvalidatedOnNotExpiredVerificationEmails() {
        GreenMail greenMail = greenmailService.getGreenMail();
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        List<Long> createEmailAddress = createEmailAddressToNotExpired();
        List<MimeMessage> receivedMessagesList = Arrays.asList(greenMail.getReceivedMessages());
        getLastEmailBody(receivedMessagesList);
        boolean change = true;
        for (Long emailAddressId : createEmailAddress) {
            int createVerificationRequest = createVerificationRequest(emailAddressId);
            boolean emailAddressVerified = emailAddressDataService.isEmailAddressVerified(emailAddressId);
            Assert.assertFalse(emailAddressVerified);

            receivedMessages = greenMail.getReceivedMessages();
            Assert.assertNotNull(receivedMessages);
            Assert.assertEquals(createVerificationRequest, receivedMessages.length);

            receivedMessagesList = Arrays.asList(receivedMessages);
            String emailBody = getLastEmailBody(receivedMessagesList);
            String[] splitEmailBody = emailBody.split("\\n");
            if (change) {
                EmailVerificationResult verifyEmailAddress = emailAddressDataService
                        .verifyEmailAddress(splitEmailBody[0].replace("\n", "").replace("\r", ""));
                Assert.assertNotNull(verifyEmailAddress);
                Assert.assertEquals(ConfirmationResult.SUCCESS, verifyEmailAddress.getResult());
                emailAddressVerified = emailAddressDataService.isEmailAddressVerified(emailAddressId);
                Assert.assertTrue(emailAddressVerified);
                change = !change;
            } else {
                EmailVerificationResult verifyEmailAddress = emailAddressDataService
                        .verifyEmailAddress(splitEmailBody[1].replace("\n", "").replace("\r", ""));
                Assert.assertNotNull(verifyEmailAddress);
                Assert.assertEquals(ConfirmationResult.REJECTED, verifyEmailAddress.getResult());
                emailAddressVerified = emailAddressDataService.isEmailAddressVerified(emailAddressId);
                Assert.assertFalse(emailAddressVerified);
                change = !change;
            }
            internalTestInvalidateEmailAddressAndIsEmailAddressVerifiedErrors(emailAddressId);
        }
    }

    @Override
    public void testVerificationAndInvalidatedOnSavedEmails() {
        List<Long> onlySaveEmailAddress = createEmailAddress();
        onlySaveEmailAddress.addAll(createEmailAddress());
        onlySaveEmailAddress.addAll(createEmailAddress());
        for (Long emailAddressId : onlySaveEmailAddress) {
            boolean emailAddressVerified = emailAddressDataService.isEmailAddressVerified(emailAddressId);
            Assert.assertFalse(emailAddressVerified);
            internalTestInvalidateEmailAddressAndIsEmailAddressVerifiedErrors(emailAddressId);
        }
    }
}
