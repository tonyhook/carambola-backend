package cc.tonyhook.carambola.backend.web;

import java.sql.Timestamp;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.dao.audit.LogRepository;
import cc.tonyhook.carambola.backend.entity.audit.Log;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CarambolaLoggerInterceptor implements HandlerInterceptor {

    private final LogRepository logRepository;

    public CarambolaLoggerInterceptor(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable ModelAndView modelAndView) throws Exception {
        if (request.getMethod().equals("GET")) {
            // for list/get
            return;
        }

        if (!(request instanceof CarambolaCachedBodyHttpServletRequest)) {
            return;
        }

        byte[] requestBody = ((CarambolaCachedBodyHttpServletRequest)request).getContentAsByteArray();
        byte[] responseBody = null;
        if (response.getClass().getName().equals("org.springframework.web.filter.ShallowEtagHeaderFilter$ConditionalContentCachingResponseWrapper")) {
            ContentCachingResponseWrapper contentCachingResponseWrapper = (ContentCachingResponseWrapper)response;
            responseBody = contentCachingResponseWrapper.getContentAsByteArray();
        } else if (response.getClass().getName().equals("cc.tonyhook.carambola.backend.web.CarambolaCachedBodyHttpServletResponse")) {
            responseBody = ((CarambolaCachedBodyHttpServletResponse)response).getContentAsByteArray();
        } else {
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String[] path = request.getRequestURI().split("/");

        Log log = new Log();
        if (response.getStatus() / 100 == 2) {
            log.setLevel("INFO");
        } else {
            log.setLevel("ERROR");
        }
        log.setCreateTime(new Timestamp(System.currentTimeMillis()));
        log.setUsername((String) request.getSession().getAttribute("username"));
        log.setUserId((Integer) request.getSession().getAttribute("id"));

        log.setRequestMethod(request.getMethod());
        if (path.length == 5) {
            // /api/managed/type/id for update/remove
            log.setRequestResourceType(path[3]);
            log.setRequestResourceId(path[4]);
        }
        if (path.length == 4) {
            // /api/managed/type for add
            log.setRequestResourceType(path[3]);
            log.setRequestResourceId(null);
        }
        log.setRequestParmeter(objectMapper.writeValueAsString(request.getParameterMap()));
        log.setRequestBody(requestBody);

        log.setResponseCode(response.getStatus());
        log.setResponseBody(responseBody);

        logRepository.save(log);
    }

}
