/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.test.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Person {

    private String name;
    private int    age;
    
    private String likes;
    
    private String street;
    private String city;
    private String state;
    private String country;
    
    private Map<String, Address> addresses;
    
    private List addressList;
    
    private Address[] addressArray;

    public Person() {
    }
    
    public Person(final String name,
                  final int age) {
        this( name, age, null );
    }

    public Person(final String name,
                  final int age, 
                  final String likes ) {
        this.name = name;
        this.age = age;
        this.likes = likes;
        this.addresses = new HashMap();
        this.addressList = new ArrayList();
        this.addressArray = new Address[10];
    }

    /**
     * @return the age
     */
    public int getAge() {
        return this.age;
    }

    /**
     * @param age the age to set
     */
    public void setAge(final int age) {
        this.age = age;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public Map<String, Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(Map<String, Address> addresses) {
        this.addresses = addresses;
    }

    public List getAddressList() {
        return addressList;
    }

    public void setAddressList(List addressList) {
        this.addressList = addressList;
    }

    public Address[] getAddressArray() {
        return addressArray;
    }

    public void setAddressArray(Address[] addressArray) {
        this.addressArray = addressArray;
    }
    
    public static class Nested1 {
        public static class Nested2 {
            public static class Nested3 {
                
            }
        }
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( addressArray );
        result = prime * result + ((addressList == null) ? 0 : addressList.hashCode());
        result = prime * result + ((addresses == null) ? 0 : addresses.hashCode());
        result = prime * result + age;
        result = prime * result + ((city == null) ? 0 : city.hashCode());
        result = prime * result + ((country == null) ? 0 : country.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((street == null) ? 0 : street.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Person other = (Person) obj;
        if ( !Arrays.equals( addressArray,
                             other.addressArray ) ) return false;
        if ( addressList == null ) {
            if ( other.addressList != null ) return false;
        } else if ( !addressList.equals( other.addressList ) ) return false;
        if ( addresses == null ) {
            if ( other.addresses != null ) return false;
        } else if ( !addresses.equals( other.addresses ) ) return false;
        if ( age != other.age ) return false;
        if ( city == null ) {
            if ( other.city != null ) return false;
        } else if ( !city.equals( other.city ) ) return false;
        if ( country == null ) {
            if ( other.country != null ) return false;
        } else if ( !country.equals( other.country ) ) return false;
        if ( name == null ) {
            if ( other.name != null ) return false;
        } else if ( !name.equals( other.name ) ) return false;
        if ( state == null ) {
            if ( other.state != null ) return false;
        } else if ( !state.equals( other.state ) ) return false;
        if ( street == null ) {
            if ( other.street != null ) return false;
        } else if ( !street.equals( other.street ) ) return false;
        return true;
    }

    public String getLikes() {
        return likes;
    }

    public void setLikes(String likes) {
        this.likes = likes;
    }
    
    public String toString() {
        return "[Person name='" + name + "' age=" + age + "]";
    }    
    
}
