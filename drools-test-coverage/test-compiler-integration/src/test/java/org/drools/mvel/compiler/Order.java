/*
 * Copyright 2006 Red Hat, Inc. and/or its affiliates.
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

package org.drools.mvel.compiler;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Order implements Serializable {

    private int number;

    private String customer;

    private Map items;

    private OrderStatus status;

    private Date date;
    
    private double total;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Order() {
        this( 0, "Bob" );
    }

    public Order(final int number, String customer) {
        this.number = number;
        this.items = new HashMap();
        this.customer = customer;
        this.date = new Date();
    }

    /**
     * @return the number
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(final int number) {
        this.number = number;
    }

    public Map getItems() {
        return this.items;
    }

    public Collection getItemsValues() {
        return this.items.values();
    }

    public Collection getItemsKeys() {
        return this.items.keySet();
    }

    public void addItem( OrderItem item ) {
        this.items.put( new Integer( item.getSeq() ), item );
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + this.number;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        final Order other = (Order) obj;
        if ( this.number != other.number ) {
            return false;
        }
        return true;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String toString() {
        return "Order( number="+this.getNumber()+" customer=\""+this.getCustomer()+"\" )";
    }

    public OrderStatus getStatus() {
        if( status == null ) {
            status = new OrderStatus();
        }
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public static class OrderStatus {
        private boolean active;
        
        private int val;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getVal() {
            return val;
        }

        public void setVal(int val) {
            this.val = val;
        }
        
        
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }

}
