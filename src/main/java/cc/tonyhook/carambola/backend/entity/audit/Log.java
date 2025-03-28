package cc.tonyhook.carambola.backend.entity.audit;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_log")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Timestamp createTime;

    private String level;

    private Integer userId;

    private String username;

    private String requestMethod;

    private String requestResourceType;

    private String requestResourceId;

    private String requestParmeter;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] requestBody;

    private Integer responseCode;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] responseBody;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getLevel() {
        return this.level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRequestMethod() {
        return this.requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestResourceType() {
        return this.requestResourceType;
    }

    public void setRequestResourceType(String requestResourceType) {
        this.requestResourceType = requestResourceType;
    }

    public String getRequestResourceId() {
        return this.requestResourceId;
    }

    public void setRequestResourceId(String requestResourceId) {
        this.requestResourceId = requestResourceId;
    }

    public String getRequestParmeter() {
        return this.requestParmeter;
    }

    public void setRequestParmeter(String requestParmeter) {
        this.requestParmeter = requestParmeter;
    }

    public byte[] getRequestBody() {
        return this.requestBody;
    }

    public void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public byte[] getResponseBody() {
        return this.responseBody;
    }

    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

}
