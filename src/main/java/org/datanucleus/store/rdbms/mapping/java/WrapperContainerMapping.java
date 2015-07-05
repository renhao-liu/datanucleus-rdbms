/**********************************************************************
Copyright (c) 2015 Renato Garcia and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:

 **********************************************************************/
package org.datanucleus.store.rdbms.mapping.java;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.CollectionMetaData;
import org.datanucleus.metadata.ElementMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.rdbms.mapping.MappingCallbacks;
import org.datanucleus.store.rdbms.mapping.datastore.DatastoreMapping;
import org.datanucleus.store.rdbms.table.Table;
import org.datanucleus.store.types.ContainerAdapter;
import org.datanucleus.store.types.ElementContainerHandler;

public class WrapperContainerMapping extends JavaTypeMapping implements MappingCallbacks
{

    private JavaTypeMapping wrappedMapping;

    private Class wrappedTypeClass;

    @Override
    public void initialize(AbstractMemberMetaData mmd, Table table, ClassLoaderResolver clr)
    {

        CollectionMetaData collectionMetaData = mmd.getCollection();
        String wrappedTypeName = collectionMetaData.getElementType();
        wrappedTypeClass = clr.classForName(wrappedTypeName);

        WrappedMemberMetaData wmmd = new WrappedMemberMetaData(mmd, wrappedTypeClass, clr);
        // Get the actual mapping that handles the the wrapped type
        wrappedMapping = table.getStoreManager().getMappingManager().getMapping(wrappedTypeClass, collectionMetaData.isSerializedElement(),
            collectionMetaData.isEmbeddedElement(), null);
        wrappedMapping.initialize(wmmd, table, clr);

        super.initialize(mmd, table, clr);
    }

    @Override
    public void setMemberMetaData(AbstractMemberMetaData mmd)
    {
        super.setMemberMetaData(mmd);

        wrappedMapping.setMemberMetaData(new WrappedMemberMetaData(mmd, wrappedTypeClass, getStoreManager().getNucleusContext().getClassLoaderResolver(null)));
    }

    public JavaTypeMapping getWrappedMapping()
    {
        return wrappedMapping;
    }

    @Override
    public boolean includeInFetchStatement()
    {
        return wrappedMapping.includeInFetchStatement();
    }

    @Override
    public boolean hasSimpleDatastoreRepresentation()
    {
        // TODO: Support non simple
        // This is called during query where wrappedMapping is not initialized.
        return wrappedMapping == null ? false : wrappedMapping.hasSimpleDatastoreRepresentation();
    }

    @Override
    public void setObject(ExecutionContext ec, PreparedStatement ps, int[] pos, Object container)
    {

        // TODO Renato: Test null containers

        ContainerAdapter containerAdapter = ec.getTypeManager().getContainerAdapter(container);
        Iterator iterator = containerAdapter.iterator();
        Object value = iterator.hasNext() ? iterator.next() : null;
        wrappedMapping.setObject(ec, ps, pos, value);
    }

    @Override
    public Object getObject(ExecutionContext ec, ResultSet rs, int[] exprIndex)
    {
        Object object = wrappedMapping.getObject(ec, rs, exprIndex);
        ElementContainerHandler containerHandler = ec.getTypeManager().getContainerHandler(mmd.getType());
        return containerHandler.newContainer(object);
    }

    @Override
    public Object getObject(ExecutionContext ec, ResultSet rs, int[] exprIndex, ObjectProvider ownerOP, int ownerFieldNumber)
    {
        throw new RuntimeException("Not implemented yet!");
    }

    @Override
    public int getNumberOfDatastoreMappings()
    {
        return wrappedMapping.getNumberOfDatastoreMappings();
    }

    @Override
    public DatastoreMapping[] getDatastoreMappings()
    {
        return wrappedMapping.getDatastoreMappings();
    }

    @Override
    public DatastoreMapping getDatastoreMapping(int index)
    {
        return wrappedMapping.getDatastoreMapping(index);
    }

    @Override
    public String getJavaTypeForDatastoreMapping(int index)
    {
        return wrappedMapping.getJavaTypeForDatastoreMapping(index);
    }

    public void insertPostProcessing(ObjectProvider op)
    {
        if (wrappedMapping instanceof MappingCallbacks)
        {
            ((MappingCallbacks) wrappedMapping).insertPostProcessing(op);
        }
    }

    public void postInsert(ObjectProvider op)
    {

        if (wrappedMapping instanceof MappingCallbacks)
        {
            ((MappingCallbacks) wrappedMapping).postInsert(op);
        }
    }

    public void postFetch(ObjectProvider op)
    {
        if (wrappedMapping instanceof MappingCallbacks)
        {
            ((MappingCallbacks) wrappedMapping).postFetch(op);
        }
    }

    public void postUpdate(ObjectProvider op)
    {
        if (wrappedMapping instanceof MappingCallbacks)
        {
            ((MappingCallbacks) wrappedMapping).postUpdate(op);
        }
    }

    public void preDelete(ObjectProvider op)
    {
        if (wrappedMapping instanceof MappingCallbacks)
        {
            ((MappingCallbacks) wrappedMapping).preDelete(op);
        }
    }

    @Override
    public Class getJavaType()
    {
        return wrappedMapping.getJavaType();
    }

    private class WrappedMemberMetaData extends AbstractMemberMetaData
    {

        private AbstractMemberMetaData wrapperContainerMetadata;

        public WrappedMemberMetaData(AbstractMemberMetaData fmd, Class type, ClassLoaderResolver clr)
        {
            super(fmd.getParent(), fmd);
            this.wrapperContainerMetadata = fmd;
            this.type = type;
            this.columnMetaData = fmd.getColumnMetaData();

            this.relationType = fmd.getRelationType(clr);
            this.relatedMemberMetaData = fmd.getRelatedMemberMetaData(clr);

            // Copy the Element embedded definition to the field embedded
            // metaData because
            // EmbeddedPCMapping reads it from there. (Maybe it should use
            // EmbeddedElementPCMapping)
            ElementMetaData fmdElementMetaData = fmd.getElementMetaData();
            if (fmdElementMetaData != null && fmdElementMetaData.getEmbeddedMetaData() != null)
            {
                setEmbeddedMetaData(fmdElementMetaData.getEmbeddedMetaData());
            }
        }

        @Override
        public int getAbsoluteFieldNumber()
        {
            return wrapperContainerMetadata.getAbsoluteFieldNumber();
        }

        @Override
        public String toString()
        {
            return "Wrapped[" + getName() + "]\n" + super.toString();
        }
    }
}
