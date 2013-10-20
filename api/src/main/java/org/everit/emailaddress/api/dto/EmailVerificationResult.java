package org.everit.emailaddress.api.dto;

import org.everit.emailaddress.api.enums.ConfirmationResult;

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
/**
 * Information of the email verification result.
 */
public final class EmailVerificationResult {

    /**
     * The id of the email address data. If not exits the member is <code>null</code>.
     */
    private final Long emailAddressId;

    /**
     * The {@link ConfirmationResult} value.
     */
    private final ConfirmationResult result;

    /**
     * The simple constructor.
     * 
     * @param emailAddressId
     *            the id of the email address data.
     * @param result
     *            the {@link ConfirmationResult} value.
     */
    public EmailVerificationResult(final Long emailAddressId, final ConfirmationResult result) {
        super();
        this.emailAddressId = emailAddressId;
        this.result = result;
    }

    public Long getEmailAddressId() {
        return emailAddressId;
    }

    public ConfirmationResult getResult() {
        return result;
    }

}
