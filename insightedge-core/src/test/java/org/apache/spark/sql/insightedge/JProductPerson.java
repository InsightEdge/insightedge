package org.apache.spark.sql.insightedge;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.spark.japi.JGridModel;
import scala.Product;
import scala.collection.AbstractIterator;
import scala.collection.Iterator;

import java.io.Serializable;

public class JProductPerson extends JGridModel {

    private String id;
    private String name;
    private Integer age;
    private JProductAddress address;

    public JProductPerson() {
    }

    public JProductPerson(String id, String name, Integer age, JProductAddress address) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.address = address;
    }

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public JProductAddress getAddress() {
        return address;
    }

    public void setAddress(JProductAddress address) {
        this.address = address;
    }
}

class JProductAddress implements Serializable, Product {
    private String city;
    private String state;

    public JProductAddress() {
    }

    public JProductAddress(String city, String state) {
        this.city = city;
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public String city() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public String state() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    public Object productElement(int n) {
        switch (n) {
            case 0:
                return city;
            case 1:
                return state;
            default:
                throw new IndexOutOfBoundsException(String.valueOf(n));
        }
    }

    public int productArity() {
        return 2;
    }

    public Iterator<Object> productIterator() {
        return new JProductIterator(this);
    }

    public String productPrefix() {
        return this.getClass().getSimpleName();
    }

    public boolean canEqual(Object that) {
        return false;
    }

    private static class JProductIterator extends AbstractIterator<Object> {
        private Product product;
        private int position = 0;

        public JProductIterator(Product product) {
            this.product = product;
        }

        public boolean hasNext() {
            return position < product.productArity();
        }

        public Object next() {
            Object result = product.productElement(position);
            position += 1;
            return result;
        }
    }
}

