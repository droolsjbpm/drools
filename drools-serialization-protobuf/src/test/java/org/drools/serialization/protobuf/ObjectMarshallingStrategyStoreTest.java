/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.serialization.protobuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.drools.core.SessionConfiguration;
import org.drools.core.common.DefaultFactHandle;
import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.core.marshalling.impl.IdentityPlaceholderResolverStrategy;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.marshalling.ObjectMarshallingStrategyAcceptor;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.marshalling.MarshallerFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;

public class ObjectMarshallingStrategyStoreTest { 

	private class Thing{
		int id;
		String value;
		Thing( int id, String value ){
			this.id = id;
			this.value = value;
		}
		
		public boolean equals( Object thing )
		{
			return thing!= null && thing instanceof Thing && ((Thing)thing).id == this.id;
		}
		
		public String toString(){
			return "Thing:"+id+","+value;
		}
	}
	
	@Test 
	public void testThrowErrorWhenExistMultipleMarshallingStrategiesWithSameName() throws IOException, ClassNotFoundException {

		Environment env = EnvironmentFactory.newEnvironment();
		final Thing entityOne = new Thing( 1, "Object 1" );
		final Thing entityTwo = new Thing( 2, "Object 2" );
		
		Collection srcItems = new ArrayList();
		srcItems.add( entityOne ); 
		srcItems.add( entityTwo );
		
		ObjectMarshallingStrategy[] strats = new ObjectMarshallingStrategy[] {
				new IdentityPlaceholderResolverStrategy( new ObjectMarshallingStrategyAcceptor() {

					@Override
					public boolean accept(Object object) {
						return entityOne.equals(object);
					}
				}, Collections.singletonMap(entityOne.id, (Object) entityOne)),
				new IdentityPlaceholderResolverStrategy( new ObjectMarshallingStrategyAcceptor() {

					@Override
					public boolean accept(Object object) {
						return entityTwo.equals(object);
					}
				}, Collections.singletonMap(entityTwo.id, (Object) entityTwo)) };

		env.set(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, strats);

		KieSessionConfiguration ksc = SessionConfiguration.newInstance();

		final KieBaseConfiguration kbconf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();

		kbconf.setOption(EventProcessingOption.STREAM);
		
		InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase( kbconf );

		KieSession ks = kbase.newKieSession( ksc, env);
		
		
		ks.insert( entityOne ); 
		ks.insert( entityTwo );
		
		try{
			ProtobufMarshaller marshaller = (ProtobufMarshaller) MarshallerFactory.newMarshaller(kbase, strats);
			// Here ocurrs the bug that shows that NamedObjectMarshallingStrategies are required.
			Assert.fail( "A runtime error must be thrown while found strategies with same name" );
		}catch( RuntimeException re ){
			Assert.assertTrue( re.getMessage().contains( "Multiple" ) );
		}
	}
	
	@Test 
	public void testMultipleObjectMarshallingStrategiesOfTheSameClassWithDifferentNames() throws IOException, ClassNotFoundException {

		Environment env = EnvironmentFactory.newEnvironment();
		final Thing entityOne = new Thing( 1, "Object 1" );
		final Thing entityTwo = new Thing( 2, "Object 2" );
		
		Collection srcItems = new ArrayList();
		srcItems.add( entityOne ); 
		srcItems.add( entityTwo );
		
		ObjectMarshallingStrategy[] strats = new ObjectMarshallingStrategy[] {
				new IdentityPlaceholderResolverStrategy("entityOne", new ObjectMarshallingStrategyAcceptor() {

					@Override
					public boolean accept(Object object) {
						return entityOne.equals(object);
					}
				}, Collections.singletonMap(entityOne.id, (Object) entityOne)),
				new IdentityPlaceholderResolverStrategy("entityTwo", new ObjectMarshallingStrategyAcceptor() {

					@Override
					public boolean accept(Object object) {
						return entityTwo.equals(object);
					}
				}, Collections.singletonMap(entityTwo.id, (Object) entityTwo)),
				};

		env.set(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, strats);

		KieSessionConfiguration ksc = SessionConfiguration.newInstance();

		final KieBaseConfiguration kbconf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();

		kbconf.setOption(EventProcessingOption.STREAM);
		
		InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase( kbconf );

		KieSession ks = kbase.newKieSession( ksc, env);
		
		
		ks.insert( entityOne ); 
		ks.insert( entityTwo );
		
		ProtobufMarshaller marshaller = (ProtobufMarshaller) MarshallerFactory.newMarshaller(kbase, strats);
		
		// Serialize object
		final byte[] b1;
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			marshaller.marshall(bos, ks, System.currentTimeMillis());
			b1 = bos.toByteArray();
			bos.close();
		}

		// Deserialize object
		StatefulKnowledgeSession ksession2;
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(b1);
			try{
				ksession2 = marshaller.unmarshall(bais, ks.getSessionConfiguration(), ks.getEnvironment());
				Collection items = ksession2.getFactHandles();
				Assert.assertTrue( items.size() == 2 );
				for( Object item : items ){
					FactHandle factHandle = (FactHandle)item;
					Assert.assertTrue( srcItems.contains( ((DefaultFactHandle)factHandle).getObject() ) );
				}
			}catch( RuntimeException npe ){
				// Here ocurrs the bug that shows that NamedObjectMarshallingStrategies are required.
				Assert.fail( "This error only happens if identity ObjectMarshallingStrategy use old name" );
			}finally{
				bais.close();
			}
			
			
		}
	}

}
