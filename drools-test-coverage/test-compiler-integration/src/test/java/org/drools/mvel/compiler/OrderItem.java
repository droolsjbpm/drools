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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class OrderItem implements Externalizable {
    private static final long serialVersionUID = 510l;

    public static final int TYPE_BOOK = 1;
    public static final int TYPE_CD = 2;

    private String name;
    private int type;
    private int price;
    private int   seq;
    private Order order;


    public OrderItem() {
        this( null,
              0 );
    }

    public OrderItem(final Order order,
                     final int seq) {
        this.order = order;
        this.seq = seq;
    }

    public OrderItem(Order order, int seq, String name, int type, int price) {
        this.order = order;
        this.seq = seq;
        this.name = name;
        this.type = type;
        this.price = price;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name    = (String)in.readObject();
        type    = in.readInt();
        price   = in.readInt();
        seq     = in.readInt();
        order   = (Order)in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(name);
        out.writeInt(type);
        out.writeInt(price);
        out.writeInt(seq);
        out.writeObject(order);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    /**
     * @return the order
     */
    public Order getOrder() {
        return this.order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(final Order order) {
        this.order = order;
    }

    /**
     * @return the seq
     */
    public int getSeq() {
        return this.seq;
    }

    /**
     * @param seq the seq to set
     */
    public void setSeq(final int seq) {
        this.seq = seq;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((this.order == null) ? 0 : this.order.hashCode());
        result = PRIME * result + this.seq;
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
        final OrderItem other = (OrderItem) obj;
        if ( this.order == null ) {
            if ( other.order != null ) {
                return false;
            }
        } else if ( !this.order.equals( other.order ) ) {
            return false;
        }
        if ( this.seq != other.seq ) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "OrderItem( order="+this.getOrder()+" seq="+this.getSeq()+")";
    }
}
