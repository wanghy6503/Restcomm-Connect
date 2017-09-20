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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.restcomm.connect.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Permission;

/**
 * Elementary mocking for AccountsDao to be used for endpoint unit testing mostly.
 * Add further implementations if needed.
 *
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class AccountsDaoMock implements AccountsDao {

    List<Account> accounts = new ArrayList<Account>();

    public AccountsDaoMock(List<Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public void addAccount(Account account) {
        throw new NotImplementedException();
    }

    @Override
    public Account getAccount(Sid sid) {
        for (Account account: accounts) {
            if (account.getSid().toString().equals(sid))
                return account;
        }
        return null;
    }

    @Override
    public Account getAccount(String name) {
        throw new NotImplementedException();
    }

    @Override
    public Account getAccountToAuthenticate(String name) {
        for (Account account: accounts) {
            if (account.getEmailAddress().equals(name)) {
                return account;
            }
        }
        return null;
    }

    @Override
    public List<Account> getChildAccounts(Sid sid) {
        throw new NotImplementedException();
    }

    @Override
    public void removeAccount(Sid sid) {
        throw new NotImplementedException();
    }

    @Override
    public void updateAccount(Account account) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getSubAccountSidsRecursive(Sid parentAccountSid) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getAccountLineage(Sid accountSid) throws AccountHierarchyDepthCrossed {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getAccountLineage(Account account) throws AccountHierarchyDepthCrossed {
        throw new NotImplementedException();
    }

    @Override
    public void addAccountPermissions(Sid account_sid, List<Permission> permissions) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addAccountPermission(Sid account_sid1, Permission permission) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateAccountPermissions(Sid account_sid1, Permission permission) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteAccountPermission(Sid account_sid1, Sid permission_sid1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clearAccountPermissions(Sid account_sid1, ArrayList<Permission> permissions) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteAccountPermissionByName(Sid account_sid1, String permission_name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<Permission> getAccountPermissions(Sid account_sid) {
        // TODO Auto-generated method stub
        return null;
    }
}
