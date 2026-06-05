package cc.tonyhook.carambola.backend.entity;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class ContainedManagedResource extends SequenceManagedResource {

    protected String containerType;

    protected Integer containerId;

    public String getContainerType() {
        return this.containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType;
    }

    public Integer getContainerId() {
        return this.containerId;
    }

    public void setContainerId(Integer containerId) {
        this.containerId = containerId;
    }

}
