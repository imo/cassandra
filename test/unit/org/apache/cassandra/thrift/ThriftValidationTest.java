package org.apache.cassandra.thrift;
/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */


import org.junit.Test;

import org.apache.cassandra.CleanupHelper;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.marshal.AsciiType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.locator.LocalStrategy;
import org.apache.cassandra.locator.NetworkTopologyStrategy;

public class ThriftValidationTest extends CleanupHelper
{
    @Test(expected=InvalidRequestException.class)
    public void testValidateCommutativeWithStandard() throws InvalidRequestException
    {
        ThriftValidation.validateColumnFamily("Keyspace1", "Standard1", true);
    }

    @Test
    public void testValidateCommutativeWithCounter() throws InvalidRequestException
    {
        ThriftValidation.validateColumnFamily("Keyspace1", "Counter1", true);
    }

    @Test
    public void testColumnNameEqualToKeyAlias()
    {
        CFMetaData metaData = Schema.instance.getCFMetaData("Keyspace1", "Standard1");
        CfDef newMetadata = metaData.toThrift();

        boolean gotException = false;

        // add a key_alias = "id"
        newMetadata.setKey_alias(AsciiType.instance.decompose("id"));

        // should not throw IRE here
        try
        {
            ThriftValidation.validateCfDef(newMetadata, metaData);
        }
        catch (InvalidRequestException e)
        {
            gotException = true;
        }

        assert !gotException : "got unexpected InvalidRequestException";

        // add a column with name = "id"
        newMetadata.addToColumn_metadata(new ColumnDef(UTF8Type.instance.decompose("id"),
                                                       "org.apache.cassandra.db.marshal.UTF8Type"));


        gotException = false;

        try
        {
            ThriftValidation.validateCfDef(newMetadata, metaData);
        }
        catch (InvalidRequestException e)
        {
            gotException = true;
        }

        assert gotException : "expected InvalidRequestException but not received.";
    }

    @Test
    public void testValidateKsDef()
    {
        KsDef ks_def = new KsDef()
                            .setName("keyspaceValid")
                            .setStrategy_class(LocalStrategy.class.getSimpleName());


        boolean gotException = false;

        try
        {
            ThriftValidation.validateKsDef(ks_def);
        }
        catch (ConfigurationException e)
        {
            gotException = true;
        }

        assert gotException : "expected ConfigurationException but not received.";

        ks_def.setStrategy_class(LocalStrategy.class.getName());

        gotException = false;

        try
        {
            ThriftValidation.validateKsDef(ks_def);
        }
        catch (ConfigurationException e)
        {
            gotException = true;
        }

        assert gotException : "expected ConfigurationException but not received.";

        ks_def.setStrategy_class(NetworkTopologyStrategy.class.getName());

        gotException = false;

        try
        {
            ThriftValidation.validateKsDef(ks_def);
        }
        catch (ConfigurationException e)
        {
            gotException = true;
        }

        assert !gotException : "got unexpected ConfigurationException";
    }
}
