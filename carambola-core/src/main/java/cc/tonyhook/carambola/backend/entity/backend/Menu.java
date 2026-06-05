package cc.tonyhook.carambola.backend.entity.backend;

import cc.tonyhook.carambola.backend.entity.HierarchyManagedResource;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_menu")
public class Menu extends HierarchyManagedResource {

    private String icon;

    private String link;

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }

}
