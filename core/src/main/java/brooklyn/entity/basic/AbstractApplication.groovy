package brooklyn.entity.basic

import java.beans.PropertyChangeListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException;

import brooklyn.entity.Application
import brooklyn.entity.Entity
import brooklyn.entity.trait.Changeable
import brooklyn.entity.trait.Startable
import brooklyn.location.Location
import brooklyn.management.Task
import brooklyn.management.internal.AbstractManagementContext
import brooklyn.management.internal.LocalManagementContext
import brooklyn.util.internal.SerializableObservableMap

public abstract class AbstractApplication extends AbstractGroup implements Application, Changeable {
    final ObservableMap entities = new SerializableObservableMap(new ConcurrentHashMap<String,Entity>());

    private volatile AbstractManagementContext mgmt = null;
    private boolean deployed = false
    
    public AbstractApplication(Map properties=[:]) {
        super(properties)
        if(properties.mgmt) {
            mgmt = (AbstractManagementContext) properties.remove("mgmt")
            mgmt.registerApplication(this)
        }

        // record ourself as an entity in the entity list
        registerWithApplication this
    }
    
    public void registerEntity(Entity entity) {
        entities.put entity.id, entity
    }
    
    Collection<Entity> getEntities() { entities.values() }

    @Override
    public void addEntityChangeListener(PropertyChangeListener listener) {
        entities.addPropertyChangeListener listener;
    }

    protected void initApplicationRegistrant() {
        // do nothing; we register ourself later
    }

    /**
     * Default start will start all Startable children
     */
    public void start(Collection<Location> locations) {
        getManagementContext()
        List<Entity> startable = ownedChildren.find { it in Startable }
        if (startable && !startable.isEmpty() && locations && !locations.isEmpty()) {
	        Task start = invokeEffectorList(startable, Startable.START, [ locations:locations ])
	        try {
	            start.get()
	        } catch (ExecutionException ee) {
	            throw ee.cause
	        }
        }
        deployed = true
    }

    public synchronized AbstractManagementContext getManagementContext() {
        if (mgmt) return mgmt

        //TODO how does user override?  expect he annotates a field in this class, then look up that field?
        //(do that here)

        mgmt = new LocalManagementContext()
        mgmt.registerApplication(this)
        return mgmt
    }
 
    public boolean isDeployed() {
        // TODO How to tell if we're deployed? What if sub-class overrides start 
        return deployed
    }
}
