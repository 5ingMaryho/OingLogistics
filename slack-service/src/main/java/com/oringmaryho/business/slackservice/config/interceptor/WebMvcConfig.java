package com.oringmaryho.business.slackservice.config.interceptor;

import com.oingmaryho.business.common.presentation.interceptor.AdminCheckInterceptor;
import com.oingmaryho.business.common.presentation.interceptor.UserCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final RedisTemplate<String, Object> redisTemplate;

  public WebMvcConfig(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new AdminCheckInterceptor(redisTemplate))
        .excludePathPatterns("/api/**");

    registry.addInterceptor(new UserCheckInterceptor(redisTemplate))
        .excludePathPatterns("/admin/**");
  }

}