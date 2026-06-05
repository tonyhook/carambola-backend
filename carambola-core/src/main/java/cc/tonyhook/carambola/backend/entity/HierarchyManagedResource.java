package cc.tonyhook.carambola.backend.entity;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class HierarchyManagedResource extends SequenceManagedResource {

    protected Integer parentId;

    public Integer getParentId() {
        return this.parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

}
