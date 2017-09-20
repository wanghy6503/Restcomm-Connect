/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.connect.http.converter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.util.StringUtils;
import org.restcomm.connect.dao.entities.Permission;

import java.lang.reflect.Type;

@ThreadSafe
public final class PermissionsConverter extends AbstractConverter implements JsonSerializer<Permission> {
    private final String apiVersion;
    private final String rootUri;

    public PermissionsConverter(final Configuration configuration) {
        super(configuration);
        this.apiVersion = configuration.getString("api-version");
        rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Permission.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final Permission permission = (Permission) object;
        writer.startNode("Permission");
        writeSid(permission.getSid(),writer);
        writer.startNode("Name");
        writer.setValue(permission.getName());
        writer.endNode();
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Permission permission, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(permission.getSid(), object);
        object.addProperty("name", permission.getName());
        return object;
    }
}
