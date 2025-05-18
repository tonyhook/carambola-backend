package cc.tonyhook.carambola.backend.entity.ad;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_bill", indexes = {
    @Index(columnList = "date"),
    @Index(columnList = "tagId"),
    @Index(columnList = "clientPort"),
})
public class Bill {

    public static final int BILL_STATUS_FETCHED  = 1;
    public static final int BILL_STATUS_UPLOADED = 2;
    public static final int BILL_STATUS_MANUAL   = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Timestamp date;

    private String tagId;

    private Integer clientPort;

    private Long request;

    private Long response;

    private Long impression;

    private Long click;

    private Long cost;

    private Integer status;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getDate() {
        return this.date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getTagId() {
        return this.tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public Integer getClientPort() {
        return this.clientPort;
    }

    public void setClientPort(Integer clientPort) {
        this.clientPort = clientPort;
    }

    public Long getRequest() {
        return this.request;
    }

    public void setRequest(Long request) {
        this.request = request;
    }

    public Long getResponse() {
        return this.response;
    }

    public void setResponse(Long response) {
        this.response = response;
    }

    public Long getImpression() {
        return this.impression;
    }

    public void setImpression(Long impression) {
        this.impression = impression;
    }

    public Long getClick() {
        return this.click;
    }

    public void setClick(Long click) {
        this.click = click;
    }

    public Long getCost() {
        return this.cost;
    }

    public void setCost(Long cost) {
        this.cost = cost;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
