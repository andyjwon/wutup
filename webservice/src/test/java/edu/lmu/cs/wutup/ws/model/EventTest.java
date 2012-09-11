package edu.lmu.cs.wutup.ws.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class EventTest {

    @Test
    public void fieldsSetByConstructorCanBeRead() {
        Event e = new Event(3, "Pool Party");
        assertThat(e.getId(), is(3));
        assertThat(e.getName(), is("Pool Party"));
    }

    @Test
    public void fieldsSetBySettersCanBeRead() {
        Event e = new Event();
        e.setId(5);
        e.setName("Programming Contest");
        assertThat(e.getId(), is(5));
        assertThat(e.getName(), is("Programming Contest"));
    }

    @Test
    public void equalsUsesIdAndNameOnly() {
        assertThat(new Event(7, "Pool Party"), equalTo(new Event(7, "Pool Party")));
        assertThat(new Event(7, "Pool Party"), not(equalTo(new Event(17, "Pool Party"))));
        assertThat(new Event(7, "Pool Party"), not(equalTo(new Event(7, "Target Practice"))));
        assertFalse(new Event(7, "Pool Party").equals("some string"));
        assertFalse(new Event(7, "Pool Party").equals(null));
    }

    @Test
    public void hashCodeProducesId() {
        assertThat(new Event(7, "Pool Party").hashCode(), is(7));
    }
}
