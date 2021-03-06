/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.connect.dao;

import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Organization;

/**
 * @author Maria Farooq
 */
public interface OrganizationsDao {
    /**
     * add new Organization
     *
     * @param organization
     */
    void addOrganization(final Organization organization);
    /**
     * getOrganization by sid
     * @param sid
     * @return Organization entity
     */
    Organization getOrganization(final Sid sid);

    /**
     * getOrganizationByDomainName
     * @param domainName
     * @return Organization entity
     */
    Organization getOrganizationByDomainName(final String domainName);
    /**
     * updateOrganization
     * @param organization
     */
    void updateOrganization(final Organization organization);
}
