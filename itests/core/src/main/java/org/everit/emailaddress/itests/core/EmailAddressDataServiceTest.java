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

import org.junit.Test;

/**
 * Test interface for testing {@link EmailAddressDataService}.
 */
public interface EmailAddressDataServiceTest {

    /**
     * Save valid e-mails and try save invalid e-mail to the database. The saved e-mails to create a verification
     * requests. Test the various errors (the null parameters (messageTemplate, tokenValidityEndDate,
     * verificationLengthBase, emailAddress), invalid email address id, non positive verification length.
     */
    @Test
    void testCreations();

    /**
     * Test the wrong token UUID and null token and finally, verify the email address.
     */
    @Test
    void testMissingVerifying();

    /**
     * Test the expired verification request. Expect always (verified (use the acceptToken or rejectToken), not verified
     * ) the {@link ConfirmationResult#FAILED} result and the verified always to be false. Test wrong email address id's
     * where expect the {@link NoSuchEmailAddressDataException}.
     */
    @Test
    void testVerificationAndInvalidatedOnExpiredVerificationEmails();

    /**
     * Test the not expired verification request. While not verified the email address obtained the verified to be false
     * then when verified the request with the verifyToken it's obtained the {@link ConfirmationResult#SUCCESS} result
     * and the verified to be true. If verified the rejectToken it's obtained the {@link ConfirmationResult#REJECTED}
     * and the verified to be false. Test wrong email address id's where expect the
     * {@link NoSuchEmailAddressDataException}.
     */
    @Test
    void testVerificationAndInvalidatedOnNotExpiredVerificationEmails();

    /**
     * Test the only saved the email addresses in the database. Expect to the the verified to be false and successful
     * invalidate the email address data. Furthermore test the verified errors and invalidate errors. Test wrong email
     * address id's where expect the {@link NoSuchEmailAddressDataException}.
     */
    @Test
    void testVerificationAndInvalidatedOnSavedEmails();
}
