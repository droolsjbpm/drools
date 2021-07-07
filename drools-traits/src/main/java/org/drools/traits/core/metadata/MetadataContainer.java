/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.traits.core.metadata;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class MetadataContainer<T extends Metadatable> implements Serializable {

	protected transient T target;

	public MetadataContainer( T metadatableObject ) {
		this.target = metadatableObject;
	}

    protected MetaClass metaClassInfo;

    public MetaClass<T> getMetaClassInfo() {
        return metaClassInfo;
    }

    protected T getTarget() {
        return target;
    }

    public void setTarget( T target ) {
        this.target = target;
    }

    public List<MetaProperty<T,?,?>> properties() {
        return Collections.unmodifiableList( Arrays.asList( getMetaClassInfo().getProperties() ) );
    }

    public String[] propertyNames() {
        String[] names = new String[ metaClassInfo.getProperties().length ];
        for ( int j = 0; j < metaClassInfo.getProperties().length; j++ ) {
            names[ j ] = metaClassInfo.getProperties()[ j ].getName();
        }
        return names;
    }

    protected <T,R> MetaProperty<T,R,?> getProperty( String name ) {
        for ( MetaProperty p : metaClassInfo.getProperties() ) {
            if ( p.getName().equals( name ) ) {
                return p;
            }
        }
        return null;
    }

    protected <T,R> MetaProperty getProperty( int index ) {
        return metaClassInfo.getProperties()[ index ];
    }

    public static URI getIdentifier( Object object ) {
        if ( object instanceof Identifiable ) {
            URI uri = ( (Identifiable) object ).getUri();
            return uri != null ? uri : URI.create( createObjectIdentifier( object ) );
        } else if ( object instanceof MetadataHolder ) {
            return URI.create( createObjectIdentifier( ( (MetadataHolder) object ).get_().getMetaClassInfo().getUri().toString(), object ) );
        } else {
            return URI.create( createObjectIdentifier( object ) );
        }
    }

    protected static String createObjectIdentifier( Object target ) {
        return createObjectIdentifier( "urn:" + target.getClass().getPackage().getName(), target );
    }

    protected static String createObjectIdentifier( String classUri, Object target ) {
        StringBuilder sb = new StringBuilder();
        sb.append( classUri )
                .append( "/" )
                .append( target.getClass().getSimpleName() )
                .append( "/" )
                .append( System.identityHashCode( target ) );
        return sb.toString();
    }

}
