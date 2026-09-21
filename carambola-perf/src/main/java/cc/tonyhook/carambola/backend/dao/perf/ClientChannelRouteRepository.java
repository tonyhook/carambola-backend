package cc.tonyhook.carambola.backend.dao.perf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;

public interface ClientChannelRouteRepository extends JpaRepository<ClientChannelRoute, Integer> {

    @Query("""
        SELECT route FROM ClientChannelRoute route
        WHERE route.clientChannel.id = :clientChannelId
            AND route.event = :event
            AND (route.deleted IS NULL OR route.deleted = false)
        ORDER BY route.id ASC
        """)
    List<ClientChannelRoute> findActiveRoutes(
            @Param("clientChannelId") Integer clientChannelId,
            @Param("event") String event);

    @Query("""
        SELECT route FROM ClientChannelRoute route
        WHERE route.clientChannel.id = :clientChannelId
            AND (route.deleted IS NULL OR route.deleted = false)
        ORDER BY route.event ASC, route.id ASC
        """)
    List<ClientChannelRoute> findActiveRoutesByChannel(
            @Param("clientChannelId") Integer clientChannelId);

    @Query("""
        SELECT route FROM ClientChannelRoute route
        WHERE route.clientChannel.id IN :clientChannelIds
            AND (route.deleted IS NULL OR route.deleted = false)
        ORDER BY route.clientChannel.id ASC, route.event ASC, route.id ASC
        """)
    List<ClientChannelRoute> findActiveRoutesByChannels(
            @Param("clientChannelIds") List<Integer> clientChannelIds);

}
