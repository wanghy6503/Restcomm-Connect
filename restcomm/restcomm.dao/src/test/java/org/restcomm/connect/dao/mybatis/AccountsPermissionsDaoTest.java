/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.restcomm.connect.dao.mybatis;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Permission;
import org.restcomm.connect.dao.entities.AccountPermission;

public class AccountsPermissionsDaoTest extends DaoTest{
    private static MybatisDaoManager manager;
    private MybatisPermissionsDao permissionsDao;
    private MybatisAccountsDao accountsDao;

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
        permissionsDao = (MybatisPermissionsDao) manager.getPermissionsDao();
        accountsDao = (MybatisAccountsDao) manager.getAccountsDao();
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void testGetAccountPermissions(){
        Sid account_sid1 = new Sid("ACae6e420f425248d6a26948c17a9e2acf");
        Sid account_sid2 = new Sid("ACae6e420f425248d6a26948c17a9e2acg");
        Sid permission_sid1 = new Sid("PE00000000000000000000000000000001");
        Sid permission_sid2 = new Sid("PE00000000000000000000000000000002");
        String permission_name1 = "RestComm:*:USSD";
        String permission_name2 = "RestComm:*:ASR";

        //Array, hash
        List<Permission> permissions = new ArrayList<Permission>();
        Permission permission = new Permission(permission_sid1, permission_name1);
        //Array, hash
        permissions = accountsDao.getAccountPermissions(account_sid1);

        assertTrue(((AccountPermission)permissions.get(0)).getSid().equals(permission_sid1));
        assertTrue(((AccountPermission)permissions.get(0)).getName().equals(permission_name1));
        assertTrue(((AccountPermission)permissions.get(0)).getValue());

        assertTrue(((AccountPermission)permissions.get(1)).getSid().equals(permission_sid2));
        assertTrue(((AccountPermission)permissions.get(1)).getName().equals(permission_name2));
        assertFalse(((AccountPermission)permissions.get(1)).getValue());
    }

    @Test
    public void testAddAccountPermissions(){
        Sid account_sid1 = new Sid("AC00000000000000000000000000000002");
        Sid permission_sid1 = new Sid("PE00000000000000000000000000000096");
        Sid permission_sid2 = new Sid("PE00000000000000000000000000000097");
        Sid permission_sid3 = new Sid("PE00000000000000000000000000000098");
        String permission_name1 = "RestComm:*:USSD";
        String permission_name2 = "RestComm:*:ASR";
        String permission_name3 = "RestComm:*:STUFF";
        //Array, hash
        List<Permission> permissions = new ArrayList<Permission>();
        List<Permission> permissions2 = new ArrayList<Permission>();
        AccountPermission permission1 = new AccountPermission(permission_sid1, permission_name1, true);
        AccountPermission permission2 = new AccountPermission(permission_sid2, permission_name2, false);
        AccountPermission permission3 = new AccountPermission(permission_sid3, permission_name3, true);
        permissionsDao.addPermission(permission1);
        permissionsDao.addPermission(permission2);
        permissionsDao.addPermission(permission3);

        permissions.add(permission1);
        permissions.add(permission2);
        permissions.add(permission3);
        accountsDao.addAccountPermissions(account_sid1, permissions);
        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions2.size()==3);
        assertTrue(((AccountPermission)permissions2.get(0)).getSid().equals(permission_sid1));
        assertTrue(((AccountPermission)permissions2.get(0)).getName().equals(permission_name1));
        assertTrue(((AccountPermission)permissions2.get(0)).getValue());

        assertTrue(((AccountPermission)permissions2.get(1)).getSid().equals(permission_sid2));
        assertTrue(((AccountPermission)permissions2.get(1)).getName().equals(permission_name2));
        assertFalse(((AccountPermission)permissions2.get(1)).getValue());

        assertTrue(((AccountPermission)permissions2.get(2)).getSid().equals(permission_sid3));
        assertTrue(((AccountPermission)permissions2.get(2)).getName().equals(permission_name3));
        assertTrue(((AccountPermission)permissions2.get(2)).getValue());

    }

    @Test
    public void testUpdateAccountPermissions(){
        Sid account_sid1 = new Sid("AC00000000000000000000000000000001");
        Sid permission_sid1 = new Sid("PE00000000000000000000000000000001");
        String permission_name = "RestComm:*:ASR";
        //Array, hash
        List<Permission> permissions1 = new ArrayList<Permission>();
        List<Permission> permissions2 = new ArrayList<Permission>();


        boolean value1 = true;
        boolean value2 = false;
        //change value 
        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        AccountPermission permission1 = (AccountPermission)permissions1.get(0);
        AccountPermission permission2 = (AccountPermission)permissions1.get(1);
        assertTrue(permission1.getValue());
        assertFalse(permission2.getValue());
        permission1.setValue(!permission1.getValue());
        permission2.setValue(!permission2.getValue());
        assertFalse(permission1.getValue());
        assertTrue(permission2.getValue());
        accountsDao.updateAccountPermissions(account_sid1, permission1);
        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        AccountPermission permission3 = (AccountPermission)permissions2.get(0);
        AccountPermission permission4 = (AccountPermission)permissions2.get(1);
        assertFalse(permission3.getValue());
        assertFalse(permission4.getValue());
    }

    @Test
    public void testDeleteAccountPermissions(){
        Sid account_sid1 = new Sid("AC00000000000000000000000000000001");
        Sid permission_sid1 = new Sid("PE00000000000000000000000000000096");
        Sid permission_sid2 = new Sid("PE00000000000000000000000000000097");
        Sid permission_sid3 = new Sid("PE00000000000000000000000000000098");
        String permission_name = "RestComm:*:ASR";
        //Array, hash
        List<Permission> permissions1 = new ArrayList<Permission>();
        List<Permission> permissions2 = new ArrayList<Permission>();
        Permission permission1 = null;

        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==3);

        accountsDao.deleteAccountPermission(account_sid1, permission_sid1);
        accountsDao.deleteAccountPermission(account_sid1, permission_sid3);
        //accountsDao.deleteAccountPermissionByName(account_sid1, permission_name);
        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        permission1 = permissions2.get(0);
        assertTrue(permissions2.size()==1);
        assertTrue(permission1.getSid().equals(permission_sid2));
    }
    @Test
    public void testDeleteAccountPermissionsByName(){
        Sid account_sid1 = new Sid("AC00000000000000000000000000000001");
        Sid permission_sid1 = new Sid("PE00000000000000000000000000000096");
        Sid permission_sid2 = new Sid("PE00000000000000000000000000000097");
        Sid permission_sid3 = new Sid("PE00000000000000000000000000000098");
        String permission_name1 = "RestComm:*:ASR";
        String permission_name2 = "RestComm:*:USSD";
        String permission_name3 = "RestComm:*:USSD";
        //Array, hash
        List<Permission> permissions1 = new ArrayList<Permission>();
        List<Permission> permissions2 = new ArrayList<Permission>();
        Permission permission1 = null;

        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==3);

        accountsDao.deleteAccountPermission(account_sid1, permission_sid1);
        accountsDao.deleteAccountPermission(account_sid1, permission_sid3);
        //accountsDao.deleteAccountPermissionByName(account_sid1, permission_name);
        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        permission1 = permissions2.get(0);
        assertTrue(permissions1.size()==1);
        assertTrue(permission1.getSid().equals(permission_sid2));
    }
    @Test
    public void testClearAccountPermissions(){
        
    }
}
