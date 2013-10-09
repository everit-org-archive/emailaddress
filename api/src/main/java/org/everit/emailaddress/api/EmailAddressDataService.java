package org.everit.emailaddress.api;

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

import org.everit.emailaddress.api.dto.EmailVerificationResult;
import org.everit.verifiabledata.api.enums.VerificationLengthBase;

/**
 * Service for managing the email address data.
 */
public interface EmailAddressDataService {

    /**
     * Create a new verification request and send e-mail the email address.
     * 
     * @param emailAddressId
     *            the id of the email address data. Must be exist the email address data.
     * @param messageTemplate
     *            the message template. Replacing the $rejectToken variable the reject token and $acceptToken variable
     *            the accept token. Cannot be <code>null</code>.
     * @param tokenValidityEndDate
     *            the expiration date of the token. Cannot be <code>null</code>.
     * @param verificationLength
     *            the verification length in seconds. Must be positive.
     * @param verificationLengthBase
     *            the {@link VerificationLengthBase} that is valid for the request. Cannot be <code>null</code>.
     * 
     * @throws IllegalArgumentException
     *             If the tokenValidityEndDate or verificationLengthBase or messageTemplate parameter is
     *             <code>null</code>.
     * @throws NonPositiveVerificationLength
     *             if the verification length is not positive.
     * @throws NoSuchEmailAddressDataException
     *             if not exist the email address data.
     */
    void createVerificationRequest(final long emailAddressId, String messageTemplate, Date tokenValidityEndDate,
            long verificationLength, VerificationLengthBase verificationLengthBase);

    /**
     * Invalidating the email address and the associated requests.
     * 
     * @param emailAddressId
     *            the id of the email address. Must be exist the email address data.
     * 
     * @throws NoSuchEmailAddressDataException
     *             if not exist the email address data.
     */
    void invalidateEmailAddress(long emailAddressId);

    /**
     * Checks the email address is verified or not.
     * 
     * @param emailAddressId
     *            the id of the email address data. Must be exist the email address data.
     * @return <code>true</code> if verified the email address and if not verified return <code>false</code>.
     * 
     * @throws NoSuchEmailAddressDataException
     *             if not exist the email address data.
     */
    boolean isEmailAddressVerified(long emailAddressId);

    /**
     * Save the email address in the database. Save only if the email address is valid.
     * 
     * @param emailAddress
     *            the email address. Cannot be <code>null</code>.
     * @return the id of the email address data if the saving is successful.
     * 
     * @throws IllegalArgumentException
     *             if the emailAddress parameter is <code>null</code>.
     * @throws InvalidEmailAddressException
     *             if the email address is invalid.
     */
    long saveEmailAddress(String emailAddress);

    /**
     * Validating the email address based on tokenUUID.
     * 
     * @param tokenUUID
     *            the token UUID. Cannot be <code>null</code>.
     * @return the {@link EmailVerificationResult} object. If the token UUID is invalid return the
     *         EmailVerificationResult object whose contains {@link ConfirmationResult#FAILED} and the verifiable
     *         data id is null.
     * 
     * @throws IllegalArgumentException
     *             if the tokenUUID is <code>null</code>.
     */
    EmailVerificationResult verifyEmailAddress(String tokenUUID);
}
