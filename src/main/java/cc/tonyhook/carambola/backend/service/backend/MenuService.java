package cc.tonyhook.carambola.backend.service.backend;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.backend.MenuRepository;
import cc.tonyhook.carambola.backend.dao.security.PermissionRepository;
import cc.tonyhook.carambola.backend.entity.backend.Menu;
import jakarta.transaction.Transactional;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;

    public MenuService(MenuRepository menuRepository, PermissionRepository permissionRepository) {
        this.menuRepository = menuRepository;
        this.permissionRepository = permissionRepository;
    }

    @PostFilter("hasPermission(filterObject, 'r')")
    public List<Menu> getMenuList() {
        List<Menu> menuList = menuRepository.findAll();

        return menuList;
    }

    @PreAuthorize("hasPermission(#id, 'menu', 'r')")
    public Menu getMenu(Integer id) {
        Menu menu = menuRepository.findById(id).orElse(null);

        return menu;
    }

    @PreAuthorize("hasPermission(#newMenu, 'c')")
    public Menu addMenu(Menu newMenu) {
        newMenu.setCreateTime(new Timestamp(System.currentTimeMillis()));
        newMenu.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        Menu updatedMenu = menuRepository.save(newMenu);

        return updatedMenu;
    }

    @PreAuthorize("hasPermission(#id, 'menu', 'u')")
    public void updateMenu(Integer id, Menu newMenu) {
        newMenu.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        menuRepository.save(newMenu);
    }

    @Transactional
    @PreAuthorize("hasPermission(#id, 'menu', 'd')")
    public void removeMenu(Integer id) {
        Menu deletedMenu = menuRepository.findById(id).orElse(null);

        permissionRepository.deleteByResourceTypeAndResourceId("menu", id);
        menuRepository.delete(deletedMenu);
    }

}
