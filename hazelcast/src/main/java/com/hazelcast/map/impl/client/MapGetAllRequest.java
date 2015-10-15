/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl.client;

import com.hazelcast.client.impl.client.RetryableRequest;
import com.hazelcast.client.impl.client.SecureRequest;
import com.hazelcast.map.impl.MapEntries;
import com.hazelcast.map.impl.MapPortableHook;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.MapPermission;
import com.hazelcast.spi.OperationFactory;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapGetAllRequest extends MapAllPartitionsClientRequest implements Portable, RetryableRequest, SecureRequest {

    private List<Data> keys = new ArrayList<Data>();

    public MapGetAllRequest() {
    }

    public MapGetAllRequest(String name, List<Data> keys) {
        this.name = name;
        this.keys = keys;
    }

    @Override
    public int getFactoryId() {
        return MapPortableHook.F_ID;
    }

    @Override
    public int getClassId() {
        return MapPortableHook.GET_ALL;
    }

    @Override
    protected OperationFactory createOperationFactory() {
        return getOperationProvider().createGetAllOperationFactory(name, keys);
    }

    @Override
    protected Object reduce(Map<Integer, Object> map) {
        MapEntries result = new MapEntries();
        MapService mapService = getService();
        for (Map.Entry<Integer, Object> entry : map.entrySet()) {
            MapEntries mapEntries = (MapEntries) mapService.getMapServiceContext().toObject(entry.getValue());
            for (Map.Entry<Data, Data> dataEntry : mapEntries) {
                result.add(dataEntry);
            }
        }
        return result;
    }

    @Override
    public String getServiceName() {
        return MapService.SERVICE_NAME;
    }

    @Override
    public void write(PortableWriter writer) throws IOException {
        writer.writeUTF("n", name);
        writer.writeInt("size", keys.size());
        if (!keys.isEmpty()) {
            ObjectDataOutput out = writer.getRawDataOutput();
            for (Data key : keys) {
                out.writeData(key);
            }
        }
    }

    @Override
    public void read(PortableReader reader) throws IOException {
        name = reader.readUTF("n");
        int size = reader.readInt("size");
        if (size > 0) {
            ObjectDataInput input = reader.getRawDataInput();
            for (int i = 0; i < size; i++) {
                Data key = input.readData();
                keys.add(key);
            }
        }

    }

    public Permission getRequiredPermission() {
        return new MapPermission(name, ActionConstants.ACTION_READ);
    }

    @Override
    public String getDistributedObjectName() {
        return name;
    }

    @Override
    public String getMethodName() {
        return "getAll";
    }

    @Override
    public Object[] getParameters() {
        return new Object[]{keys};
    }
}
