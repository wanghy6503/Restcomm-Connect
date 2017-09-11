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

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Permission;

public class PermissionsDaoTest extends DaoTest{
    private static MybatisDaoManager manager;
    private MybatisPermissionsDao permissionsDao;

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
        permissionsDao = (MybatisPermissionsDao) manager.getPermissionsDao();
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void TestGetPermission() {
        Sid sid1 = new Sid("PE00000000000000000000000000000001");
        Sid sid2 = new Sid("PE00000000000000000000000000000002");
        Sid sid3 = new Sid("PE00000000000000000000000000000003");
        String name1 = "RestComm:*:USSD";//change to constant
        String name2 = "RestComm:*:ASR";//change to constant

        Permission permission1 = null;
        Permission permission2 = null;
        Permission permission3 = null;

        permission1 = permissionsDao.getPermission(sid1);
        assertNotNull(permission1);
        assertTrue(permission1.getSid().equals(sid1));
        assertTrue(permission1.getName().equals(name1));

        permission2 = permissionsDao.getPermission(sid2);
        assertNotNull(permission2);
        assertTrue(permission2.getSid().equals(sid2));
        assertTrue(permission2.getName().equals(name2));

        permission3 = permissionsDao.getPermission(sid3);
        assertNull(permission3);

    }

    @Test
    public void TestAddPermission() {
        Sid sid3 = new Sid("PE00000000000000000000000000000003");
        String name1 = "Restcomm:*:USSD";
        Permission permission = null;
        permission = permissionsDao.getPermission(sid3);
        assertNull(permission);

        permission = new Permission(sid3, name1);
        permissionsDao.addPermission(permission);
        permission = permissionsDao.getPermission(sid3);
        assertNotNull(permission);
        assertTrue(permission.getSid().equals(sid3));
        assertTrue(permission.getName().equals(name1));
    }

    @Test
    public void TestUpdatePermission() {
        Sid sid1 = new Sid("PE00000000000000000000000000000001");
        String name1 = "RestComm:*:USSD";//change to constant
        String name2 = "RestComm:*:ASR";//change to constant
        Permission permission = null;

        permission = permissionsDao.getPermission(sid1);
        assertNotNull(permission);
        assertTrue(permission.getSid().equals(sid1));
        assertTrue(permission.getName().equals(name1));

        permission.setName(name2);
        permissionsDao.updatePermission(sid1, permission);
        permission = permissionsDao.getPermission(sid1);
        assertNotNull(permission);
        assertTrue(permission.getSid().equals(sid1));
        assertTrue(permission.getName().equals(name2));
    }

    @Test
    public void TestDeletePermission() {
        Sid sid1 = new Sid("PE00000000000000000000000000000001");
        Permission permission = null;

        permission = permissionsDao.getPermission(sid1);
        assertNotNull(permission);
        permissionsDao.deletePermission(sid1);
        permission = permissionsDao.getPermission(sid1);
        assertNull(permission);

    }
}
