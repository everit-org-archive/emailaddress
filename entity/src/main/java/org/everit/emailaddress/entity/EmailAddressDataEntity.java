package org.everit.emailaddress.entity;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.everit.verifiabledata.entity.VerifiableDataEntity;

/**
 * The entity of the email address data.
 */
@Entity
@Table(name = "EMAILADDRESS_DATA")
public class EmailAddressDataEntity {

    /**
     * The id of the email address data.
     */
    @Id
    @GeneratedValue
    @Column(name = "EMAIL_ADDRESS_ID")
    private long emailAddressDataId;

    /**
     * The email address.
     */
    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;

    /**
     * Optional field. If cannot be check the e-mail address it's <code>null</code>. If can be check the email address
     * we are checking the verifiable_data components.
     */
    @ManyToOne
    @JoinColumn(name = "VERIFIABLE_DATA_ID")
    private VerifiableDataEntity verifiableData;

    /**
     * The default constructor.
     */
    public EmailAddressDataEntity() {
    }

    /**
     * The simple constructor.
     * 
     * @param emailAddressDataId
     *            the id of the email address data.
     * @param emailAddress
     *            the email address of the email address data.
     * @param verifiableData
     *            the {@link VerifiableDataEntity} object.
     */
    public EmailAddressDataEntity(final long emailAddressDataId, final String emailAddress,
            final VerifiableDataEntity verifiableData) {
        super();
        this.emailAddressDataId = emailAddressDataId;
        this.emailAddress = emailAddress;
        this.verifiableData = verifiableData;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public long getEmailAddressDataId() {
        return emailAddressDataId;
    }

    public VerifiableDataEntity getVerifiableData() {
        return verifiableData;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setEmailAddressDataId(final long emailAddressDataId) {
        this.emailAddressDataId = emailAddressDataId;
    }

    public void setVerifiableData(final VerifiableDataEntity verifiableData) {
        this.verifiableData = verifiableData;
    }

}
