package cc.tonyhook.carambola.backend.controller.managed.backend;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.backend.Menu;
import cc.tonyhook.carambola.backend.service.backend.MenuService;

@RestController
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping(value = "/api/managed/menu", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Menu>> getMenuList() {
        List<Menu> menuList = menuService.getMenuList();

        return ResponseEntity.ok().body(menuList);
    }

    @GetMapping(value = "/api/managed/menu/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Menu> getMenu(
            @PathVariable Integer id) {
        Menu menu = menuService.getMenu(id);

        if (menu != null) {
            return ResponseEntity.ok().body(menu);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/menu", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Menu> addMenu(
            @RequestBody Menu newMenu) throws URISyntaxException {
        Menu updatedMenu = menuService.addMenu(newMenu);

        return ResponseEntity
                .created(new URI("/api/managed/menu/" + updatedMenu.getId()))
                .body(updatedMenu);
    }

    @PutMapping(value = "/api/managed/menu/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateMenu(
            @PathVariable Integer id,
            @RequestBody Menu newMenu) {
        if (!id.equals(newMenu.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Menu targetMenu = menuService.getMenu(id);
        if (targetMenu == null) {
            return ResponseEntity.notFound().build();
        }

        menuService.updateMenu(id, newMenu);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/managed/menu/{id}")
    public ResponseEntity<?> removeMenu(
            @PathVariable Integer id) {
        Menu deletedMenu = menuService.getMenu(id);
        if (deletedMenu == null) {
            return ResponseEntity.notFound().build();
        }

        menuService.removeMenu(id);

        return ResponseEntity.ok().build();
    }

}
