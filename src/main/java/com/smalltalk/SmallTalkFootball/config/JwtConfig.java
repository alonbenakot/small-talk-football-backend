package com.smalltalk.SmallTalkFootball.config;

import com.smalltalk.SmallTalkFootball.services.UserService;
import com.smalltalk.SmallTalkFootball.system.utils.JwtUtil;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.EnumSet;


@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtUtil jwtUtil;
    private final UserService userService;


    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter() {
        var filterRegistrationBean = new FilterRegistrationBean<JwtAuthFilter>();
        filterRegistrationBean.setFilter(new JwtAuthFilter(jwtUtil, userService));
        filterRegistrationBean.addUrlPatterns("/small-infos");
        filterRegistrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        return filterRegistrationBean;
    }


}
