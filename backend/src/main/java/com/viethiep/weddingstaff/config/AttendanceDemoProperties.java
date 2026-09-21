package com.viethiep.weddingstaff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.attendance.demo")
public class AttendanceDemoProperties {
    /** Chỉ bật ở máy demo/dev. Production phải để false. */
    private boolean enabled = false;
}
