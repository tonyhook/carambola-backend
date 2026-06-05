package cc.tonyhook.carambola.backend.entity;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class SequenceManagedResource extends ManagedResource {

    protected Integer sequence;

    public Integer getSequence() {
        return this.sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

}
