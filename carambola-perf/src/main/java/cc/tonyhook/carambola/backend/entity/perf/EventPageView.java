package cc.tonyhook.carambola.backend.entity.perf;

import java.util.List;

public class EventPageView {

    private List<EventView> content;

    private Long totalElements;

    private Integer page;

    private Integer size;

    public List<EventView> getContent() {
        return this.content;
    }

    public void setContent(List<EventView> content) {
        this.content = content;
    }

    public Long getTotalElements() {
        return this.totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getPage() {
        return this.page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return this.size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

}
