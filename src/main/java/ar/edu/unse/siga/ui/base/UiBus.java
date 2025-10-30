package ar.edu.unse.siga.ui.base;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public final class UiBus {
    private static final PropertyChangeSupport PCS = new PropertyChangeSupport(UiBus.class);

    private UiBus() {}

    public static void on(String eventName, PropertyChangeListener l) {
        PCS.addPropertyChangeListener(eventName, l);
    }

    public static void off(String eventName, PropertyChangeListener l) {
        PCS.removePropertyChangeListener(eventName, l);
    }

    public static void fire(String eventName, Object payload) {
        PCS.firePropertyChange(eventName, null, payload);
    }

    public static void fire(String eventName) {
        fire(eventName, null);
    }
}
